package org.jetbrains.bazel.ui.widgets.tool.window.actions;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

public abstract class CopyTargetIdAction extends AnAction {
    protected CopyTargetIdAction() {
        super(BazelPluginLocalize.widgetCopyTargetId(), LocalizeValue.empty(), PlatformIconGroup.actionsCopy());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        BuildTargetInfo targetInfo = getTargetInfo();
        if (targetInfo != null) {
            copyIdToClipboard(targetInfo, project);
        }
    }

    protected abstract @Nullable BuildTargetInfo getTargetInfo();

    @RequiredUIAccess
    private void copyIdToClipboard(BuildTargetInfo receiver, Project project) {
        CopyPasteManager clipboard = CopyPasteManager.getInstance();
        // consulo: DataTransfer instead of TextTransferable
        DataTransfer transferable = DataTransfer.of(BazelRepoMappingUtils.toShortString(receiver.id(), project));
        clipboard.setContents(transferable);
    }

    public abstract static class FromContainer extends CopyTargetIdAction {
        public FromContainer(JComponent component) {
            registerCustomShortcutSet(CommonShortcuts.getCopy(), component);
        }
    }

    public static class FromTargetInfo extends CopyTargetIdAction {
        private final BuildTargetInfo myTargetInfo;

        public FromTargetInfo(BuildTargetInfo targetInfo) {
            myTargetInfo = targetInfo;
        }

        @Override
        protected BuildTargetInfo getTargetInfo() {
            return myTargetInfo;
        }
    }
}
