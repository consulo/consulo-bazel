package org.jetbrains.bazel.run.config;

import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.CommonParameterFragments;
import consulo.bazel.execution.ui.RunConfigurationFragmentedEditor;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The base editor for a Bazel run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 * handler changes won't be affected in the UI until the editor is recreated
 */
public class BazelRunConfigurationEditor extends RunConfigurationFragmentedEditor<BazelRunConfiguration> {
    private final BazelRunConfiguration myRunConfiguration;
    private final @Nullable BazelRunHandler myHandler;

    public BazelRunConfigurationEditor(BazelRunConfiguration runConfiguration, @Nullable BazelRunHandler handler) {
        super(runConfiguration, handler == null ? null : handler.getExtensionsManager());
        myRunConfiguration = runConfiguration;
        myHandler = handler;
    }

    public BazelRunConfigurationEditor(BazelRunConfiguration runConfiguration) {
        this(runConfiguration, runConfiguration.getHandler());
    }

    public @Nullable BazelRunHandler getHandler() {
        return myHandler;
    }

    @Override
    protected void initFragments(Collection<? extends SettingsEditorFragment<BazelRunConfiguration, ?>> fragments) {
        super.initFragments(fragments);
        List<SettingsEditorFragment<BazelRunConfiguration, ?>> allFragments = new ArrayList<>(fragments);
        Application.get().getExtensionPoint(BazelRunConfigurationEditorCustomizer.class)
            .forEachExtensionSafe(it -> it.fragmentsCreated(myRunConfiguration, allFragments));
    }

    @Override
    protected void resetEditorFrom(BazelRunConfiguration s) {
        super.resetEditorFrom(s);
        List<SettingsEditorFragment<BazelRunConfiguration, ?>> allFragments = new ArrayList<>(getFragments());
        Application.get().getExtensionPoint(BazelRunConfigurationEditorCustomizer.class)
            .forEachExtensionSafe(it -> it.fragmentsReset(s, allFragments));
    }

    @Override
    protected List<SettingsEditorFragment<BazelRunConfiguration, ?>> createRunFragments() {
        return SettingsEditorFragmentContainer.<BazelRunConfiguration>fragments(container -> {
            // consulo: CommonTags.parallelRun() and the before run fragments are dropped, the Consulo run configuration dialog
            // already shows "allow parallel run" and the before run tasks
            // add(CommonTags.parallelRun())
            // addBeforeRunFragment()
            addTargetLabelsFragment(container);
            container.add(new RunHandlerFragment());
            addStateEditorFragment(container);
        });
    }

    // consulo: see createRunFragments(), BeforeRunComponent/BeforeRunFragment are not ported
    // private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addBeforeRunFragment() {
    //   val beforeRunComponent = BeforeRunComponent(this@BazelRunConfigurationEditor)
    //   add(BeforeRunFragment.createBeforeRun(beforeRunComponent, null))
    //   addAll(BeforeRunFragment.createGroup())
    // }

    private void addStateEditorFragment(SettingsEditorFragmentContainer<BazelRunConfiguration> receiver) {
        BazelRunHandler handler = myHandler;
        if (handler == null) {
            return;
        }
        receiver.add(CommonParameterFragments.<BazelRunConfiguration>createHeader(BazelPluginLocalize.runconfigHeader()));
        receiver.addAll(handler.getState().createFragments(myRunConfiguration));
    }

    private void addTargetLabelsFragment(SettingsEditorFragmentContainer<BazelRunConfiguration> receiver) {
        receiver.add(new TargetsFragment(getProject()));
    }

    /**
     * Updates the run handlers on offer for the targets of {@code configuration}. The other fragments keep their state.
     */
    public void resetRunHandlerFragment(BazelRunConfiguration configuration) {
        for (SettingsEditorFragment<BazelRunConfiguration, ?> fragment : getFragments()) {
            if (fragment instanceof RunHandlerFragment runHandlerFragment) {
                runHandlerFragment.resetFrom(configuration);
            }
        }
    }

    public @Nullable String findFocusedFragmentId() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return null;
        }
        for (SettingsEditorFragment<BazelRunConfiguration, ?> fragment : getFragments()) {
            if (SwingUtilities.isDescendingFrom(focusOwner, fragment.component())) {
                return fragment.getId();
            }
        }
        return null;
    }

    public void focusFragment(String fragmentId) {
        for (SettingsEditorFragment<BazelRunConfiguration, ?> fragment : getFragments()) {
            if (fragment.getId().equals(fragmentId)) {
                fragment.getEditorComponent().requestFocus();
                return;
            }
        }
    }
}
