package org.jetbrains.bazel.server.sync.firstPhase;

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewTargets;
import org.jetbrains.bazel.server.model.BazelSyncProject.PhasedSyncProject;
import org.jetbrains.bsp.protocol.TaskId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FirstPhaseProjectResolver {
    private final Path myWorkspaceRoot;
    private final BazelRunner myBazelRunner;
    private final ProjectView myProjectView;
    private final BazelInfo myBazelInfo;

    public FirstPhaseProjectResolver(
        Path workspaceRoot,
        BazelRunner bazelRunner,
        ProjectView projectView,
        BazelInfo bazelInfo
    ) {
        myWorkspaceRoot = workspaceRoot;
        myBazelRunner = bazelRunner;
        myProjectView = projectView;
        myBazelInfo = bazelInfo;
    }

    public CompletableFuture<PhasedSyncProject> resolve(TaskId taskId) {
        // consulo: upstream coroutineScope
        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.query(query -> {
                    query.getOptions().add("--output=streamed_proto");
                    query.getOptions().add(BazelFlag.keepGoing());

                    query.addTargetsFromExcludableList(ProjectViewTargets.getTargets(myProjectView));
                })
            );

        return myBazelRunner
            .runBazelCommand(command, taskId, false)
            .waitAndGetResult()
            .thenApply(bazelResult -> {
                InputStream inputStream = new ByteArrayInputStream(bazelResult.getStdout());
                // consulo: upstream generateSequence { Target.parseDelimitedFrom(inputStream) }.associateBy { ... }
                Map<Label, Target> modules = new LinkedHashMap<>();
                try {
                    Target target;
                    while ((target = Target.parseDelimitedFrom(inputStream)) != null) {
                        modules.put(Label.parse(target.getRule().getName()), target);
                    }
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                return new PhasedSyncProject(
                    myWorkspaceRoot,
                    myBazelInfo.release(),
                    modules,
                    bazelResult.isNotSuccess()
                );
            });
    }
}
