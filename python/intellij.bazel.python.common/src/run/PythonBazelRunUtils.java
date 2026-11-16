package org.jetbrains.bazel.python.run;

import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.impl.psi.PyUtil;
import com.jetbrains.python.impl.testing.PythonUnitTestUtil;
import com.jetbrains.python.impl.testing.pytest.PyTestUtil;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyIfStatement;
import com.jetbrains.python.psi.types.TypeEvalContext;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.python.lang.PythonLanguageClass;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PythonBazelRunUtils {
    private PythonBazelRunUtils() {
    }

    @RequiredReadAction
    public static boolean isRunLineMarkerElement(PsiElement element) {
        if (!(element.getContainingFile() instanceof PyFile pyFile)) {
            return false;
        }
        return (!getPythonBazelMainFileTargets(pyFile).isEmpty() && isIfNameMain(element)) ||
            isTestFunctionNameIdentifier(element);
    }

    @RequiredReadAction
    public static boolean isTestFunctionNameIdentifier(PsiElement element) {
        if (!(element.getContainingFile() instanceof PyFile pyFile) || getPythonBazelTestTargets(pyFile).isEmpty()) {
            return false;
        }
        ASTNode node = element.getNode();
        return node != null && node.getElementType() == PyTokenTypes.IDENTIFIER &&
            getTestFunctionFromNameIdentifier(element) != null;
    }

    @RequiredReadAction
    public static List<String> getTestRunnerArguments(PsiElement element) {
        PyFunction function = getTestFunctionFromNameIdentifier(element);
        String argument = function != null ? getTestRunnerArgument(function) : null;
        return argument != null ? List.of(argument) : List.of();
    }

    // consulo: upstream private top-level extensions of PythonBazelRunUtils.kt

    @RequiredReadAction
    private static List<BuildTargetInfo> getPythonBazelMainFileTargets(PyFile receiver) {
        List<BuildTargetInfo> result = new ArrayList<>();
        for (BuildTargetInfo it : getPythonBazelTargets(receiver)) {
            if (isMainFileInTarget(receiver, it)) {
                result.add(it);
            }
        }
        return result;
    }

    @RequiredReadAction
    private static List<BuildTargetInfo> getPythonBazelTestTargets(PyFile receiver) {
        List<BuildTargetInfo> result = new ArrayList<>();
        for (BuildTargetInfo it : getPythonBazelTargets(receiver)) {
            if (it.kind().ruleType() == RuleType.TEST) {
                result.add(it);
            }
        }
        return result;
    }

    @RequiredReadAction
    private static List<BuildTargetInfo> getPythonBazelTargets(PyFile receiver) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(receiver.getProject());
        VirtualFile file = receiver.getVirtualFile();
        if (file == null) {
            return List.of();
        }
        List<BuildTargetInfo> result = new ArrayList<>();
        for (Label label : targetUtils.getTargetsForFile(file)) {
            BuildTargetInfo info = TargetStorage.getTargetInfo(targetUtils, label);
            if (info != null && info.kind().languageClasses().contains(PythonLanguageClass.PYTHON)) {
                result.add(info);
            }
        }
        return result;
    }

    @RequiredReadAction
    private static boolean isMainFileInTarget(PyFile receiver, BuildTargetInfo target) {
        if (target.kind().ruleType() != RuleType.BINARY && target.kind().ruleType() != RuleType.TEST) {
            return false;
        }
        TargetStorage targetStorage = TargetStorage.getTargetStorage(receiver.getProject());
        PythonBuildTarget pythonBuildTargetData = targetStorage.getTargetDataForLabel(target.id(), PythonBuildTarget.class);
        if (pythonBuildTargetData == null) {
            return false;
        }
        VirtualFile virtualFile = receiver.getVirtualFile();
        Path fileNioPath = toNioPathOrNull(virtualFile);
        OutputLocation mainFileLocation = pythonBuildTargetData.mainFile();
        Path mainFile = mainFileLocation != null ? targetStorage.resolveExecrootOutputLocation(mainFileLocation) : null;
        if (mainFile != null && Files.isRegularFile(mainFile)) {
            return mainFile.equals(fileNioPath);
        }
        else if (pythonBuildTargetData.mainModule() == null || pythonBuildTargetData.mainModule().isEmpty()) {
            // When both the main file and main module aren't defined, py_binary expects targetName + ".py".
            return virtualFile != null && virtualFile.getName().equals(target.id().getTargetName() + ".py");
        }
        else {
            // Resolving Python modules to files is intentionally not supported here.
            return false;
        }
    }

    @SuppressWarnings("unused")
    @RequiredReadAction
    private static @Nullable PyFunction getTestFunctionFromContext(PsiElement receiver) {
        PyFunction fromNameIdentifier = getTestFunctionFromNameIdentifier(receiver);
        if (fromNameIdentifier != null) {
            return fromNameIdentifier;
        }
        if (receiver instanceof PyFunction function && isTestFunction(function)) {
            return function;
        }
        PyFunction parent = PsiTreeUtil.getParentOfType(receiver, PyFunction.class, false);
        return parent != null && isTestFunction(parent) ? parent : null;
    }


    @RequiredReadAction
    private static @Nullable PyFunction getTestFunctionFromNameIdentifier(PsiElement receiver) {
        // consulo: Kotlin parentOfType<PsiNameIdentifierOwner>() (strict)
        PsiNameIdentifierOwner owner = PsiTreeUtil.getParentOfType(receiver, PsiNameIdentifierOwner.class);
        if (owner == null || !Objects.equals(owner.getNameIdentifier(), receiver)) {
            return null;
        }
        if (!(owner instanceof PyFunction function)) {
            return null;
        }
        return isTestFunction(function) ? function : null;
    }

    @RequiredReadAction
    private static boolean isTestFunction(PyFunction receiver) {
        // consulo: IntelliJ com.jetbrains.python.testing.isTestFunction(function) -> PyTestUtil.isPyTestFunction (the name starts
        // with "test"; the Consulo version does not exclude pytest fixtures)
        if (!PyTestUtil.isPyTestFunction(receiver)) {
            return false;
        }

        PyClass containingClass = receiver.getContainingClass();
        return containingClass != null ? isTestClass(containingClass) : PyUtil.isTopLevel(receiver);
    }

    @RequiredReadAction
    private static boolean isTestClass(PyClass receiver) {
        // consulo: IntelliJ com.jetbrains.python.testing.isTestClass(cls, context) -> PyTestUtil.isPyTestClass (a unittest.TestCase
        // subclass, or a class named "Test..." with a test method)
        return PyTestUtil.isPyTestClass(receiver, getTypeEvalContext(receiver));
    }

    @RequiredReadAction
    private static boolean isUnitTestClass(PyClass receiver) {
        // consulo: IntelliJ com.jetbrains.python.testing.isUnitTestCaseClass(cls, context) -> PythonUnitTestUtil.isUnitTestCaseClass(cls)
        return PythonUnitTestUtil.isUnitTestCaseClass(receiver) || hasUnitTestCaseSuperClassExpression(receiver);
    }

    @RequiredReadAction
    private static boolean hasUnitTestCaseSuperClassExpression(PyClass receiver) {
        for (PyExpression it : receiver.getSuperClassExpressions()) {
            if (it.getText().equals("unittest.TestCase") || it.getText().equals("TestCase")) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    private static TypeEvalContext getTypeEvalContext(PsiElement receiver) {
        return TypeEvalContext.codeAnalysis(receiver.getProject(), Objects.requireNonNull(receiver.getContainingFile()));
    }

    @RequiredReadAction
    private static @Nullable String getTestRunnerArgument(PyFunction receiver) {
        if (isUnitTestFunction(receiver)) {
            return getUnitTestArgument(receiver);
        }

        PsiFile containingFile = receiver.getContainingFile();
        String relativeFilePath = containingFile != null ? getPathRelativeToProjectRoot(containingFile) : null;
        if (relativeFilePath == null) {
            return null;
        }
        return getPyTestArgument(receiver, relativeFilePath);
    }

    private static @Nullable String getPathRelativeToProjectRoot(PsiFile receiver) {
        VirtualFile virtualFile = receiver.getVirtualFile();
        Path filePath = toNioPathOrNull(virtualFile);
        if (virtualFile == null || filePath == null) {
            return null;
        }
        Path projectRoot = toNioPathOrNull(org.jetbrains.bazel.config.project.getRootDir(receiver.getProject()));
        if (projectRoot == null) {
            return virtualFile.getName();
        }
        if (filePath.startsWith(projectRoot)) {
            // consulo: Kotlin Path.invariantSeparatorsPathString
            return projectRoot.relativize(filePath).toString().replace('\\', '/');
        }
        else {
            return virtualFile.getName();
        }
    }

    @RequiredReadAction
    private static boolean isUnitTestFunction(PyFunction receiver) {
        PyClass containingClass = receiver.getContainingClass();
        return containingClass != null && isUnitTestClass(containingClass);
    }

    @RequiredReadAction
    private static @Nullable String getUnitTestArgument(PyFunction receiver) {
        PyClass containingClass = receiver.getContainingClass();
        String className = containingClass != null ? containingClass.getName() : null;
        if (className == null) {
            return null;
        }
        String functionName = receiver.getName();
        if (functionName == null) {
            return null;
        }
        return className + "." + functionName;
    }

    @RequiredReadAction
    private static @Nullable String getPyTestArgument(PyFunction receiver, String relativeFilePath) {
        String functionName = receiver.getName();
        if (functionName == null) {
            return null;
        }
        PyClass containingClass = receiver.getContainingClass();
        String className = containingClass != null ? containingClass.getName() : null;
        if (className == null) {
            return relativeFilePath + "::" + functionName;
        }
        else {
            return relativeFilePath + "::" + className + "::" + functionName;
        }
    }

    // Checks if a PSI element is a top-level < if __name__ == "__main__" > Python statement.
    @RequiredReadAction
    private static boolean isIfNameMain(PsiElement receiver) {
        ASTNode node = receiver.getNode();
        if (node == null || node.getElementType() != PyTokenTypes.IF_KEYWORD) {
            return false;
        }
        PsiElement element = receiver;
        while (true) {
            PyIfStatement ifStatement = PsiTreeUtil.getParentOfType(element, PyIfStatement.class);
            if (ifStatement == null) {
                break;
            }
            element = ifStatement;
        }
        return element instanceof PyIfStatement ifStatement && PyUtil.isIfNameEqualsMain(ifStatement);
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
