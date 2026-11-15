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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consulo replacement of IntelliJ
 * {@code com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer}.
 * <p>
 * The Kotlin extension functions {@code addLabeledSettingsEditorFragment} / {@code addSettingsEditorFragment} of upstream
 * {@code SettingsEditorFragmentBuilders.kt} are instance methods here, so call sites keep their shape.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SettingsEditorFragmentContainer<S> {
    private final List<SettingsEditorFragment<S, ?>> myFragments = new ArrayList<>();

    public <F extends SettingsEditorFragment<S, ?>> F add(F fragment) {
        myFragments.add(fragment);
        return fragment;
    }

    public <F extends SettingsEditorFragment<S, ?>> Collection<F> addAll(Collection<F> fragments) {
        myFragments.addAll(fragments);
        return fragments;
    }

    public List<SettingsEditorFragment<S, ?>> toList() {
        return myFragments;
    }

    public static <S> List<SettingsEditorFragment<S, ?>> fragments(Consumer<SettingsEditorFragmentContainer<S>> configure) {
        SettingsEditorFragmentContainer<S> container = new SettingsEditorFragmentContainer<>();
        configure.accept(container);
        return container.toList();
    }

    public <C extends JComponent> SettingsEditorFragment<S, SettingsEditorLabeledComponent<C>> addLabeledSettingsEditorFragment(
        LabeledSettingsFragmentInfo settingsFragmentInfo,
        Function<Disposable, C> createComponent,
        BiConsumer<S, C> reset,
        BiConsumer<S, C> apply
    ) {
        SettingsEditorFragment<S, SettingsEditorLabeledComponent<C>> fragment =
            addLabeledSettingsEditorFragment(settingsFragmentInfo, createComponent, reset, apply, it -> true);
        fragment.setRemovable(false);
        return fragment;
    }

    public <C extends JComponent> SettingsEditorFragment<S, SettingsEditorLabeledComponent<C>> addLabeledSettingsEditorFragment(
        LabeledSettingsFragmentInfo settingsFragmentInfo,
        Function<Disposable, C> createComponent,
        BiConsumer<S, C> reset,
        BiConsumer<S, C> apply,
        Predicate<S> initialSelection
    ) {
        return addSettingsEditorFragment(
            settingsFragmentInfo,
            it -> new SettingsEditorLabeledComponent<>(settingsFragmentInfo.getEditorLabel(), createComponent.apply(it)),
            (it, c) -> reset.accept(it, c.getComponent()),
            (it, c) -> apply.accept(it, c.getComponent()),
            initialSelection
        );
    }

    public <C extends JComponent> SettingsEditorFragment<S, C> addSettingsEditorFragment(
        LabeledSettingsFragmentInfo settingsFragmentInfo,
        Function<Disposable, C> createComponent,
        BiConsumer<S, C> reset,
        BiConsumer<S, C> apply,
        Predicate<S> initialSelection
    ) {
        Disposable settingsEditorDisposable = Disposable.newDisposable(settingsFragmentInfo.getSettingsId());
        C settingsEditorComponent = createComponent.apply(settingsEditorDisposable);
        SettingsEditorFragment<S, C> settingsEditorFragment = new SettingsEditorFragment<>(
            settingsFragmentInfo.getSettingsId(),
            settingsFragmentInfo.getSettingsName(),
            settingsFragmentInfo.getSettingsGroup(),
            settingsEditorComponent,
            settingsFragmentInfo.getSettingsPriority(),
            settingsFragmentInfo.getSettingsType(),
            reset,
            apply,
            initialSelection
        );

        Disposer.register(settingsEditorFragment, settingsEditorDisposable);

        settingsEditorFragment.setHint(settingsFragmentInfo.getSettingsHint());
        settingsEditorFragment.setActionHint(settingsFragmentInfo.getSettingsActionHint());

        JComponent editorComponent = settingsEditorFragment.getEditorComponent();
        if (settingsFragmentInfo.getSettingsType() == SettingsEditorFragmentType.COMMAND_LINE) {
            if (editorComponent instanceof JTextComponent || editorComponent instanceof JComboBox<?>) {
                CommonParameterFragments.setMonospaced(editorComponent);
            }
        }
        // TODO consulo: TextComponentEmptyText.setupPlaceholderVisibility(JTextComponent) has no Consulo counterpart
        // if (editorComponent instanceof JBTextField) {
        //     TextComponentEmptyText.setupPlaceholderVisibility(editorComponent);
        // }

        return add(settingsEditorFragment);
    }
}
