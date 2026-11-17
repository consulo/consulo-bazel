package com.intellij.bazel.python.backend;

import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.impl.psi.types.PyModuleType;
import com.jetbrains.python.psi.PyFromImportStatement;
import com.jetbrains.python.psi.PyImportElement;
import com.jetbrains.python.psi.PyImportStatement;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.impl.PyPsiUtils;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.util.QualifiedName;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class BazelPythonImportCompletionContributor extends CompletionContributor implements DumbAware {
    public BazelPythonImportCompletionContributor() {
        extend(CompletionType.BASIC, psiElement().withLanguage(PythonLanguage.INSTANCE), BazelPythonImportCompletionProvider.INSTANCE);
    }

    // consulo: completion contributors are language-bound extensions (upstream XML language="Python")
    @Override
    public Language getLanguage() {
        return PythonLanguage.INSTANCE;
    }
}

// consulo: upstream private object
final class BazelPythonImportCompletionProvider implements CompletionProvider {
    static final BazelPythonImportCompletionProvider INSTANCE = new BazelPythonImportCompletionProvider();

    private BazelPythonImportCompletionProvider() {
    }

    @Override
    @RequiredReadAction
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        Project project = parameters.getPosition().getProject();
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return;
        }

        PyReferenceExpression importSource = findQualifiedImportSourceReference(parameters);
        if (importSource == null) {
            return;
        }
        QualifiedName importSourceQName = PyPsiUtils.asQualifiedName(importSource);
        if (importSourceQName == null) {
            return;
        }
        QualifiedName qualifier = importSourceQName.removeLastComponent();
        Map<String, Path> children = project.getInstance(PythonResolveIndexService.class).findDirectChildren(qualifier);
        if (children.isEmpty()) {
            return;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        Set<String> existingNames = new HashSet<>();
        for (Path path : children.values()) {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByNioPath(path);
            if (virtualFile == null) {
                continue;
            }
            PsiElement item;
            if (virtualFile.isDirectory()) {
                item = psiManager.findDirectory(virtualFile);
            }
            else {
                item = psiManager.findFile(virtualFile);
            }
            if (item instanceof PsiFileSystemItem fileSystemItem) {
                LookupElementBuilder lookupElement = PyModuleType.buildFileLookupElement(parameters.getOriginalFile(), fileSystemItem, existingNames);
                if (lookupElement != null) {
                    result.addElement(lookupElement);
                }
            }
        }
    }

    /**
     * Finds the module-path reference expression at the caret in {@code from <here> import ...} or plain {@code import <here>}.
     * The imported-names list of {@code from X import <here>} also uses {@link PyImportElement}, so it is explicitly excluded.
     */
    @RequiredReadAction
    private static @Nullable PyReferenceExpression findQualifiedImportSourceReference(CompletionParameters parameters) {
        // Use `position` (completion copy with the dummy identifier), not `originalPosition`: right after a `.`
        // (e.g. `from lib.<caret>`) `originalPosition` is the whitespace after the dot and has no PyReferenceExpression
        // ancestor. The dummy is stripped by the caller via `asQualifiedName(...).removeLastComponent()`.
        PsiElement position = parameters.getPosition();
        PyReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(position, PyReferenceExpression.class, false);
        if (referenceExpression == null) {
            return null;
        }

        PyFromImportStatement fromImportStatement = PsiTreeUtil.getParentOfType(referenceExpression, PyFromImportStatement.class);
        PyReferenceExpression importSource = fromImportStatement != null ? fromImportStatement.getImportSource() : null;
        if (importSource != null && PsiTreeUtil.isAncestor(importSource, referenceExpression, false)) {
            return importSource;
        }

        PyImportElement importElement = PsiTreeUtil.getParentOfType(referenceExpression, PyImportElement.class);
        if (importElement != null && importElement.getParent() instanceof PyImportStatement) {
            PyReferenceExpression importReference = importElement.getImportReferenceExpression();
            if (importReference != null && PsiTreeUtil.isAncestor(importReference, referenceExpression, false)) {
                return importReference;
            }
        }

        return null;
    }
}
