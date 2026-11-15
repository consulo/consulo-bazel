package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.clion.sync.CcBuildTarget;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// consulo: upstream Kotlin top-level declarations of buildEquivalenceClasses.kt; the data class CcResolveConfiguration is
// in CcResolveConfiguration.java
public final class buildEquivalenceClasses {
    private buildEquivalenceClasses() {
    }

    private static final String CC_CONFIGURATION_MARKER = "BAZEL_CC";

    // consulo: upstream `context(ctx: CcImportContext) fun buildEquivalenceClasses(...)`, the context parameter is the first
    // parameter
    public static List<CcResolveConfiguration> buildEquivalenceClasses(
        CcImportContext ctx,
        Map<WorkspaceTargetKey, @Nullable CcCompilerInfo> target2Compiler
    ) {
        // consulo: IntelliJ MultiMap
        Map<CcResolveConfiguration.EquivalenceClass, List<WorkspaceTargetKey>> equivalenceClasses = new LinkedHashMap<>();

        for (BuildTarget target : ctx.getSnapshot().targets().allTargets().toList()) {
            CcBuildTarget info = BuildTarget.extractData(target, CcBuildTarget.class);
            if (info == null) {
                continue;
            }

            CcCompilerInfo compilerSettings = target2Compiler.get(target.getKey());
            if (compilerSettings == null) {
                continue;
            }

            CcBuildTarget.@Nullable RuleContext ruleContext = info.ruleContext();
            CcResolveConfiguration.EquivalenceClass configuration = new CcResolveConfiguration.EquivalenceClass(
                target.getKey().configuration(),
                compilerSettings,
                ruleContext != null ? ruleContext.copts().asList() : List.of(),
                ruleContext != null ? ruleContext.conlyopts().asList() : List.of(),
                ruleContext != null ? ruleContext.cxxopts().asList() : List.of(),
                info.compilationContext().includes(),
                info.compilationContext().quoteIncludes(),
                info.compilationContext().systemIncludes(),
                info.compilationContext().defines().asList()
            );

            equivalenceClasses.computeIfAbsent(configuration, it -> new ArrayList<>()).add(target.getKey());
        }

        List<CcResolveConfiguration> result = new ArrayList<>();
        for (Map.Entry<CcResolveConfiguration.EquivalenceClass, List<WorkspaceTargetKey>> entry : equivalenceClasses.entrySet()) {
            CcResolveConfiguration configuration = createResolveConfiguration(ctx, entry.getKey(), entry.getValue());
            if (configuration != null) {
                result.add(configuration);
            }
        }
        return result;
    }

    private static @Nullable CcResolveConfiguration createResolveConfiguration(
        CcImportContext ctx,
        CcResolveConfiguration.EquivalenceClass clazz,
        Collection<WorkspaceTargetKey> targets
    ) {
        List<String> sources = collectSources(ctx, targets);
        if (sources.isEmpty()) {
            return null;
        }

        return new CcResolveConfiguration(
            new CcResolveConfiguration.Identifier(clazz.hashCode(), clazz.configuration()),
            computeDisplayName(ctx, targets),
            clazz,
            List.copyOf(targets),
            sources
        );
    }

    private static List<String> collectSources(CcImportContext ctx, Collection<WorkspaceTargetKey> targets) {
        return targets.stream()
            .map(it -> ctx.getSnapshot().targets().findTargetByKey(it, TargetLoadOptions.ALL))
            .filter(Objects::nonNull)
            .flatMap(target -> target.getSources().getOutputLocations())
            .map(it -> ctx.getOutputResolver().resolve(it, RepoMapping.getLocalRepositories(ctx.getSnapshot().repoMapping())))
            .filter(Objects::nonNull)
            // consulo: CIDR OCFileTypeHelpers.isSourceFile -> the consulo-cpp file type of the name
            .filter(it -> buildWorkspaceModel.isSourceFile(it.getFileName().toString()))
            // consulo: VirtualFileUrl -> VFS url string
            .map(it -> VirtualFileUtil.pathToUrl(it.toString()))
            .toList();
    }

    private static String computeDisplayName(CcImportContext ctx, Collection<WorkspaceTargetKey> targets) {
        WorkspaceTargetKey minTargetKey = targets.stream().min(Comparator.comparing(WorkspaceTargetKey::label)).orElseThrow();

        StringBuilder builder = new StringBuilder();
        builder.append(minTargetKey.label());
        String checksum = minTargetKey.configuration().getShortChecksum();
        if (checksum != null) {
            builder.append(" (");
            WorkspaceConfiguration configuration = ctx.getSnapshot().configurations().get(minTargetKey.configuration());
            String mnemonic = configuration != null ? configuration.summary().mnemonic() : null;
            if (mnemonic != null && !mnemonic.isBlank()) {
                builder.append(mnemonic).append(' ');
            }
            builder.append(checksum).append(')');
        }

        if (targets.size() > 1) {
            builder.append(BazelCLionCommonLocalize.ccResolveConfigurationOtherTargets(targets.size() - 1).get());
        }
        return builder.toString();
    }

    public static String encode(CcResolveConfiguration.Identifier receiver) {
        return CC_CONFIGURATION_MARKER + ":" + receiver.getHashCode() + ":" + receiver.getConfigurationId().getShortChecksum();
    }

    // consulo: upstream extension of CIDR OCResolveConfiguration, here of its counterpart CcWorkspaceModel.Configuration
    public static CcResolveConfiguration.@Nullable Identifier getCcIdentifier(CcWorkspaceModel.Configuration receiver) {
        String[] parts = receiver.getUniqueId().split(":", -1);
        if (parts.length != 3) {
            return null;
        }

        String marker = parts[0];
        String hashCode = parts[1];
        String configurationId = parts[2];
        if (!marker.equals(CC_CONFIGURATION_MARKER)) {
            return null;
        }

        int parsedHashCode;
        try {
            parsedHashCode = Integer.parseInt(hashCode);
        }
        catch (NumberFormatException e) {
            return null;
        }

        return new CcResolveConfiguration.Identifier(
            parsedHashCode,
            WorkspaceConfigurationId.of(configurationId)
        );
    }
}
