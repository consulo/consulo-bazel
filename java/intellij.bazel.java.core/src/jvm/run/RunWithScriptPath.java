package org.jetbrains.bazel.jvm.run;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.server.bep.TestXmlParser;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaBazelRules;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bazel.util.BspClientTestNotifier;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jetbrains.bazel.utils.ExecutableInfo.UnexpectedScriptContentException;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class RunWithScriptPath {
    private RunWithScriptPath() {
    }

    /**
     * See {@link ScriptPathBeforeRunTaskProvider}, we have to pass {@code env} to the script process manually
     * because Bazel doesn't include environment variable setup in the generated script.
     */
    public static CompletableFuture<Integer> runWithScriptPath(
        TaskId taskId,
        Path scriptPath,
        Project project,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler,
        Map<String, String> env,
        List<String> additionalScriptParameters,
        boolean isTest,
        boolean isPassParentEnvs,
        @Nullable String testFilter,
        Function<ProcessHandler, CompletableFuture<?>> processHandlerCreated
    ) {
        GeneralCommandLine commandLine =
            Utils.scriptPathCommandLine(scriptPath, env, isTest, isPassParentEnvs, testFilter, additionalScriptParameters);

        return runJvmCommandLine(commandLine, pidDeferred, handler, processHandlerCreated)
            // consulo: upstream runInterruptible(Dispatchers.IO) { }
            .thenCompose(exitCode -> CompletableFuture.runAsync(() -> findXmlOutputAndReport(taskId, scriptPath, project), ioDispatcher())
                .thenApply(ignored -> exitCode));
    }

    /**
     * Starts {@code commandLine} and copies its output to {@code handler}.
     * When {@code handler} is destroyed, the process is destroyed too.
     * {@code pidDeferred} gets the pid when the JVM can take an attach, or after 5 seconds.
     * The function returns the exit code of the process when the process ends.
     */
    // consulo: upstream OSProcessHandler is internal in Consulo, the process handler is built by ProcessHandlerBuilderFactory
    public static CompletableFuture<Integer> runJvmCommandLine(
        GeneralCommandLine commandLine,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler,
        Function<ProcessHandler, CompletableFuture<?>> processHandlerCreated
    ) {
        ProcessHandler scriptHandler;
        try {
            scriptHandler = Application.get().getInstance(ProcessHandlerBuilderFactory.class).newBuilder(commandLine).build();
        }
        catch (ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
        scriptHandler.addProcessListener(
            new ProcessListener() {
                private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

                @Override
                public void onTextAvailable(ProcessEvent e, Key outputType) {
                    myAnsiEscapeDecoder.escapeText(e.getText(), outputType, (text, type) -> {
                        handler.notifyTextAvailable(text, type);
                    });
                }
            }
        );

        // necessary for terminating the debug process from scriptHandler when handler.destroyProcess() is called
        handler.addProcessListener(
            new ProcessListener() {
                @Override
                public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
                    if (willBeDestroyed) {
                        scriptHandler.destroyProcess();
                    }
                }
            }
        );

        return processHandlerCreated.apply(scriptHandler).thenCompose(ignored -> {
            scriptHandler.startNotify();

            // consulo: upstream scriptHandler.process.pid(); Consulo ProcessHandler.getId() is the native pid
            long pid = scriptHandler.getId();
            // We run the bash script generated via --script_path, which then starts the JVM via Unix's exec: https://en.wikipedia.org/wiki/Exec_(system_call)
            // Since the PID of the bash process is the same as that of the JVM, we wait for bash script completion before returning the pid to IDEA
            // consulo: upstream withTimeoutOrNull(5.seconds) { while (...) { ...; delay(50.milliseconds) } } and
            // runInterruptible(Dispatchers.IO) { }, both on a pooled thread
            return CompletableFuture.supplyAsync(() -> {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!scriptHandler.isProcessTerminated() && System.nanoTime() < deadline) {
                    // TODO consulo: com.intellij.debugger.impl.attach.JavaDebuggerAttachUtil.isAttachable(pid) is missing in Consulo,
                    //  so the pid is delivered after 5 seconds or when the process ends
                    // if (JavaDebuggerAttachUtil.isAttachable(pid.toString())) break
                    try {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                pidDeferred.complete(pid);
                scriptHandler.waitFor();
                Integer exitCode = scriptHandler.getExitCode();
                if (exitCode == null) {
                    throw new CancellationException();
                }
                return exitCode;
            }, ioDispatcher());
        });
    }

    private static void findXmlOutputAndReport(TaskId taskId, Path scriptPath, Project project) {
        String scriptContent;
        try {
            scriptContent = Files.readString(scriptPath);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ExecutableInfo executableInfo;
        try {
            executableInfo = ExecutableInfo.fromBazelRunScript(scriptContent);
        }
        catch (UnexpectedScriptContentException e) {
            LOG.warn(e);
            return;
        }
        Path testXml = executableInfo.getTestXml();
        if (testXml == null) {
            return;
        }
        BazelTaskEventsService taskHandler = BazelTaskEventsService.getInstance(project);
        BspClientTestNotifier testNotifier = new BspClientTestNotifier(taskHandler);

        new TestXmlParser(testNotifier).parseAndReport(
            taskId, /* ??? */
            testXml,
            JavaBazelRules.JAVA_TEST_KIND
        );
    }

    private static final Logger LOG = Logger.getInstance(RunWithScriptPath.class);

    // consulo: Dispatchers.IO
    private static Executor ioDispatcher() {
        return Application.get().getInstance(ApplicationConcurrency.class).executor();
    }
}
