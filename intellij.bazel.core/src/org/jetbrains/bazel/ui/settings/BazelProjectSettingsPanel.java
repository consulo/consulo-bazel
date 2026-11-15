package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.HasValidator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.inject.Inject;
import org.jetbrains.bazel.buildifier.BuildifierUtil;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.settings.bazel.BazelProjectSettings;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// consulo: upstream BoundCompositeSearchableConfigurable (Kotlin UI DSL) -> ProjectConfigurable with a FormBuilder panel,
// the composite part (child configurables of the settings providers) is implemented by hand
@ExtensionImpl
class BazelProjectSettingsConfigurable implements ProjectConfigurable, SearchableConfigurable {
    public static final String ID = "bazel.project.settings";
    public static final String DISPLAY_NAME_KEY = "project.settings.display.name";

    // consulo: upstream XML <projectConfigurable parentId="build.tools">
    public static final String PARENT_ID = "build.tools";

    private final Project myProject;

    private final JTextField myProjectViewPathField;
    // consulo: TextFieldWithBrowseButton (an ExtendableTextField) -> Consulo UI TextBox with a browse button, created with the UI
    private FileChooserTextBoxBuilder.@Nullable Controller myBuildifierExecutablePathField;
    private final JBCheckBox myRunBuildifierOnSaveCheckBox;

    private BazelProjectSettings myCurrentProjectSettings;

    private @Nullable String myDetectedBuildifier = null;

    private @Nullable List<UnnamedConfigurable> myConfigurables = null;

    // consulo: the ExtendableTextComponent.Extension is an action of the TextBox suffix toolbar; it is visible when
    // updateBuildifierRevertButton() would have added the extension
    private final AnAction myBuildifierRevertExtension = new BuildifierRevertAction();

    // consulo: the TextBox value is read on the UI thread only, the action updates in background
    private volatile String myBuildifierExecutablePathText = "";

    @Inject
    public BazelProjectSettingsConfigurable(Project project) {
        myProject = project;
        myCurrentProjectSettings = BazelProjectSettings.getBazelProjectSettings(project);

        myProjectViewPathField = initProjectViewFileField();
        myRunBuildifierOnSaveCheckBox = initRunBuildifierOnSaveCheckBox();
    }

    // consulo: no Configurable.WithEpDependencies
    //override fun getDependencies(): List<BaseExtensionPointName<*>> = listOf(BazelGeneralSettingsProvider.ep)

