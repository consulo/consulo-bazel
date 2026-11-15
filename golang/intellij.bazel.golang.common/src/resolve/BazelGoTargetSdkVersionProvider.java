package org.jetbrains.bazel.golang.resolve;

import com.goide.sdk.GoSdkService;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil;
import org.jspecify.annotations.Nullable;

/**
 * rules_go with Bazel has no distinction between compile and target SDK versions,
 * unlike, e.g., go.mod` where you can set the Go version explicitly.
 */
// TODO consulo: not registered (upstream com.goide sdk.targetSdkVersionProvider). The GoTargetSdkVersionProvider extension point
//  is missing in consulo-google-go
public class BazelGoTargetSdkVersionProvider /* : GoTargetSdkVersionProvider */ {
    public boolean isApplicable(PsiFile file) {
        return org.jetbrains.bazel.config.project.isBazelProject(file.getProject());
    }

    public @Nullable String getTargetSdkVersion(PsiFile file) {
        Project project = file.getProject();
        Module module = GoWorkspaceModuleUtil.findModule(project);
        if (module == null) {
            return null;
        }
        // TODO consulo: IntelliJ GoSdkService.getSdk(module).majorVersion is missing, Consulo only has the full SDK version string
        return GoSdkService.getInstance(project).getSdkVersion(module);
    }
}
