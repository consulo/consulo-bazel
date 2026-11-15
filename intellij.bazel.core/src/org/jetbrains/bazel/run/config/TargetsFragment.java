package org.jetbrains.bazel.run.config;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentType;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.ui.awt.TextFieldWithCompletion;
import consulo.language.editor.ui.textCompletion.DefaultTextCompletionValueDescriptor;
import consulo.language.editor.ui.textCompletion.TextCompletionProvider;
import consulo.language.editor.ui.textCompletion.ValuesCompletionProvider;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TargetsFragment
    extends SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<TextFieldWithCompletion>> {
    private final TextFieldWithCompletion myTargetsField;

    public TargetsFragment(TextFieldWithCompletion targetsField) {
        super(
            "bsp.target.fragment",
            BazelPluginLocalize.runconfigTargetsName(),
            BazelPluginLocalize.runconfigTargetsGroup(),
            new SettingsEditorLabeledComponent<>(BazelPluginLocalize.runconfigTargetsName(), targetsField),
            /* priority = */ 0,
            SettingsEditorFragmentType.EDITOR,
            (configuration, component) -> component.getComponent()
                .setText(configuration.getTargets().stream().map(Label::toString).collect(Collectors.joining(" "))),
            (configuration, component) -> configuration.updateTargets(parseTargets(component.getComponent().getText())),
            configuration -> true
        );
        myTargetsField = targetsField;
        setHint(BazelPluginLocalize.runconfigTargetsHint());
        setActionHint(BazelPluginLocalize.runconfigTargetsActionHint());
    }

    public TargetsFragment(Project project) {
        this(createTargetsField(project));
    }

    public List<Label> getTargets() {
        List<Label> result = new ArrayList<>();
        for (String it : myTargetsField.getText().split(" ", -1)) {
            String trimmed = it.trim();
            Label label = Label.parseOrNull(trimmed.isEmpty() ? null : trimmed);
            if (label != null) {
                result.add(label);
            }
        }
        return result;
    }

    public void addTargetsListener(Runnable listener) {
        myTargetsField.addDocumentListener(
            new DocumentListener() {
                @Override
                public void documentChanged(DocumentEvent event) {
                    listener.run();
                }
            }
        );
    }

    private static TextFieldWithCompletion createTargetsField(Project project) {
        // consulo: TextFieldWithAutoCompletion.StringsCompletionProvider is not a TextCompletionProvider in Consulo,
        // ValuesCompletionProvider with a string descriptor (space separated values) is used instead
        TextCompletionProvider provider = new ValuesCompletionProvider<>(
            new DefaultTextCompletionValueDescriptor.StringValueDescriptor() {
                @Override
                protected @Nullable Image getIcon(String item) {
                    return /* icon = */ BazelPluginIcons.bazel();
                }
            },
            List.of(' '),
            /* variants = */
            TargetStorage.getTargetStorage(project)
                .getAllExecutableTargetLabels(),
            false
        );
        return new TextFieldWithCompletion(
            /* project = */ project,
            /* provider = */ provider,
            /* value = */ "",
            /* oneLineMode = */ true,
            /* autoPopup = */ true,
            /* forceAutoPopup = */ false,
            /* showHint = */ true
        );
    }

    private static List<Label> parseTargets(String text) {
        if (!text.isBlank()) {
            return Arrays.stream(text.trim().split(" ", -1)).map(Label::parse).toList();
        }
        else {
            return List.of();
        }
    }
}
