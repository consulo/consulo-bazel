package org.jetbrains.bazel.action.registered;

import consulo.application.util.function.ThrowableComputable;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.config.BazelPluginConstants;

// TODO consulo: not registered while actionPerformed lacks CreateDirectoryOrPackageHandler (see below); upstream registration:
// @ActionImpl(
//     id = "Bazel.NewBazelPackageAction",
//     parents = @ActionParentRef(
//         value = @ActionRef(id = "NewGroup"),
//         anchor = ActionRefAnchor.AFTER,
//         relatedToAction = @ActionRef(id = "NewDir")
//     )
// )
public class NewBazelPackageAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public NewBazelPackageAction() {
        super(BazelPluginLocalize.actionBazelNewbazelpackageactionText());
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Presentation presentation = e.getPresentation();
        IdeView view = e.getData(IdeView.KEY);
        if (project == null || !org.jetbrains.bazel.config.project.isBazelProject(project) || view == null ||
            view.getDirectories().length == 0) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(true);
        String buildSystem = BazelPluginConstants.BAZEL_DISPLAY_NAME;
        presentation.setText(BazelPluginLocalize.actionNewBazelPackageText(buildSystem));
        presentation.setDescription(BazelPluginLocalize.actionNewBazelPackageDescription(buildSystem));
        presentation.setIcon(BazelPluginIcons.bazelDirectory());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }
        IdeView ideView = event.getData(IdeView.KEY);
        if (ideView == null) {
            return;
        }
        PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(ideView);
        if (directory == null) {
            return;
        }
        // TODO consulo: CreateDirectoryOrPackageHandler (validation + creation of the directories) exists only in
        //  consulo.ide.impl (consulo.ide.impl.idea.ide.actions.CreateDirectoryOrPackageHandler), there is no API for it
        // val validator: CreateDirectoryOrPackageHandler =
        //   object : CreateDirectoryOrPackageHandler(project, directory, false, ".") {
        //     override fun createDirectories(subDirName: String) {
        //       super.createDirectories(subDirName)
        //       val element = createdElement
        //       if (element is PsiDirectory) {
        //         createBuildFile(project, element, ideView)
        //       }
        //     }
        //   }
        //
        // // UI code based on CreateDirectoryOrPackageAction
        // val contentPanel = NewItemSimplePopupPanel()
        // val nameField = contentPanel.textField
        // nameField.document.addDocumentListener(
        //   object : DocumentAdapter() {
        //     override fun textChanged(event: DocumentEvent) {
        //       val name = nameField.text
        //       validator.checkInput(name)
        //       val errorText = validator.getErrorText(name) ?: validator.getWarningText(name)
        //       // invokeLater here to make sure that the text update in the UI does not interfere with showing the error popups
        //       SwingUtilities.invokeLater {
        //         contentPanel.setError(errorText)
        //       }
        //     }
        //   },
        // )
        //
        // val popup =
        //   NewItemPopupUtil.createNewItemPopup(
        //     bundledMessage("widget.new.bazel.package.popup.title"),
        //     contentPanel,
        //     nameField,
        //   )
        // contentPanel.applyAction =
        //   Consumer { event ->
        //     val name = nameField.getText()
        //     validator.checkInput(name)
        //     if (validator.getErrorText(name) == null && validator.getWarningText(name) == null && validator.canClose(name)) {
        //       popup.closeOk(event)
        //     }
        //   }
        // popup.showCenteredInCurrentWindow(project)
    }

    @SuppressWarnings("unused") // consulo: called from the commented out CreateDirectoryOrPackageHandler above
    @RequiredUIAccess
    private void createBuildFile(
        Project project,
        PsiDirectory parent,
        IdeView ideView
    ) {
        String filename = Constants.defaultBuildFileName();
        PsiFile buildFile =
            WriteCommandAction.writeCommandAction(project).compute(
                (ThrowableComputable<PsiFile, RuntimeException>) () -> parent.createFile(filename)
            );
        ideView.selectElement(buildFile);
        // consulo: upstream OpenFileAction.openFile(file, project) (ide-impl only) opens the file in an editor
        FileEditorManager.getInstance(project).openFile(buildFile.getViewProvider().getVirtualFile(), true);
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)
}
