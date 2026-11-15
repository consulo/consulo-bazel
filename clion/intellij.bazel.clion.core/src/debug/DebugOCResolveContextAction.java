package org.jetbrains.bazel.clion.debug;

import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.project.Project;

import java.util.concurrent.CompletableFuture;

/**
 * Asks for a C/C++ resolve configuration, then reports it.
 *
 * <p>It reads the committed {@code OCWorkspace}, so it shows the settings that code resolution uses.
 */
// TODO consulo: OCWorkspace / OCResolveConfiguration / OCCompilerSettings (com.jetbrains.cidr.lang.workspace) do not exist in
//   consulo-cpp, which has no resolve configurations (its compiler settings are CCompileSettings of a module extension). The action
//   is not registered (upstream id Bazel.CLion.DebugOCResolveContext in the group Bazel.CLion.DebugActions).
public class DebugOCResolveContextAction extends BazelDebugAction {
    // TODO consulo: CLanguageKind (com.jetbrains.cidr.lang) does not exist in consulo-cpp
    // private val CC_LANGUAGES = listOf(CLanguageKind.C, CLanguageKind.CPP)

    public DebugOCResolveContextAction() {
        super(BazelCLionCoreLocalize.actionBazelClionDebugocresolvecontextText());
    }

    @Override
    protected boolean getShowOutputInEditor() {
        return true;
    }

    @Override
    protected CompletableFuture<?> exec(Project project) {
        // TODO consulo: OCWorkspace.getInstance(project).getConfigurations(CC_CLIENT_KEY) does not exist in consulo-cpp
        // val configurations = readAction { OCWorkspace.getInstance(project).getConfigurations(CC_CLIENT_KEY) }
        // if (configurations.isEmpty()) {
        //   fail("no $CC_CLIENT_KEY resolve configuration found, the project is not synced")
        // }
        //
        // val chosen = chooseInPopup(
        //   project,
        //   BazelCLionCoreBundle.message("debug.popup.resolve.configurations.title"),
        //   configurations,
        // ) { it.displayName }
        //
        // return chosen.toJsonMap()
        return fail("no resolve configuration found, OCWorkspace is not available in consulo-cpp");
    }

    // TODO consulo: OCResolveConfiguration does not exist in consulo-cpp
    // private fun OCResolveConfiguration.toJsonMap(): Map<String, Any?> {
    //   return mapOf(
    //     "unique_id" to uniqueId,
    //     "display_name" to displayName,
    //     "cc_identifier" to getCcIdentifier(),
    //     "variant" to variant?.toString(),
    //     "sources" to sources,
    //     "languages" to CC_LANGUAGES.associate { it.displayName to getCompilerSettings(it).toJsonMap() },
    //   )
    // }

    // TODO consulo: OCCompilerSettings / CidrCompilerSwitches do not exist in consulo-cpp
    // private fun OCCompilerSettings.toJsonMap(): Map<String, Any?> {
    //   return mapOf(
    //     "compiler_kind" to compilerKind?.toString(),
    //     "compiler_executable" to compilerExecutable,
    //     "compiler_working_dir" to compilerWorkingDir,
    //     "switches" to compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty(),
    //     "headers_search_paths" to headersSearchPaths.map { it.toString() },
    //     "implicit_includes" to implicitIncludeUrls,
    //     "preprocessor_defines" to preprocessorDefines,
    //   )
    // }
}
