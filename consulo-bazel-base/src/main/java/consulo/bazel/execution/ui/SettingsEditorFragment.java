/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.execution.ui;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ComponentValidator;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consulo replacement of IntelliJ {@code com.intellij.execution.ui.SettingsEditorFragment}: a single settings fragment of a
 * {@link RunConfigurationFragmentedEditor}, a Swing component with reset/apply callbacks.
 * <p>
 * Consulo has no fragmented settings editor ("Modify options"), so the bridge shows every available fragment;
 * {@link #isInitiallyVisible}, {@link #isRemovable()} and the action hint are kept for upstream code but do not change the UI.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SettingsEditorFragment<Settings, C extends JComponent> extends SettingsEditor<Settings> {
    private final String myId;
    private final LocalizeValue myName;
    private final LocalizeValue myGroup;
    protected C myComponent;
    private final BiConsumer<? super Settings, ? super C> myOnResetEditorFromSettings;
    private final BiConsumer<? super Settings, ? super C> myOnApplyEditorToSettings;
    private final List<Function<? super Settings, List<ValidationInfo>>> myValidation = new ArrayList<>();
    private final SettingsEditorFragmentType myType;
    private final int myPriority;
    private final Predicate<? super Settings> myInitialSelection;
    private LocalizeValue myHint = LocalizeValue.empty();
    private LocalizeValue myActionHint = LocalizeValue.empty();
    private @Nullable Function<? super C, ? extends JComponent> myEditorGetter;
    private boolean myRemovable = true;

    public SettingsEditorFragment(
        String id,
        LocalizeValue name,
        @Nullable LocalizeValue group,
        C component,
        int priority,
        SettingsEditorFragmentType type,
        BiConsumer<? super Settings, ? super C> resetEditorFromSettings,
        BiConsumer<? super Settings, ? super C> applyEditorToSettings,
        Predicate<? super Settings> initialSelection
    ) {
        myId = id;
        myName = name;
        myGroup = group == null ? LocalizeValue.empty() : group;
        myComponent = component;
        myOnResetEditorFromSettings = resetEditorFromSettings;
        myOnApplyEditorToSettings = applyEditorToSettings;
        myPriority = priority;
        myType = type;
        myInitialSelection = initialSelection;
    }

    public SettingsEditorFragment(
        String id,
        LocalizeValue name,
        @Nullable LocalizeValue group,
        C component,
        SettingsEditorFragmentType type,
        BiConsumer<? super Settings, ? super C> resetEditorFromSettings,
        BiConsumer<? super Settings, ? super C> applyEditorToSettings,
        Predicate<? super Settings> initialSelection
    ) {
        this(id, name, group, component, 0, type, resetEditorFromSettings, applyEditorToSettings, initialSelection);
    }

    public SettingsEditorFragment(
        String id,
        LocalizeValue name,
        @Nullable LocalizeValue group,
        C component,
        int commandLinePosition,
        BiConsumer<? super Settings, ? super C> resetEditorFromSettings,
        BiConsumer<? super Settings, ? super C> applyEditorToSettings,
        Predicate<? super Settings> initialSelection
    ) {
        this(
            id,
            name,
            group,
            component,
            commandLinePosition,
            getType(commandLinePosition),
            resetEditorFromSettings,
            applyEditorToSettings,
            initialSelection
        );
    }

    public SettingsEditorFragment(
        String id,
        LocalizeValue name,
        @Nullable LocalizeValue group,
        C component,
        BiConsumer<? super Settings, ? super C> resetEditorFromSettings,
        BiConsumer<? super Settings, ? super C> applyEditorToSettings,
        Predicate<? super Settings> initialSelection
    ) {
        this(
            id,
            name,
            group,
            component,
            0,
            SettingsEditorFragmentType.EDITOR,
            resetEditorFromSettings,
            applyEditorToSettings,
            initialSelection
        );
    }

    private static SettingsEditorFragmentType getType(int commandLinePosition) {
        // consulo: upstream also maps a TagButton component to TAG, TagButton is not ported
        return commandLinePosition == -2 ? SettingsEditorFragmentType.BEFORE_RUN :
            commandLinePosition == -1 ? SettingsEditorFragmentType.HEADER :
                commandLinePosition == 0 ? SettingsEditorFragmentType.EDITOR :
                    SettingsEditorFragmentType.COMMAND_LINE;
    }

    public String getId() {
        return myId;
    }

    public LocalizeValue getName() {
        return myName;
    }

    /**
     * Empty when the fragment has no group.
     */
    public LocalizeValue getGroup() {
        return myGroup;
    }

    public C component() {
        return myComponent;
    }

    public SettingsEditorFragmentType getType() {
        return myType;
    }

    public boolean isTag() {
        return SettingsEditorFragmentType.TAG == myType;
    }

    public boolean isCommandLine() {
        return SettingsEditorFragmentType.COMMAND_LINE == myType;
    }

    public boolean isHeader() {
        return SettingsEditorFragmentType.HEADER == myType;
    }

    public boolean isBeforeRun() {
        return SettingsEditorFragmentType.BEFORE_RUN == myType;
    }

    public boolean isEditor() {
        return SettingsEditorFragmentType.EDITOR == myType;
    }

    public int getPriority() {
        return myPriority;
    }

    public boolean isInitiallyVisible(Settings settings) {
        return myInitialSelection.test(settings);
    }

    public boolean isRemovable() {
        return myRemovable;
    }

    public void setRemovable(boolean removable) {
        myRemovable = removable;
    }

    public void setValidation(@Nullable Function<? super Settings, List<ValidationInfo>> validation) {
        myValidation.clear();
        if (validation != null) {
            myValidation.add(validation);
        }
    }

    /**
     * Runs the validations of this fragment and shows the result on the validated components (like upstream, which does it on a
     * pooled thread). Returns all validation infos with a message, so that the editor can reject errors.
     */
    @RequiredUIAccess
    protected List<ValidationInfo> validate(Settings s) {
        if (myValidation.isEmpty()) {
            return List.of();
        }
        List<ValidationInfo> infos = new ArrayList<>();
        for (Function<? super Settings, List<ValidationInfo>> validation : myValidation) {
            infos.addAll(validation.apply(s));
        }
        List<ValidationInfo> result = new ArrayList<>();
        for (ValidationInfo info : infos) {
            JComponent component = info.component;
            if (!info.message.isEmpty()) {
                result.add(info);
            }
            if (component == null) {
                continue;
            }
            Optional<ComponentValidator> optional = ComponentValidator.getInstance(component);
            ComponentValidator validator;
            if (optional.isEmpty()) {
                validator = new ComponentValidator(this);
                validator.installOn(component);
            }
            else {
                validator = optional.get();
            }
            validator.updateInfo(info.message.isEmpty() ? null : info);
        }
        return result;
    }

    /**
     * Allows to control the visibility of the fragment for the fragmented editor.
     * E.g., if a fragment should be displayed only when some setting or some other fragment field has some value,
     * overriding this method allows to hide it from the editor.
     *
     * @return whether the given fragment should be available in the fragmented editor
     */
    public boolean isAvailable() {
        return true;
    }

    public void setEditorGetter(@Nullable Function<? super C, ? extends JComponent> editorGetter) {
        myEditorGetter = editorGetter;
    }

    public JComponent getEditorComponent() {
        C component = component();
        Function<? super C, ? extends JComponent> editorGetter = myEditorGetter;
        if (editorGetter != null) {
            return editorGetter.apply(component);
        }
        return getEditorComponent(component);
    }

    private static JComponent getEditorComponent(JComponent component) {
        JComponent result = component;
        if (result instanceof LabeledComponent<?> labeledComponent) {
            result = labeledComponent.getComponent();
        }

        if (result instanceof ComponentWithBrowseButton<?> componentWithBrowseButton) {
            return componentWithBrowseButton.getChildComponent();
        }
        if (result instanceof RawCommandLineEditor rawCommandLineEditor) {
            // consulo: RawCommandLineEditor has no getEditorField(), the text field is the editor
            return rawCommandLineEditor.getTextField();
        }
        return result;
    }

    @Override
    protected void resetEditorFrom(Settings s) {
        myOnResetEditorFromSettings.accept(s, myComponent);
    }

    @Override
    protected void applyEditorTo(Settings s) throws ConfigurationException {
        myOnApplyEditorToSettings.accept(s, myComponent);
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        myComponent.setVisible(isAvailable());
        return TargetAWT.wrap(myComponent);
    }

    public List<SettingsEditorFragment<Settings, ?>> getChildren() {
        return Collections.emptyList();
    }

    public LocalizeValue getActionHint() {
        return myActionHint;
    }

    public void setActionHint(LocalizeValue hint) {
        myActionHint = hint;
    }

    /**
     * Empty when the fragment has no hint.
     */
    public LocalizeValue getHint() {
        return myHint;
    }

    public void setHint(LocalizeValue hint) {
        myHint = hint;
    }
}
