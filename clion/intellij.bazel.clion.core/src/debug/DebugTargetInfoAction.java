package org.jetbrains.bazel.clion.debug;

import consulo.annotation.component.ActionImpl;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.project.Project;
import org.jetbrains.bazel.clion.sync.CcBuildTarget;
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.clion.workspace.utils;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Asks for a target, then reports every section of it.
 */
// consulo: the group Bazel.CLion.DebugActions (no class upstream) is written by the coordinator
@ActionImpl(id = "Bazel.CLion.DebugTargetInfo")
public class DebugTargetInfoAction extends BazelDebugAction {
    public DebugTargetInfoAction() {
        super(BazelCLionCoreLocalize.actionBazelClionDebugtargetinfoText());
    }

    @Override
    protected boolean getShowOutputInEditor() {
        return true;
    }

    @Override
    protected CompletableFuture<?> exec(Project project) {
        return snapshotOrFail(project).thenCompose(snapshot -> {
            List<BuildTargetInfo> infos = snapshot.targets().allTargetInfos().toList();
            if (infos.isEmpty()) {
                fail("no target found in the workspace snapshot");
            }

            return chooseInPopup.chooseInPopup(project, BazelCLionCoreLocalize.debugPopupTargetsTitle(), infos, it ->
                it.kind().kind() + " rule " + utils.presentable(it.key())
            ).thenApply(chosen -> {
                BuildTarget target = snapshot.targets().findTargetByKey(chosen.key());
                if (target == null) {
                    return fail("target " + chosen.key() + " is absent from the workspace snapshot");
                }

                return toJsonMap(target, snapshot.repoMapping());
            });
        });
    }

    private static Map<String, @Nullable Object> toJsonMap(BuildTarget receiver, RepoMapping repoMapping) {
        Map<String, @Nullable Object> data = new LinkedHashMap<>();
        for (BuildTargetData it : receiver.getData()) {
            data.put(it.getClass().getSimpleName(), toJsonMap(it));
        }

        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        result.put("key", receiver.getKey());
        result.put("kind", receiver.getKind());
        result.put("base_directory", BuildTargetUtils.getBaseDirectoryLocation(receiver));
        result.put("generator_name", receiver.getGeneratorName());
        result.put("is_workspace", BuildTargetUtils.isWorkspace(receiver, repoMapping));
        result.put("is_test_only", receiver.isTestOnly());
        result.put("tags", receiver.getTags());
        result.put("dependencies", receiver.getDependencies());
        result.put("sources", receiver.getSources());
        result.put("resources", receiver.getResources());
        result.put("data", data);
        return result;
    }

    private static Map<String, @Nullable Object> toJsonMap(BuildTargetData receiver) {
        if (receiver instanceof CcBuildTarget ccBuildTarget) {
            return toJsonMap(ccBuildTarget);
        }
        else if (receiver instanceof CcToolchainBuildTarget ccToolchainBuildTarget) {
            return toJsonMap(ccToolchainBuildTarget);
        }
        else {
            return Map.of();
        }
    }

    private static Map<String, @Nullable Object> toJsonMap(CcBuildTarget receiver) {
        CcBuildTarget.RuleContext it = receiver.ruleContext();
        Map<String, @Nullable Object> ruleContext = null;
        if (it != null) {
            ruleContext = new LinkedHashMap<>();
            ruleContext.put("headers", it.headers());
            ruleContext.put("textualHeaders", it.textualHeaders());
            ruleContext.put("copts", it.copts());
            ruleContext.put("conlyopts", it.conlyopts());
            ruleContext.put("cxxopts", it.cxxopts());
            ruleContext.put("args", it.args());
            ruleContext.put("include_prefix", it.includePrefix());
            ruleContext.put("strip_include_prefix", it.stripIncludePrefix());
        }

        Map<String, @Nullable Object> compilationContext = new LinkedHashMap<>();
        compilationContext.put("headers", receiver.compilationContext().headers());
        compilationContext.put("defines", receiver.compilationContext().defines());
        compilationContext.put("includes", receiver.compilationContext().includes());
        compilationContext.put("quote_includes", receiver.compilationContext().quoteIncludes());
        compilationContext.put("system_includes", receiver.compilationContext().systemIncludes());

        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        result.put("rule_context", ruleContext);
        result.put("compilationContext", compilationContext);
        return result;
    }

    private static Map<String, @Nullable Object> toJsonMap(CcToolchainBuildTarget receiver) {
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        result.put("target_name", receiver.targetName());
        result.put("compiler_name", receiver.compilerName());
        result.put("c_compiler", receiver.cCompiler());
        result.put("cpp_compiler", receiver.cppCompiler());
        result.put("c_option", receiver.cOption());
        result.put("cpp_option", receiver.cppOption());
        result.put("built_in_include_directories", receiver.builtInIncludeDirectories());
        result.put("sysroot", receiver.sysroot());
        result.put("c_environment", new TreeMap<>(receiver.cEnvironment()));
        result.put("cpp_environment", new TreeMap<>(receiver.cppEnvironment()));
        return result;
    }
}
