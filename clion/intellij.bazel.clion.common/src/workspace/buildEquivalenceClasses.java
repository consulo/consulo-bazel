package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.clion.sync.CcBuildTarget;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
        List<BuildTarget> buildTargets = targets.stream()
            .map(it -> ctx.getSnapshot().targets().findTargetByKey(it))
            .filter(Objects::nonNull)
            .toList();

        List<String> sources = collectSources(ctx, buildTargets);
        // a file in srcs and in hdrs is compiled, so it is a source
        Set<String> sourceSet = Set.copyOf(sources);
        List<String> headers = collectHeaders(ctx, buildTargets).stream().filter(it -> !sourceSet.contains(it)).toList();

        if (sources.isEmpty() && headers.isEmpty()) {
            return null;
        }

        return new CcResolveConfiguration(
            new CcResolveConfiguration.Identifier(clazz.hashCode(), clazz.configuration()),
            computeDisplayName(ctx, targets),
            clazz,
            List.copyOf(targets),
            sources,
            headers
        );
    }

    /**
     * Only actual source files in srcs are compiled by rules_cc.
     */
    private static List<String> collectSources(CcImportContext ctx, List<BuildTarget> targets) {
        return resolveFiles(
            ctx,
            targets.stream().flatMap(target -> target.getSources().getOutputLocations()),
            // consulo: CIDR OCFileTypeHelpers.isSourceFile -> the consulo-cpp file type of the name
            buildWorkspaceModel::isSourceFile,
            false
        ).toList();
    }

    /**
     * All headers in srcs and hdrs are available for compilation of the sources.
     */
    private static List<String> collectHeaders(CcImportContext ctx, List<BuildTarget> targets) {
        Stream<String> privateHeaders = resolveFiles(
            ctx,
            targets.stream().flatMap(target -> target.getSources().getOutputLocations()),
            // consulo: CIDR OCFileTypeHelpers.isHeaderFile -> the consulo-cpp file type of the name
            buildWorkspaceModel::isHeaderFile,
            false
        );

        Stream<String> directHeaders = resolveFiles(
            ctx,
            targets.stream().flatMap(buildEquivalenceClasses::directHeaders),
            name -> buildWorkspaceModel.isSourceFile(name) || buildWorkspaceModel.isHeaderFile(name),
            true
        );

        // a header in both srcs and hdrs reaches both sequences
        Set<String> result = new LinkedHashSet<>();
        privateHeaders.forEach(result::add);
        directHeaders.forEach(result::add);
        return List.copyOf(result);
    }

    /**
     * {@code collapseReExports} replaces a header that {@code strip_include_prefix} re-exports with its source file.
     */
    private static Stream<String> resolveFiles(
        CcImportContext ctx,
        Stream<OutputLocation> receiver,
        Predicate<String> filter,
        boolean collapseReExports
    ) {
        return receiver
            .map(location -> {
                Path path = ctx.getOutputResolver().resolve(location, RepoMapping.getLocalRepositories(ctx.getSnapshot().repoMapping()));
                return path != null ? Map.entry(location, path) : null;
            })
            .filter(Objects::nonNull)
            .filter(it -> filter.test(it.getValue().getFileName().toString()))
            .map(it -> collapseReExports && OutputLocation.isGenerated(it.getKey()) ? originalFile(it.getValue()) : it.getValue())
            .distinct()
            // consulo: VirtualFileUrl -> VFS url string
            .map(it -> VirtualFileUtil.pathToUrl(it.toString()));
    }

    /**
     * Returns the source file of a symlinked output, which the sync creates for CPP-52156.
     */
    private static Path originalFile(Path receiver) {
        if (Files.isSymbolicLink(receiver)) {
            Path realPath = PathUtil.toRealPathOrNull(receiver);
            return realPath != null ? realPath : receiver;
        }
        return receiver;
    }

    private static Stream<OutputLocation> directHeaders(BuildTarget receiver) {
        CcBuildTarget data = BuildTarget.extractData(receiver, CcBuildTarget.class);
        if (data == null) {
            return Stream.empty();
        }
        CcBuildTarget.CompilationContext compilationContext = data.compilationContext();
        return compilationContext.directHeaders().getOutputLocations();
    }

    private static String computeDisplayName(CcImportContext ctx, Collection<WorkspaceTargetKey> targets) {
        WorkspaceTargetKey minTargetKey = targets.stream().min(Comparator.comparing(WorkspaceTargetKey::label)).orElseThrow();

        StringBuilder builder = new StringBuilder();
        builder.append(displayName(minTargetKey, ctx.getSnapshot().configurations()));

        if (targets.size() > 1) {
            builder.append(BazelCLionCommonLocalize.ccResolveConfigurationOtherTargets(targets.size() - 1).get());
        }
        return builder.toString();
    }

    public static String displayName(WorkspaceTargetKey receiver, Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations) {
        StringBuilder builder = new StringBuilder();
        builder.append(receiver.label());
        String checksum = receiver.configuration().getShortChecksum();
        if (checksum != null) {
            builder.append(" (");
            WorkspaceConfiguration configuration = configurations.get(receiver.configuration());
            String mnemonic = configuration != null ? configuration.summary().mnemonic() : null;
            if (mnemonic != null && !mnemonic.isBlank()) {
                builder.append(mnemonic).append(' ');
            }
            builder.append(checksum).append(')');
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
