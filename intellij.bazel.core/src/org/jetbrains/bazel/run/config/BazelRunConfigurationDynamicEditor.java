package org.jetbrains.bazel.run.config;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.bazel.run.BazelRunHandler;

import javax.swing.*;

/**
 * Delegates to {@link BazelRunConfigurationEditor} and recreates it whenever {@link BazelRunHandler} changes.
 * <p>
 * Code based on {@code com.intellij.execution.ui.RunnerAndConfigurationSettingsEditor} which also deals with {@link SettingsEditor} delegation.
 */
// consulo: RunnerAndConfigurationAwareSettingsEditor (and RunnerAndConfigurationSettingsImpl) are not available in Consulo, the
// editor only edits the run configuration
class BazelRunConfigurationDynamicEditor extends SettingsEditor<BazelRunConfiguration> {
    private final BazelRunConfiguration myRunConfiguration;
    private BazelRunConfigurationEditor myDelegate;
    // private var runnerAndConfigurationSettings: RunnerAndConfigurationSettingsImpl? = null

    BazelRunConfigurationDynamicEditor(BazelRunConfiguration runConfiguration) {
        myRunConfiguration = runConfiguration;
        myDelegate = new BazelRunConfigurationEditor(runConfiguration);
        registerDelegateListeners();
    }

    private void registerDelegateListeners() {
        Disposer.register(this, myDelegate);
        myDelegate.addSettingsEditorListener(editor -> fireEditorStateChanged());
    }

    @Override
    protected void resetEditorFrom(BazelRunConfiguration s) {
        myDelegate.resetEditorFrom(s);
    }

    @Override
    protected void applyEditorTo(BazelRunConfiguration s) throws ConfigurationException {
        // consulo: applyEditorTo() of the delegate is not accessible here, applyTo() calls it
        myDelegate.applyTo(s);
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        Wrapper wrapper = new Wrapper((JComponent) TargetAWT.to(myDelegate.getUIComponent()));
        BazelRunConfiguration.BazelRunHandlerChangedListener listener = new BazelRunConfiguration.BazelRunHandlerChangedListener() {
            @RequiredUIAccess
            @Override
            public void onRunHandlerChanged(BazelRunHandler newRunHandler) {
                if (myDelegate.getHandler() != newRunHandler) {
                    // runnerAndConfigurationSettings?.let { delegate.applyEditorTo(it) }
                    try {
                        myDelegate.applyTo(myRunConfiguration);
                    }
                    catch (ConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                    Disposer.dispose(myDelegate);

                    myDelegate = new BazelRunConfigurationEditor(myRunConfiguration);
                    registerDelegateListeners();
                    // runnerAndConfigurationSettings?.let { delegate.resetEditorFrom(it) }
                    myDelegate.resetFrom(myRunConfiguration);
                    wrapper.setContent((JComponent) TargetAWT.to(myDelegate.getUIComponent()));
                    // Run handler can only change if we're typing new targets.
                    // Make sure the focus is still on the edit field after the run config is recreated.
                    myDelegate.focusTargetLabelsFragment();
                }
            }
        };
        myRunConfiguration.addOnRunHandlerChangedListener(listener, this);
        return TargetAWT.wrap(wrapper);
    }

    // consulo: the delegate is disposed together with this editor (Disposer.register in registerDelegateListeners())
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
