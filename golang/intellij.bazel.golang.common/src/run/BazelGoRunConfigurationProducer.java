package org.jetbrains.bazel.golang.run;

import com.goide.psi.GoFunctionDeclaration;
import com.goide.psi.GoFunctionOrMethodDeclaration;
import com.goide.runconfig.GoRunUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.go.GoConstants;
import consulo.go.language.testing.GoTestFinder;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * this impl is inspired by the OG impl <a href="https://github.com/bazelbuild/intellij/blob/master/golang/src/com/google/idea/blaze/golang/run/producers/GoTestContextProvider.java">here</a>
 */
@ExtensionImpl
public class BazelGoRunConfigurationProducer extends BazelRunConfigurationProducer {
    @Override
    @RequiredReadAction
    public @Nullable GutterAction getGutterAction(PsiElement element, BuildTargetInfo target) {
        if (isMainFunction(element)) {
            return new GutterAction();
        }
        if (!GoTestFinder.isTestFile(element.getContainingFile())) {
            return null;
        }
        // consulo: GoRunConfigurationProducerBase.isPackageContext -> GoRunUtil.isPackageContext
        if (GoRunUtil.isPackageContext(element)) {
            return new GutterAction();
        }
        // TODO consulo: GoTestFinder.findTestFunctionInContext(element) is missing in consulo-google-go (a private method of
        //  GoTestRunConfigurationProducerBase there)
        // val function = GoTestFinder.findTestFunctionInContext(element) ?: return null
        GoFunctionOrMethodDeclaration function = null;
        if (function == null) {
            return null;
        }
        String rawTestFilter = calculateRawTestFilterForElement(element, function);
        String testFilter = rawTestFilter != null ? regexifyTestFilter(rawTestFilter) : null;
        if (testFilter == null) {
            return null;
        }
        return new GutterAction(testFilter, List.of(), Map.of(), rawTestFilter);
    }

    @RequiredReadAction
    private static boolean isMainFunction(PsiElement receiver) {
        return GoRunUtil.isMainGoFile(receiver.getContainingFile()) &&
            receiver.getParent() instanceof GoFunctionDeclaration declaration &&
            GoConstants.MAIN.equals(declaration.getName());
    }

    /**
     * Given a code element, calculate the test filter we'd need to run exactly that element.
     * It takes into account nested tests.
     * Here is an example of the expected results of clicking on each section:
     * <pre>
     * func Test(t *testing.T) {                    // returns "Test"
     * t.Run("with_nested", func(t *testing.T) {  // returns "Test/with_nested"
     * t.Run("subtest", func(t *testing.T) {}) // returns "Test/with_nested/subtest"
     * })
     * }
     * </pre>
     * When a user clicks in the "&gt;" button, we get pointed to the "Run" part of "t.Run".
     * We need to walk the tree up to see the function call that has the actual argument.
     * <p>
     * This function doesn't worry about turning this filter into a regex which can be passed to --test_filter.
     *
     * @param element           Element the user has clicked on.
     * @param enclosingFunction Go function encompassing this test.
     * @return String representation of the proper test filter.
     * @see #regexifyTestFilter for converting this to a --test_filter value.
     */
    @RequiredReadAction
    private static @Nullable String calculateRawTestFilterForElement(PsiElement element, GoFunctionOrMethodDeclaration enclosingFunction) {
        PsiElement parent = element.getParent();
        if (parent != null && parent.isEquivalentTo(enclosingFunction)) {
            return enclosingFunction.getName();
        }
        else {
            // TODO consulo: GoTestRunConfigurationProducerBase.findSubTestInContext(element, enclosingFunction) is missing in
            //  consulo-google-go
            // GoTestRunConfigurationProducerBase.findSubTestInContext(element, enclosingFunction)
            return null;
        }
    }

    // consulo: upstream top-level functions of BazelGoRunConfigurationProducer.kt

    public static String regexifyTestFilter(String text) {
        return "^" + escapeRegexChars(text) + "$";
    }

    private static String escapeRegexChars(String name) {
        StringBuilder output = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (isRegexCharNeedingEscaping(c)) {
                output.append("\\");
            }
            output.append(c);
        }
        return output.toString();
    }

    private static boolean isRegexCharNeedingEscaping(char c) {
        // Taken from https://cs.opensource.google/go/go/+/refs/tags/go1.21.4:src/regexp/regexp.go;l=720
        return c == '\\' ||
            c == '.' ||
            c == '+' ||
            c == '*' ||
            c == '?' ||
            c == '(' ||
            c == ')' ||
            c == '|' ||
            c == '[' ||
            c == ']' ||
            c == '{' ||
            c == '}' ||
            c == '^' ||
            c == '$';
    }
}
