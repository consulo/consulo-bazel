package org.jetbrains.bazel.languages.projectview.annotation.suppress;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.AbstractEmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionWithOptions;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

// consulo: upstream top-level declarations of ProjectViewSuppressActions.kt; the public ones are nested classes of this file class.
// The ModCommand API does not exist in Consulo: the suppress actions are classic SuppressIntentionAction-s
public final class ProjectViewSuppressActions {
    private ProjectViewSuppressActions() {
    }

    public static List<SuppressIntentionAction> suppressActions(ProjectViewProblem.Id receiver) {
        return Arrays.stream(ProjectViewSuppressScope.values())
            .<SuppressIntentionAction>map(it -> new ProjectViewSuppressAction(receiver, it))
            .toList();
    }

    public interface ProjectViewActionWithSuppressOptions extends IntentionActionWithOptions {

        List<? extends IntentionAction> getSuppressActions();

        @Override
        default List<IntentionAction> getOptions() {
            return List.copyOf(getSuppressActions());
        }

        // consulo: upstream getCombiningPolicy() = CombiningPolicy.IntentionOptionsOnly, Consulo has no combining policy.
        // Upstream also has the companion HighlightDisplayKey KEY ("BazelProjectView", the title of the submenu of the suppress
        // actions); a Consulo annotation fix needs the default annotator key to get its options (see ProjectViewProblemsAnnotator)
    }

    /**
     * Groups the suppress actions of a warning that has no quick fixes. The actions are in the submenu of this entry.
     * Like for the Kotlin compiler warnings, the suppress actions are never at the top level of the intention list.
     * A warning with quick fixes does not need this entry, because the submenu of each fix holds the suppress actions.
     */
    public static class ProjectViewSuppressOptionsAction extends AbstractEmptyIntentionAction
        implements ProjectViewActionWithSuppressOptions, LowPriorityAction {

        private final ProjectViewProblem.Id myProblemId;
        private final List<SuppressIntentionAction> mySuppressActions;

        public ProjectViewSuppressOptionsAction(
            ProjectViewProblem.Id problemId,
            List<SuppressIntentionAction> suppressActions
        ) {
            myProblemId = problemId;
            mySuppressActions = suppressActions;
        }

        @Override
        public List<SuppressIntentionAction> getSuppressActions() {
            return mySuppressActions;
        }

        @Override
        public LocalizeValue getText() {
            return BazelProjectViewLocalize.annotatorWarningOptions(myProblemId.value());
        }

        // consulo: Consulo intention actions have no family name; kept from upstream
        public LocalizeValue getFamilyName() {
            return getText();
        }

        @Override
        public boolean isAvailable(Project project, Editor editor, PsiFile psiFile) {
            return true;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof ProjectViewSuppressOptionsAction that && that.myProblemId.equals(myProblemId);
        }

        @Override
        public int hashCode() {
            return myProblemId.hashCode();
        }
    }
}

// consulo: upstream private class ProjectViewSuppressAction : ModCommandAction
class ProjectViewSuppressAction extends SuppressIntentionAction {
    private final ProjectViewProblem.Id myId;
    private final ProjectViewSuppressScope myScope;

    ProjectViewSuppressAction(
        ProjectViewProblem.Id id,
        ProjectViewSuppressScope scope
    ) {
        myId = id;
        myScope = scope;
    }

    // consulo: Consulo intention actions have no family name; kept from upstream
    public LocalizeValue getFamilyName() {
        return BazelProjectViewLocalize.suppressFamilyName();
    }

    // consulo: upstream getPresentation(context) is null when there is no container; isAvailable checks it here
    @Override
    public LocalizeValue getText() {
        return LocalizeKey.of(BazelProjectViewLocalize.ID, myScope.getActionKey()).getValue(myId.value());
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement element) {
        return container(editor, element) != null;
    }

    // consulo: upstream perform(context) returned the command; the write action is started by SuppressIntentionAction
    @Override
    public void invoke(Project project, Editor editor, PsiElement element) {
        PsiElement container = container(editor, element);
        if (container == null) {
            return;
        }
        Runnable command = ProjectViewSuppressionCommand.addProjectViewSuppressionCommand(container, myId);
        if (command != null) {
            command.run();
        }
    }

    // consulo: ActionContext.findLeaf() is the element at the caret, findLeafOnTheLeft() the element before the caret
    private @Nullable PsiElement container(Editor editor, PsiElement leaf) {
        PsiElement container = myScope.containerOf(leaf);
        if (container != null) {
            return container;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiFile leafFile = leaf.getContainingFile();
        PsiElement leafOnTheLeft = offset > 0 && leafFile != null ? leafFile.findElementAt(offset - 1) : null;
        return leafOnTheLeft != null ? myScope.containerOf(leafOnTheLeft) : null;
    }
}
