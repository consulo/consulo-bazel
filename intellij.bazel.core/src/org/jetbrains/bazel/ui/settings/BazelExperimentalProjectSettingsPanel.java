package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.configurable.ConfigurationException;
import consulo.configurable.OptionalConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.inject.Inject;
import org.jetbrains.bazel.settings.bazel.BazelProjectSettings;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// consulo: upstream BoundCompositeSearchableConfigurable (Kotlin UI DSL) -> ProjectConfigurable with a FormBuilder panel,
// the composite part (child configurables of the settings providers) is implemented by hand
@ExtensionImpl
class BazelExperimentalProjectSettingsConfigurable implements ProjectConfigurable, SearchableConfigurable, OptionalConfigurable {
    public static final String ID = "bazel.experimental.project.settings";
    public static final String DISPLAY_NAME_KEY = "project.settings.experimental.settings";

    private final Project myProject;

    private BazelProjectSettings myCurrentProjectSettings;

    private @Nullable List<UnnamedConfigurable> myConfigurables = null;

    @Inject
    public BazelExperimentalProjectSettingsConfigurable(Project project) {
        myProject = project;
        myCurrentProjectSettings = BazelProjectSettings.getBazelProjectSettings(project);
    }

    private JComponent createPanel(Disposable parentDisposable) {
        FormBuilder builder = FormBuilder.createFormBuilder();
        // add settings from extensions
        List<UnnamedConfigurable> sortedConfigurables = new ArrayList<>(getConfigurables());
        sortedConfigurables.sort(
            Comparator.comparing(
                (UnnamedConfigurable c) -> c instanceof Configurable configurable ? configurable.getDisplayName().get() : ""
            )
        );
        for (UnnamedConfigurable it : sortedConfigurables) {
            Component component = it.createUIComponent(parentDisposable);
            if (component != null) {
                builder.addComponent(TargetAWT.to(component));
            }
        }
        return builder.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        return TargetAWT.wrap(createPanel(parentDisposable));
    }

    // consulo: no Configurable.WithEpDependencies
    //override fun getDependencies(): List<BaseExtensionPointName<*>> = listOf(BazelExperimentalSettingsProvider.ep)

    private List<UnnamedConfigurable> createConfigurables() {
        List<UnnamedConfigurable> result = new ArrayList<>();
        for (BazelSettingsProvider it : BazelSettingsProvider.getBazelExperimentalSettingsProviders(myProject)) {
            result.add(it.createConfigurable(myProject));
        }
        return result;
    }

    // consulo: upstream BoundCompositeSearchableConfigurable.configurables
    private List<UnnamedConfigurable> getConfigurables() {
        List<UnnamedConfigurable> configurables = myConfigurables;
        if (configurables == null) {
            configurables = createConfigurables();
            myConfigurables = configurables;
        }
        return configurables;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        for (UnnamedConfigurable it : getConfigurables()) {
            if (it.isModified()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        for (UnnamedConfigurable it : getConfigurables()) {
            it.apply();
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        for (UnnamedConfigurable it : getConfigurables()) {
            it.reset();
        }
        myCurrentProjectSettings = BazelProjectSettings.getBazelProjectSettings(myProject);
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        List<UnnamedConfigurable> configurables = myConfigurables;
        if (configurables != null) {
            for (UnnamedConfigurable it : configurables) {
                it.disposeUIResources();
            }
        }
        myConfigurables = null;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.projectSettingsExperimentalSettings();
    }

    @Override
    public String getId() {
        return ID;
    }

    // consulo: upstream nested <configurable> of <projectConfigurable id="bazel.project.settings">
    @Override
    public @Nullable String getParentId() {
        return BazelProjectSettingsConfigurable.ID;
    }

    // consulo: upstream BazelExperimentalProjectSettingsConfigurableProvider.canCreateConfigurable()
    @Override
    public boolean needDisplay() {
        return !BazelSettingsProvider.getBazelExperimentalSettingsProviders(myProject).isEmpty();
    }
}

class BazelExperimentalProjectSettingsConfigurableProvider extends ConfigurableProvider {
    private final Project myProject;

    BazelExperimentalProjectSettingsConfigurableProvider(Project project) {
        myProject = project;
    }

    public boolean canCreateConfigurable() {
        return !BazelSettingsProvider.getBazelExperimentalSettingsProviders(myProject).isEmpty();
    }

    // consulo: not used, Consulo instantiates the @ExtensionImpl BazelExperimentalProjectSettingsConfigurable directly
    // (canCreateConfigurable() is OptionalConfigurable.needDisplay() there)
    @Override
    public Configurable createConfigurable() {
        return new BazelExperimentalProjectSettingsConfigurable(myProject);
    }
}
