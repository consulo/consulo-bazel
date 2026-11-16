package org.jetbrains.bazel.flow.modify;

import consulo.language.psi.PsiDirectory;
import consulo.util.lang.ThreeState;
import org.jetbrains.bazel.config.project;

/**
 * With single-file source roots, Java/Kotlin plugins assume that since the directory is not covered, no file can be created.
 * Hence, we just allow creating source files anywhere in the project via the respective extension points.
 */
// TODO consulo: IntelliJ CreateJavaClassActionAvailabilityOverride (<java.createClassActionAvailabilityOverride>) is missing in
//  consulo-java (CreateTemplateInPackageAction.isAvailable has no hook), so the class is not registered
public class BazelCreateJavaClassActionAvailabilityOverride {
    public ThreeState isActionAvailable(PsiDirectory[] directories) {
        if (directories.length > 0 && project.isBazelProject(directories[0].getProject())) {
            return ThreeState.YES;
        }
        else {
            return ThreeState.UNSURE;
        }
    }
}
