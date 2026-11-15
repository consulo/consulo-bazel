package org.jetbrains.bazel.run;

import consulo.execution.test.sm.ServiceMessageBuilder;
import consulo.process.ProcessOutputType;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

class BazelTestProcessHandler extends BazelProcessHandler {
    private final boolean myIsIdBasedTestTree;

    BazelTestProcessHandler(
        Project project,
        CompletableFuture<?> runDeferred,
        @Nullable CompletableFuture<@Nullable Long> pidDeferred,
        boolean isIdBasedTestTree
    ) {
        super(project, runDeferred, pidDeferred);
        myIsIdBasedTestTree = isIdBasedTestTree;
    }

    BazelTestProcessHandler(Project project, CompletableFuture<?> runDeferred, @Nullable CompletableFuture<@Nullable Long> pidDeferred) {
        this(project, runDeferred, pidDeferred, false);
    }

    BazelTestProcessHandler(Project project, CompletableFuture<?> runDeferred) {
        this(project, runDeferred, null);
    }

    @Override
    protected void destroyProcessImpl() {
        // send a fake terminated test to the test console - otherwise it will show "No tests were found" instead of "Terminated"
        ServiceMessageBuilder message = ServiceMessageBuilder.testSuiteStarted("Run cancelled");
        if (myIsIdBasedTestTree) {
            message.addAttribute("nodeId", "cancelled").addAttribute("parentNodeId", "0");
        }
        notifyTextAvailable(message.toString() + "\n", ProcessOutputType.STDOUT);
        super.destroyProcessImpl();
    }
}
