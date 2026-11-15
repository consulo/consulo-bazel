package org.jetbrains.bazel.clion.debug;

import consulo.annotation.component.ActionImpl;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Reports every Bazel configuration that the workspace snapshot holds.
 */
// consulo: the group Bazel.CLion.DebugActions (no class upstream) is written by the coordinator
@ActionImpl(id = "Bazel.CLion.DebugBazelConfiguration")
public class DebugBazelConfigurationAction extends BazelDebugAction {
    public DebugBazelConfigurationAction() {
        super(BazelCLionCoreLocalize.actionBazelClionDebugbazelconfigurationText());
    }

    @Override
    protected CompletableFuture<?> exec(Project project) {
        return snapshotOrFail(project).thenApply(snapshot -> {
            if (snapshot.configurations().isEmpty()) {
                fail("no configuration found in the workspace snapshot");
            }

            List<Map<String, @Nullable Object>> result = snapshot.configurations().entrySet().stream()
                .sorted(Comparator.comparing(
                    (Map.Entry<WorkspaceConfigurationId, WorkspaceConfiguration> it) ->
                        StringUtil.notNullize(it.getKey().getShortChecksum())
                ))
                .map(it -> toJsonMap(it.getValue()))
                .toList();
            return result;
        });
    }

    private static Map<String, @Nullable Object> toJsonMap(WorkspaceConfiguration receiver) {
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        result.put("id", receiver.id());
        result.put("hash", receiver.summary().hash());
        result.put("mnemonic", receiver.summary().mnemonic());
        result.put("platform_name", receiver.summary().platformName());
        result.put("cpu", receiver.summary().cpu());
        result.put("is_tool", receiver.summary().isTool());
        return result;
    }
}
