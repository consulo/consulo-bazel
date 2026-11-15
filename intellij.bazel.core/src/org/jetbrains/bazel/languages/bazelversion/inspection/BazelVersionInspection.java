package org.jetbrains.bazel.languages.bazelversion.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.SemVer;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage.BazelVersionFileType;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionFile;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionImpl
public class BazelVersionInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof BazelVersionFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new BazelVersionVisitor(holder);
    }

    // consulo: upstream XML registration <localInspection language="BazelVersion" key="inspection.display.name.bazel.version.check"
    // groupPath="Bazel" groupKey="bazel.inspection.group.name" enabledByDefault="true" level="WARNING">
    @Override
    public @Nullable Language getLanguage() {
        return BazelVersionLanguage.INSTANCE;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameBazelVersionCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.bazelInspectionGroupName();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    public static class BazelVersionVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        public BazelVersionVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        public ProblemsHolder getHolder() {
            return myHolder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {
            if (!(psiFile instanceof BazelVersionFile file)) {
                return;
            }
            BazelVersionLiteral bazelVersion = file.getBazelVersion();
            SemVer currentVersion = bazelVersion != null ? BazelVersionLiteral.toSemVer(bazelVersion) : null;
            if (currentVersion == null) {
                return;
            }
            BazelVersionLiteral latestBazelVersion =
                myHolder.getProject()
                    .getInstance(BazelVersionCheckerService.class)
                    .getLatestBazelVersion();
            SemVer latestVersion = latestBazelVersion != null ? BazelVersionLiteral.toSemVer(latestBazelVersion) : null;
            if (latestVersion == null) {
                return;
            }
            // consulo: IntelliJ SemVer.isGreaterThan(other) is compareTo(other) > 0
            if (latestVersion.compareTo(currentVersion) > 0) {
                myHolder.newProblem(BazelPluginLocalize.inspectionDescriptionBazelVersion(latestVersion))
                    .range(psiFile)
                    .withFixes(new BazelVersionQuickFix())
                    .create();
            }
        }
    }

    public static class BazelVersionQuickFix implements LocalQuickFix {
        // consulo: upstream getFamilyName(); Consulo quick fixes only have getName()
        @Override
        public LocalizeValue getName() {
            return BazelPluginLocalize.quickfixBazelVersion();
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor descriptor) {
            PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
            Document doc = manager.getDocument((PsiFile) Objects.requireNonNull(descriptor.getPsiElement()));
            if (doc == null) {
                return;
            }
            BazelVersionLiteral version =
                project
                    .getInstance(BazelVersionCheckerService.class)
                    .getLatestBazelVersion();
            if (version == null) {
                return;
            }
            WriteCommandAction.runWriteCommandAction(project, () -> {
                doc.setText(BazelVersionLiteral.toBazelVersionStringLiteral(version));
                manager.commitDocument(doc);
            });
        }
    }
}
