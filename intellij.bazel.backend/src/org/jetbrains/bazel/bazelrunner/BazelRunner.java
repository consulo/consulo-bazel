package org.jetbrains.bazel.bazelrunner;

import consulo.project.Project;
import org.jetbrains.bazel.bazelrunner.BazelCommand.BazelCommandExecutionDescriptor;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.workspace.BazelExecutableProvider;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jetbrains.bazel.bazelrunner.params.BazelFlag.color;
import static org.jetbrains.bazel.bazelrunner.params.BazelFlag.curses;

/**
 * Runs Bazel commands with proper repository configuration.
 */
public class BazelRunner {
    private final @Nullable BazelTaskEventsHandler myTaskEventsHandler;
    private final Path myWorkspaceRoot;
    private final BazelProcessLauncher myBazelProcessLauncher;
    private final Path myBazelBinary;

    public BazelRunner(
        @Nullable BazelTaskEventsHandler taskEventsHandler,
        Path workspaceRoot,
        BazelProcessLauncher bazelProcessLauncher,
        Path bazelBinary
    ) {
        myTaskEventsHandler = taskEventsHandler;
        myWorkspaceRoot = workspaceRoot;
        myBazelProcessLauncher = bazelProcessLauncher;
        myBazelBinary = bazelBinary;
    }

    public Path getWorkspaceRoot() {
        return myWorkspaceRoot;
    }

    public static CompletableFuture<BazelRunner> create(
        Project project,
        @Nullable BazelTaskEventsHandler taskEventsHandler,
        Path workspaceRoot,
        BazelProcessLauncher bazelProcessLauncher,
        ProjectView projectView
    ) {
        @Nullable Path projectViewBazelBinary = ProjectViewExtensions.getBazelBinary(projectView);
        CompletableFuture<Path> bazelBinary =
            projectViewBazelBinary != null
                ? CompletableFuture.completedFuture(workspaceRoot.resolve(projectViewBazelBinary))
                : BazelExecutableProvider.computeBazelExecutableOrFail(project);
        return bazelBinary.thenApply(it -> new BazelRunner(taskEventsHandler, workspaceRoot, bazelProcessLauncher, it));
    }

    // consulo: the upstream builder lambdas with receiver (`BazelCommand.X.() -> Unit`) are Consumer<BazelCommand.X>,
    // the overloads without a builder replace the default argument `{}`
    public static class CommandBuilder {
        private final String myBazelBinary;
        private boolean myInheritWorkspaceOptions = false;

        public CommandBuilder(String bazelBinary) {
            myBazelBinary = bazelBinary;
        }

        public boolean getInheritWorkspaceOptions() {
            return myInheritWorkspaceOptions;
        }

        public void setInheritWorkspaceOptions(boolean inheritWorkspaceOptions) {
            myInheritWorkspaceOptions = inheritWorkspaceOptions;
        }

        public BazelCommand.Clean clean() {
            return clean(it -> {
            });
        }

        public BazelCommand.Clean clean(Consumer<BazelCommand.Clean> builder) {
            return apply(new BazelCommand.Clean(myBazelBinary), builder);
        }

        public BazelCommand.ShutDown shutDown() {
            return shutDown(it -> {
            });
        }

        public BazelCommand.ShutDown shutDown(Consumer<BazelCommand.ShutDown> builder) {
            return apply(new BazelCommand.ShutDown(myBazelBinary), builder);
        }

        public BazelCommand.Info info() {
            return info(it -> {
            });
        }

        public BazelCommand.Info info(Consumer<BazelCommand.Info> builder) {
            return apply(new BazelCommand.Info(myBazelBinary), builder);
        }

        public BazelCommand.Run run(Label target) {
            return run(target, it -> {
            });
        }