    private List<UnnamedConfigurable> createConfigurables() {
        List<UnnamedConfigurable> result = new ArrayList<>();
        for (BazelSettingsProvider it : BazelSettingsProvider.getBazelGeneralSettingsProviders(myProject)) {
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

    private JComponent createPanel(Disposable parentDisposable) {
        FileChooserTextBoxBuilder.Controller buildifierExecutablePathField = initBuildifierExecutablePathField(parentDisposable);
        myBuildifierExecutablePathField = buildifierExecutablePathField;
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addLabeledComponent(BazelPluginLocalize.projectSettingsProjectViewLabel().get(), myProjectViewPathField)
            .addLabeledComponent(BazelPluginLocalize.projectSettingsBuildifierLabel().get(), TargetAWT.to(buildifierExecutablePathField.getComponent()))
            .addComponent(myRunBuildifierOnSaveCheckBox);

        buildifierExecutablePathField.getComponent().addValidator(value -> buildifierExecutableValidationInfo());

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
        return builder.addComponentFillVertically(new javax.swing.JPanel(), 0).getPanel();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        return TargetAWT.wrap(createPanel(parentDisposable));
    }

    private JTextField initProjectViewFileField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        // consulo: upstream columns(COLUMNS_SHORT)
        field.setColumns(18);
        return field;
    }

    @RequiredUIAccess
    private FileChooserTextBoxBuilder.Controller initBuildifierExecutablePathField(Disposable parentDisposable) {
        // consulo: DslComponentProperty.INTERACTIVE_COMPONENT is a Kotlin UI DSL property
        LocalizeValue title = BazelPluginLocalize.buildifierSelectPathToExecutable();
        FileChooserTextBoxBuilder.Controller field = FileChooserTextBoxBuilder.create(myProject)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
            .dialogTitle(title)
            .firstActions(myBuildifierRevertExtension)
            .uiDisposable(parentDisposable)
            .build();
        // consulo: TextComponentEmptyText.setupPlaceholderVisibility() is not needed, the TextBox shows its placeholder
        // updateBuildifierRevertButton(): the revert action updates its visibility itself

        field.getComponent().addValueListener(event -> {
            String text = field.getValue();
            myBuildifierExecutablePathText = text;
            @Nullable Path newPath = !text.isBlank() ? Path.of(text) : null;
            myCurrentProjectSettings = myCurrentProjectSettings.withNewBuildifierExecutablePath(newPath);
        });
        return field;
    }

    @RequiredUIAccess
    private HasValidator.@Nullable ValidationInfo buildifierExecutableValidationInfo() {
        FileChooserTextBoxBuilder.Controller field = myBuildifierExecutablePathField;
        if (field == null) {
            return null;
        }
        String text = field.getValue();
        return validateBuildifierExecutable(!text.isBlank() ? text : null);
    }

    private JBCheckBox initRunBuildifierOnSaveCheckBox() {
        JBCheckBox checkBox = new JBCheckBox(BazelPluginLocalize.projectSettingsPluginRunBuildifierOnSaveCheckboxText().get());
        checkBox.setSelected(myCurrentProjectSettings.runBuildifierOnSave());
        checkBox.addItemListener(e ->
            myCurrentProjectSettings =
                new BazelProjectSettings(myCurrentProjectSettings.buildifierExecutablePath(), checkBox.isSelected())
        );
        return checkBox;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        boolean childrenModified = false;
        for (UnnamedConfigurable it : getConfigurables()) {
            if (it.isModified()) {
                childrenModified = true;
                break;
            }
        }
        return childrenModified ||
            !myCurrentProjectSettings.equals(BazelProjectSettings.getBazelProjectSettings(myProject));
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        for (UnnamedConfigurable it : getConfigurables()) {
            it.apply();
        }
        BazelProjectSettings.setBazelProjectSettings(myProject, myCurrentProjectSettings);
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        for (UnnamedConfigurable it : getConfigurables()) {
            it.reset();
        }
        BazelProjectSettings savedSettings = BazelProjectSettings.getBazelProjectSettings(myProject);
        myProjectViewPathField.setText(String.valueOf(ProjectViewService.getInstance(myProject).getProjectViewPath()));

        myDetectedBuildifier = BuildifierUtil.detectBuildifierExecutable(myProject);
        Path buildifierExecutablePath = savedSettings.buildifierExecutablePath();
        FileChooserTextBoxBuilder.Controller buildifierExecutablePathField = myBuildifierExecutablePathField;
        if (buildifierExecutablePathField != null) {
            buildifierExecutablePathField.setValue(buildifierExecutablePath != null ? buildifierExecutablePath.toString() : "");
            myBuildifierExecutablePathText = buildifierExecutablePathField.getValue();
        }
        updateBuildifierEmptyText();
        // consulo: updateBuildifierRevertButton(): the revert action updates its visibility itself

        myRunBuildifierOnSaveCheckBox.setSelected(savedSettings.runBuildifierOnSave());

        myCurrentProjectSettings = savedSettings;
        if (buildifierExecutablePathField != null) {
            buildifierExecutablePathField.getComponent().validate();
        }
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

    @RequiredUIAccess
    private void updateBuildifierEmptyText() {
        String detectedBuildifier = myDetectedBuildifier;
        LocalizeValue newEmptyText =
            detectedBuildifier != null
                ? BazelPluginLocalize.projectSettingsBuildifierPlaceholder(detectedBuildifier)
                : BazelPluginLocalize.buildifierExecutableNotFound(Platform.current().os().isWindows() ? 0 : 1);
        FileChooserTextBoxBuilder.Controller field = myBuildifierExecutablePathField;
        if (field != null) {
            field.getComponent().setPlaceholder(newEmptyText);
        }
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.projectSettingsDisplayName();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return PARENT_ID;
    }

    private final class BuildifierRevertAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private BuildifierRevertAction() {
            super(
                BazelPluginLocalize.projectSettingsBuildifierRevertTooltip(),
                LocalizeValue.empty(),
                PlatformIconGroup.actionsRollback()
            );
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            FileChooserTextBoxBuilder.Controller field = myBuildifierExecutablePathField;
            if (field != null) {
                field.setValue("");
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myDetectedBuildifier != null && !myBuildifierExecutablePathText.isEmpty());
        }
    }

    static final class SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
        static final List<String> KEYS =
            List.of(
                "project.settings.buildifier.label",
                "project.settings.project.view.label",
                "project.settings.plugin.title",
                "project.settings.plugin.run.buildifier.on.save.checkbox.text"
            );

        private SearchIndex() {
        }
    }

