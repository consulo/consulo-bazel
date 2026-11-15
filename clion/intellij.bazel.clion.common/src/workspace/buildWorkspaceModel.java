package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.workspace.CcCompilerSwitches;
import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.cpp.lang.CPPSourceFileType;
import consulo.cpp.lang.CSourceFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jetbrains.bazel.clion.workspace.copts.applyCopts;
import org.jetbrains.bsp.protocol.OutputLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * consulo: upstream Kotlin top-level declarations of buildWorkspaceModel.kt. Upstream writes CLion's OC workspace
 * ({@code OCWorkspace.ModifiableModel}); here its counterpart {@link CcWorkspaceModel}, which consulo-cpp reads through
 * {@code consulo.bazel.cpp.workspace.CcFileOptionsContributor}. The CIDR language kinds ({@code CLanguageKind.C},
 * {@code CLanguageKind.CPP}) are the consulo-cpp file types {@link CSourceFileType} and {@link CPPSourceFileType}.
 */
public final class buildWorkspaceModel {
    private buildWorkspaceModel() {
    }

    private static final FileType DEFAULT_LANGUAGE_KIND = CPPSourceFileType.INSTANCE;

    // consulo: upstream `context(ctx: CcImportContext) internal suspend fun buildWorkspaceModel(model, configs)`; it does
    // not suspend, so it is synchronous
    public static void buildWorkspaceModel(CcImportContext ctx, CcWorkspaceModel model, List<CcResolveConfiguration> configs) {
        for (CcResolveConfiguration config : configs) {
            // consulo: upstream passes variant = null
            CcWorkspaceModel.Configuration workspaceConfig =
                model.addConfiguration(buildEquivalenceClasses.encode(config.id()), config.name());

            CcCompilerInfo settings = config.shared().compilerSettings();

            CcCompilerSwitches cSwitches = buildSwitches(
                ctx,
                config,
                settings.cCompilerKind(),
                settings.cSwitches(),
                config.shared().copts(),
                config.shared().conlyopts()
            );

            CcCompilerSwitches cppSwitches = buildSwitches(
                ctx,
                config,
                settings.cppCompilerKind(),
                settings.cppSwitches(),
                config.shared().copts(),
                config.shared().cxxopts()
            );

            for (String file : config.sources()) {
                FileType languageKind = getDeclaredLanguageKind(fileName(file));
                CcWorkspaceModel.CompilerSettings fileConfig = workspaceConfig.addSource(file, languageKind);

                if (languageKind == CSourceFileType.INSTANCE) {
                    fileConfig.setCompiler(settings.cCompilerKind(), settings.cCompiler(), ctx.getExecroot());
                    fileConfig.setCompilerSwitches(cSwitches);
                }

                if (languageKind == CPPSourceFileType.INSTANCE) {
                    fileConfig.setCompiler(settings.cppCompilerKind(), settings.cppCompiler(), ctx.getExecroot());
                    fileConfig.setCompilerSwitches(cppSwitches);
                }
            }

            CcWorkspaceModel.CompilerSettings cSettings = workspaceConfig.getLanguageCompilerSettings(CSourceFileType.INSTANCE);
            cSettings.setCompiler(settings.cCompilerKind(), settings.cCompiler(), ctx.getExecroot());
            cSettings.setCompilerSwitches(
                buildSwitches(ctx, config, settings.cCompilerKind(), settings.cSwitches(), config.shared().conlyopts())
            );

            CcWorkspaceModel.CompilerSettings cppSettings = workspaceConfig.getLanguageCompilerSettings(CPPSourceFileType.INSTANCE);
            cppSettings.setCompiler(settings.cppCompilerKind(), settings.cppCompiler(), ctx.getExecroot());
            cppSettings.setCompilerSwitches(
                buildSwitches(ctx, config, settings.cppCompilerKind(), settings.cppSwitches(), config.shared().cxxopts())
            );
        }
    }

    /**
     * Builds the switches of one language. Every list of {@code options} passes the copts processing.
     */
    @SafeVarargs
    private static CcCompilerSwitches buildSwitches(
        CcImportContext ctx,
        CcResolveConfiguration config,
        CcCompilerKind kind,
        List<String>... options
    ) {
        CcCompilerSwitches.Builder builder = CcCompilerSwitches.getBuilder(kind);
        appendCompilationContext(ctx, builder, config);
        for (List<String> it : options) {
            applyCopts.applyCopts(ctx, builder, kind, it);
        }
        return builder.build();
    }

    private static void appendCompilationContext(CcImportContext ctx, CcCompilerSwitches.Builder receiver, CcResolveConfiguration config) {
        config.shared().transitiveDefines().forEach(receiver::withMacro);
        resolve(ctx, config.shared().transitiveIncludes().getOutputLocations()).forEach(receiver::withIncludePath);
        resolve(ctx, config.shared().transitiveQuoteIncludes().getOutputLocations()).forEach(receiver::withQuoteIncludePath);
        resolve(ctx, config.shared().transitiveSystemIncludes().getOutputLocations()).forEach(receiver::withSystemIncludePath);
    }

    /**
     * consulo: upstream is private; the file options contributor uses it for the files that are not a source of a
     * configuration, CIDR OCFileTypeHelpers is replaced by the consulo-cpp file type of the name
     */
    public static FileType getDeclaredLanguageKind(String sourceOrHeaderFileName) {
        String name = sourceOrHeaderFileName;

        if (isSourceFile(name)) {
            return FileTypeRegistry.getInstance().getFileTypeByFileName(name);
        }

        return DEFAULT_LANGUAGE_KIND;
    }

    /**
     * consulo: CIDR {@code OCFileTypeHelpers.isSourceFile(name)}: a C or C++ source (not a header) by the consulo-cpp
     * file type of its name.
     * <p>
     * TODO consulo: CIDR also knows the sources of the Objective-C and CUDA dialects; consulo-cpp registers them in other
     *  plugins (e.g. consulo-cuda), with file types of their own
     */
    static boolean isSourceFile(String name) {
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(name);
        return fileType == CSourceFileType.INSTANCE || fileType == CPPSourceFileType.INSTANCE;
    }

    // consulo: VirtualFileUrl.fileName of a VFS url string
    private static String fileName(String url) {
        int slash = url.lastIndexOf('/');
        return slash < 0 ? url : url.substring(slash + 1);
    }

    private static Stream<String> resolve(CcImportContext ctx, Stream<OutputLocation> receiver) {
        // TODO: again, do we want to report errors when a resolve fails?
        return receiver.map(it -> utils.resolveOutputLocation(ctx, it)).filter(Objects::nonNull).map(Path::toString);
    }
}
