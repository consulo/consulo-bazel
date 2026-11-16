package org.jetbrains.bazel.run.config;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Delegates to {@link BazelRunConfigurationEditor} and recreates it when the {@link BazelRunHandler} changes.
 * A change of the targets alone only updates the run handlers on offer.
 * <p>
 * Code based on {@code com.intellij.execution.ui.RunnerAndConfigurationSettingsEditor} which also deals with {@link SettingsEditor} delegation.
 */
// consulo: RunnerAndConfigurationAwareSettingsEditor (and RunnerAndConfigurationSettingsImpl) are not available in Consulo, the
// editor only edits the run configuration
class BazelRunConfigurationDynamicEditor extends SettingsEditor<BazelRunConfiguration> {
    private BazelRunConfigurationEditor myDelegate;
    private @Nullable RunHandlerProvider myDelegateHandlerProvider;
    private List<Label> myDelegateTargets = List.of();
    // private var runnerAndConfigurationSettings: RunnerAndConfigurationSettingsImpl? = null
    private final Wrapper myWrapper = new Wrapper();

    BazelRunConfigurationDynamicEditor(BazelRunConfiguration runConfiguration) {
        myDelegate = createDelegate(runConfiguration);
    }

    private BazelRunConfigurationEditor createDelegate(BazelRunConfiguration configuration) {
        BazelRunConfigurationEditor newDelegate = new BazelRunConfigurationEditor(configuration);
        myDelegateHandlerProvider = configuration.getHandlerProvider();
        myDelegateTargets = List.copyOf(configuration.getTargets());
        Disposer.register(this, newDelegate);
        newDelegate.addSettingsEditorListener(editor -> fireEditorStateChanged());
        return newDelegate;
    }

    @RequiredUIAccess
    private void recreateDelegate(BazelRunConfiguration configuration) {
        Disposer.dispose(myDelegate);
        myDelegate = createDelegate(configuration);
        // settings?.let { delegate.resetEditorFrom(it) }
        myDelegate.resetFrom(configuration);
        myWrapper.setContent((JComponent) TargetAWT.to(myDelegate.getUIComponent()));
    }

    @Override
    protected void resetEditorFrom(BazelRunConfiguration s) {
        if (s.getHandlerProvider() == myDelegateHandlerProvider) {
            myDelegateTargets = List.copyOf(s.getTargets());
            myDelegate.resetEditorFrom(s);
        }
        else {
            recreateDelegate(s);
        }
    }

    @Override
    protected void applyEditorTo(BazelRunConfiguration s) throws ConfigurationException {
        // consulo: applyEditorTo() of the delegate is not accessible here, applyTo() calls it
        myDelegate.applyTo(s);
        if (s.getHandlerProvider() != myDelegateHandlerProvider) {
            String focusedFragmentId = myDelegate.findFocusedFragmentId();
            // val settingsCopy = runnerAndConfigurationSettings?.clone()
            // settingsCopy?.let { delegate.applyEditorTo(it) }
            recreateDelegate((BazelRunConfiguration) s.clone());
            if (focusedFragmentId != null) {
                myDelegate.focusFragment(focusedFragmentId);
            }
        }
        else if (!s.getTargets().equals(myDelegateTargets)) {
            // The user is in the middle of an edit of the targets, so the targets field must stay.
            myDelegateTargets = List.copyOf(s.getTargets());
            myDelegate.resetRunHandlerFragment(s);
        }
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        myWrapper.setContent((JComponent) TargetAWT.to(myDelegate.getUIComponent()));
        return TargetAWT.wrap(myWrapper);
    }

    // consulo: the delegate is disposed together with this editor (Disposer.register in createDelegate())
    // override fun disposeEditor() {
    //   delegate.disposeEditor()
    // }

    @Override
    public BazelRunConfiguration getSnapshot() throws ConfigurationException {
        return myDelegate.getSnapshot();
    }

    // consulo: Consulo's SettingsEditor has no isSpecificallyModified(), isReadyForApply(), isInplaceValidationSupported() and
    // the RunnerAndConfigurationAwareSettingsEditor methods (resetEditorFrom/applyEditorTo of RunnerAndConfigurationSettingsImpl,
    // targetChanged)
    // override fun isSpecificallyModified(): Boolean = delegate.isSpecificallyModified
    // override fun isReadyForApply(): Boolean = delegate.isReadyForApply
    // override fun resetEditorFrom(s: RunnerAndConfigurationSettingsImpl) {
    //   runnerAndConfigurationSettings = s
    //   delegate.resetEditorFrom(s)
    // }
    // override fun applyEditorTo(s: RunnerAndConfigurationSettingsImpl) = delegate.applyEditorTo(s)
    // override fun targetChanged(targetName: String?) = delegate.targetChanged(targetName)
    // override fun isInplaceValidationSupported(): Boolean = delegate.isInplaceValidationSupported()
}
