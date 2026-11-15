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

import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consulo replacement of the IntelliJ extension function
 * {@code com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment}
 * ({@code ExternalSystemRunConfigurationFragmentBuilders.kt}), built on Consulo's
 * {@link EnvironmentVariablesTextFieldWithBrowseButton}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class EnvironmentFragments {
    private EnvironmentFragments() {
    }

    @RequiredUIAccess
    public static <S> SettingsEditorFragment<S, SettingsEditorLabeledComponent<JComponent>> addEnvironmentFragment(
        SettingsEditorFragmentContainer<S> receiver,
        LabeledSettingsFragmentInfo settingsFragmentInfo,
        Function<S, Map<String, String>> getEnvs,
        BiConsumer<S, Map<String, String>> setEnvs,
        Predicate<S> isPassParentEnvs,
        BiConsumer<S, Boolean> setPassParentEnvs,
        boolean hideWhenEmpty
    ) {
        // consulo: Consulo's EnvironmentVariablesTextFieldWithBrowseButton is not a Swing component, the fragment component is
        // its Swing peer and reset/apply go through the field itself
        EnvironmentVariablesTextFieldWithBrowseButton field = new EnvironmentVariablesTextFieldWithBrowseButton();
        SettingsEditorFragment<S, SettingsEditorLabeledComponent<JComponent>> fragment = receiver.addLabeledSettingsEditorFragment(
            settingsFragmentInfo,
            it -> (JComponent) TargetAWT.to(field.getComponent()),
            (it, c) -> {
                field.setEnvs(getEnvs.apply(it));
                field.setPassParentEnvs(isPassParentEnvs.test(it));
            },
            (it, c) -> {
                setEnvs.accept(it, field.getEnvs());
                setPassParentEnvs.accept(it, field.isPassParentEnvs());
            },
            it -> !hideWhenEmpty || !getEnvs.apply(it).isEmpty() || !isPassParentEnvs.test(it)
        );
        fragment.setRemovable(hideWhenEmpty);
        return fragment;
    }
}
