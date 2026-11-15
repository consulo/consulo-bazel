package org.jetbrains.bazel.clion.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.util.ModificationTracker;
import consulo.execution.action.Location;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;

import java.util.List;

// consulo: upstream extension point org.jetbrains.bazel.ccTestSupport
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelCcTestFrameworkSupport {
    // TODO consulo: CidrTestLanguageSupport (com.jetbrains.cidr.execution.testing) does not exist in consulo-cpp, which has no C/C++
    //   test framework support (Google Test, Catch2) to detect test objects in the PSI
    // val framework: CidrTestLanguageSupport

    /**
     * Resolves a test path to its declaration or an empty list if this framework does not know the test.
     */
    List<Location> locate(String path, Project project, GlobalSearchScope scope);

    // TODO consulo: CidrTestScopeElement (com.jetbrains.cidr.execution.testing) does not exist in consulo-cpp
    // /**
    //  * The complete `--test_filter` value selecting exactly [tests], or null if no filter can be built.
    //  *
    //  * Joining is up to the implementation, since every framework spells a list of tests differently.
    //  */
    // fun createTestFilter(tests: List<CidrTestScopeElement>): String?

    ModificationTracker getLocationCacheModificationTracker(Project project);

    // consulo: upstream EP_NAME.extensionList
    static List<BazelCcTestFrameworkSupport> getExtensions() {
        return Application.get().getExtensionList(BazelCcTestFrameworkSupport.class);
    }

    // TODO consulo: CidrTestScopeElement / CidrTestLanguageSupport.findTestObject do not exist in consulo-cpp
    // fun findTestAndFramework(element: PsiElement): Pair<CidrTestScopeElement, BazelCcTestFrameworkSupport>? =
    //   EP_NAME.computeSafeIfAny { support ->
    //     val testElement = support.framework.findTestObject(element)
    //     if (testElement != null) {
    //       testElement to support
    //     }
    //     else {
    //       null
    //     }
    //   }
}
