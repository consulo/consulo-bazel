package org.jetbrains.bazel.jvm.run;

import com.intellij.java.debugger.impl.DefaultDebugEnvironment;
import com.intellij.java.execution.configurations.RemoteConnection;
import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.console.ConsoleView;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

public abstract class JvmDebuggableCommandLineState extends BazelCommandLineStateBase {
    private final int myPort;
    private final BazelRunConfiguration myConfiguration;

    protected JvmDebuggableCommandLineState(
        ExecutionEnvironment environment,
        int port,
        BazelRunConfiguration configuration
    ) {
        super(environment);
        myPort = port;
        myConfiguration = configuration;
    }

    public DefaultDebugEnvironment createDebugEnvironment(ExecutionEnvironment environment) {
        // create remote connection with client mode
        RemoteConnection remoteConnection =
            new RemoteConnection(
                true,
                "localhost",
                String.valueOf(myPort), // this is the port used
                false
            );
        if (environment.getRunProfile() instanceof RunConfigurationBase runConfigurationBase) {
            KotlinCoroutineLibraryFinderBeforeRunTaskProvider.attachCoroutinesDebuggerConnection(runConfigurationBase);
        }
        return new DefaultDebugEnvironment(
            environment,
            this,
            remoteConnection,
            true
        );
    }

    @Override
    protected @Nullable ConsoleView createConsole(Executor executor) throws ExecutionException {
        ConsoleView console = super.createConsole(executor);
        if (console == null) {
            return null;
        }
        return JavaRunConfigurationExtensionManager.getInstance().decorateExecutionConsole(
            myConfiguration,
            getRunnerSettings(),
            console,
            executor
        );
    }
}