    private HasValidator.@Nullable ValidationInfo validateBuildifierExecutable(@Nullable String executablePath) {
        @Nullable LocalizeValue message;
        if (executablePath == null || executablePath.isEmpty()) {
            if (myDetectedBuildifier == null) {
                message = BazelPluginLocalize.buildifierExecutableNotFoundBrief();
            }
            else {
                message = null;
            }
        }
        else if (!isAbsolutePath(executablePath)) {
            message = BazelPluginLocalize.pathValidationMustBeAbsolute();
        }
        else if (executablePath.endsWith(" ")) {
            message = BazelPluginLocalize.pathValidationEndsWithWhitespace();
        }
        else {
            try {
                Path nioPath = Path.of("").resolve(executablePath);
                message = getErrorMessage(nioPath);
            }
            catch (InvalidPathException e) {
                message = BazelPluginLocalize.pathValidationInvalid(Objects.requireNonNullElse(e.getMessage(), ""));
            }
            // consulo: Java rejects catching IOException that the try block cannot throw
            //catch (e: IOException) {
            //  BazelPluginBundle.message("path.validation.inaccessible", e.message.orEmpty())
            //}
        }
        return message != null ? new HasValidator.ValidationInfo(message.get()) : null;
    }

    private @Nullable LocalizeValue getErrorMessage(Path receiver) {
        if (Files.isRegularFile(receiver)) {
            return Files.isExecutable(receiver) ? null : BazelPluginLocalize.pathValidationCannotExecute(receiver);
        }
        else if (Files.isDirectory(receiver)) {
            return BazelPluginLocalize.pathValidationCannotExecute(receiver);
        }
        else {
            return BazelPluginLocalize.pathValidationFileNotFound(receiver);
        }
    }

    private boolean isAbsolutePath(String path) {
        // consulo: upstream com.intellij.execution.Platform.current() UNIX/WINDOWS
        if (!Platform.current().os().isWindows()) {
            return path.startsWith("/");
        }
        else {
            // On Windows user may create project in \\wsl
            return isAbsoluteDosPath(path) || path.startsWith("\\\\wsl");
        }
    }

    // TODO consulo: switch to consulo.util.io.OSAgnosticPathUtil.isAbsoluteDosPath (ported to the platform, not yet in the
    //  published snapshot); same logic
    private static boolean isAbsoluteDosPath(String path) {
        char drive = path.isEmpty() ? 0 : path.charAt(0);
        boolean isDriveLetter = drive >= 'a' && drive <= 'z' || drive >= 'A' && drive <= 'Z';
        return path.length() > 2 && path.charAt(1) == ':' && isDriveLetter &&
            (path.charAt(2) == '/' || path.charAt(2) == '\\');
    }
}

class BazelProjectSettingsConfigurableProvider extends ConfigurableProvider {
    private final Project myProject;

    BazelProjectSettingsConfigurableProvider(Project project) {
        myProject = project;
    }

    // consulo: not used, Consulo instantiates the @ExtensionImpl BazelProjectSettingsConfigurable directly
    @Override
    public Configurable createConfigurable() {
        return new BazelProjectSettingsConfigurable(myProject);
    }
}