        public BazelCommand.Run run(Label target, Consumer<BazelCommand.Run> builder) {
            BazelCommand.Run command = apply(new BazelCommand.Run(myBazelBinary, target), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.ModGraph graph() {
            return graph(it -> {
            });
        }

        public BazelCommand.ModGraph graph(Consumer<BazelCommand.ModGraph> builder) {
            return apply(new BazelCommand.ModGraph(myBazelBinary), builder);
        }

        public BazelCommand.ModPath path() {
            return path(it -> {
            });
        }

        public BazelCommand.ModPath path(Consumer<BazelCommand.ModPath> builder) {
            return apply(new BazelCommand.ModPath(myBazelBinary), builder);
        }

        public BazelCommand.ModShowRepo showRepo() {
            return showRepo(it -> {
            });
        }

        public BazelCommand.ModShowRepo showRepo(Consumer<BazelCommand.ModShowRepo> builder) {
            return apply(new BazelCommand.ModShowRepo(myBazelBinary), builder);
        }

        public BazelCommand.ModDumpRepoMapping dumpRepoMapping() {
            return dumpRepoMapping(it -> {
            });
        }

        public BazelCommand.ModDumpRepoMapping dumpRepoMapping(
            Consumer<BazelCommand.ModDumpRepoMapping> builder
        ) {
            return apply(new BazelCommand.ModDumpRepoMapping(myBazelBinary), builder);
        }

        public BazelCommand.Query query() {
            return query(true);
        }

        public BazelCommand.Query query(boolean allowManualTargetsSync) {
            return query(allowManualTargetsSync, it -> {
            });
        }

        public BazelCommand.Query query(Consumer<BazelCommand.Query> builder) {
            return query(true, builder);
        }

        public BazelCommand.Query query(boolean allowManualTargetsSync, Consumer<BazelCommand.Query> builder) {
            return apply(new BazelCommand.Query(myBazelBinary, allowManualTargetsSync), builder);
        }

        public BazelCommand.Config config(Consumer<BazelCommand.Config> builder) {
            BazelCommand.Config command = apply(new BazelCommand.Config(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.QueryExpression fileQuery(List<Path> filePaths) {
            return fileQuery(filePaths, it -> {
            });
        }

        /**
         * Special version of `query` for asking Bazel about files instead of a target
         */
        public BazelCommand.QueryExpression fileQuery(List<Path> filePaths, Consumer<BazelCommand.QueryExpression> builder) {
            String fileString = filePaths.stream()
                .map(it -> "\"" + invariantSeparatorsPathString(it) + "\"")
                .collect(Collectors.joining(" ", "set(", ")"));
            return queryExpression(fileString, builder);
        }

        public BazelCommand.QueryExpression queryExpression(String expression) {
            return queryExpression(expression, it -> {
            });
        }

        /**
         * Purest form of `query`, asking for exact string to execute instead of `Label`s
         */
        public BazelCommand.QueryExpression queryExpression(String expression, Consumer<BazelCommand.QueryExpression> builder) {
            return apply(new BazelCommand.QueryExpression(myBazelBinary, expression), builder);
        }

        public BazelCommand.CQuery cquery() {
            return cquery(it -> {
            });
        }

        public BazelCommand.CQuery cquery(Consumer<BazelCommand.CQuery> builder) {
            BazelCommand.CQuery command = apply(new BazelCommand.CQuery(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.AQuery aquery() {
            return aquery(it -> {
            });
        }

        public BazelCommand.AQuery aquery(Consumer<BazelCommand.AQuery> builder) {
            BazelCommand.AQuery command = apply(new BazelCommand.AQuery(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.Build build() {
            return build(it -> {
            });
        }

        public BazelCommand.Build build(Consumer<BazelCommand.Build> builder) {
            BazelCommand.Build command = apply(new BazelCommand.Build(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.MobileInstall mobileInstall(Label target) {
            return mobileInstall(target, it -> {
            });
        }

        public BazelCommand.MobileInstall mobileInstall(Label target, Consumer<BazelCommand.MobileInstall> builder) {
            BazelCommand.MobileInstall command = new BazelCommand.MobileInstall(myBazelBinary, target);
            // --tool_tag is not supported by mobile-install
            command.getOptions().clear();
            builder.accept(command);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.Test test() {
            return test(it -> {
            });
        }

        public BazelCommand.Test test(Consumer<BazelCommand.Test> builder) {
            BazelCommand.Test command = apply(new BazelCommand.Test(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        public BazelCommand.Coverage coverage() {
            return coverage(it -> {
            });
        }

        public BazelCommand.Coverage coverage(Consumer<BazelCommand.Coverage> builder) {
            BazelCommand.Coverage command = apply(new BazelCommand.Coverage(myBazelBinary), builder);
            myInheritWorkspaceOptions = true;
            return command;
        }

        // consulo: Kotlin `apply { builder() }`
        private static <C extends BazelCommand> C apply(C command, Consumer<C> builder) {
            builder.accept(command);
            return command;
        }

        // consulo: Kotlin Path.invariantSeparatorsPathString
        private static String invariantSeparatorsPathString(Path path) {
            String separator = path.getFileSystem().getSeparator();
            return "/".equals(separator) ? path.toString() : path.toString().replace(separator, "/");
        }
    }

    // consulo: overload for the upstream default argument
    public BazelCommand buildBazelCommand(
        ProjectView projectView,
        Function<CommandBuilder, BazelCommand> doBuild
    ) {
        return buildBazelCommand(projectView, null, doBuild);
    }

    public BazelCommand buildBazelCommand(
        ProjectView projectView,
        @Nullable Boolean inheritProjectviewOptionsOverride,
        Function<CommandBuilder, BazelCommand> doBuild
    ) {
        CommandBuilder commandBuilder = new CommandBuilder(myBazelBinary.toString());
        BazelCommand command = doBuild.apply(commandBuilder);

        if (command.getEnablePty()) {
            command.getOptions().addAll(
                List.of(
                    curses(true),
                    color(true)
                )
            );
        }
        else {
            command.getOptions().addAll(
                List.of(
                    curses(false),
                    color(true),
                    "--noprogress_in_terminal_title"
                )
            );
        }

        if (inheritProjectviewOptionsOverride != null) {
            commandBuilder.setInheritWorkspaceOptions(inheritProjectviewOptionsOverride);
        }

        if (commandBuilder.getInheritWorkspaceOptions()) {
            command.getOptions().addAll(ProjectViewExtensions.getBuildFlags(projectView));
        }

        return command;
    }

    // consulo: overload for the upstream default argument
    public BazelProcess runBazelCommand(
        BazelCommand command,
        @Nullable TaskId taskId
    ) {
        return runBazelCommand(command, taskId, true);
    }

    public BazelProcess runBazelCommand(
        BazelCommand command,
        @Nullable TaskId taskId,
        boolean logProcessOutput
    ) {
        return runBazelCommand(command.buildExecutionDescriptor(), taskId, logProcessOutput);
    }

    // consulo: overload for the upstream default argument
    public BazelProcess runBazelCommand(
        BazelCommandExecutionDescriptor executionDescriptor,
        @Nullable TaskId taskId
    ) {
        return runBazelCommand(executionDescriptor, taskId, true);
    }

    public BazelProcess runBazelCommand(
        BazelCommandExecutionDescriptor executionDescriptor,
        @Nullable TaskId taskId,
        boolean logProcessOutput
    ) {
        Runnable finishCallback = executionDescriptor.finishCallback();
        List<String> processArgs = executionDescriptor.command();
        Map<String, String> environment = executionDescriptor.environment();

        @Nullable BazelTaskEventsHandler taskEventsHandler = myTaskEventsHandler;
        @Nullable BazelTaskLogger taskLogger =
            taskId != null && taskEventsHandler != null ? BazelTaskEventsHandler.asLogger(taskEventsHandler, taskId) : null;
        if (taskLogger != null) {
            String log = envToString(environment) + " " + String.join(" ", processArgs);
            taskLogger.info(log);
        }
        @Nullable BazelTaskLogger outputLogger = null;
        if (taskLogger != null) {
            outputLogger = logProcessOutput ? taskLogger : new BazelTaskLoggerWithoutProcessOutput(taskLogger);
        }

        Process process = myBazelProcessLauncher.launchProcess(executionDescriptor);
        boolean showStdout = BazelFeatureFlags.getShowBazelStdout() || executionDescriptor.alwaysShowStdout();

        BazelProcess bazelProcess = new BazelProcess(
            process,
            showStdout,
            outputLogger,
            finishCallback
        );
        BazelLog.write(writer -> {
            writer.append(String.join(" ", executionDescriptor.command())).append('\n');
            writer.append(envToString(environment)).append('\n');
        });
        return bazelProcess;
    }

    private String envToString(Map<String, String> environment) {
        return environment.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining(" "));
    }
}

final class BazelTaskLoggerWithoutProcessOutput implements BazelTaskLogger {
    private final BazelTaskLogger myDelegate;

    BazelTaskLoggerWithoutProcessOutput(BazelTaskLogger delegate) {
        myDelegate = delegate;
    }

    // consulo: Kotlin `BazelTaskLogger by delegate`
    @Override
    public void info(String message) {
        myDelegate.info(message);
    }

    @Override
    public void message(String message) {
        // Do nothing
    }

    @Override
    public void warn(String message) {
        myDelegate.warn(message);
    }

    @Override
    public void error(String errorMessage) {
        myDelegate.error(errorMessage);
    }
}
