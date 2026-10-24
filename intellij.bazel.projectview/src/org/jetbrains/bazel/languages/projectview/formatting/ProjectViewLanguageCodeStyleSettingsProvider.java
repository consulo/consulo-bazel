package org.jetbrains.bazel.languages.projectview.formatting;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.configurable.Configurable;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.language.codeStyle.setting.IndentOptionsEditor;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractConfigurable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.SmartIndentOptionsEditor;
import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ProjectViewLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public @Nullable String getCodeSample(SettingsType settingsType) {
        return """
            targets:
              //java/com/google/android/myproject:MyProjectDevTarget
              -//java/com/google/android/myproject:MyExpensiveTarget
              //javatests/com/google/android/myproject/...""";
    }

    protected void customizeDefaults(CommonCodeStyleSettings commonSettings, CommonCodeStyleSettings.IndentOptions indentOptions) {
        commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false;
        commonSettings.LINE_COMMENT_ADD_SPACE = true;
        indentOptions.TAB_SIZE = 2;
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 2;
    }

    // consulo: there is no customizeDefaults() hook, so it is applied here like the IntelliJ base implementation does
    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings commonSettings = new CommonCodeStyleSettings(getLanguage());
        CommonCodeStyleSettings.IndentOptions indentOptions = commonSettings.initIndentOptions();
        customizeDefaults(commonSettings, indentOptions);
        return commonSettings;
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new ProjectViewCodeStyleSettings(settings);
    }

    @Override
    public LocalizeValue getConfigurableDisplayName() {
        return BazelProjectViewLocalize.bazelLanguageCodestyleDisplayName();
    }

    // consulo: upstream createConfigurable(...): CodeStyleConfigurable is createSettingsPage(...): Configurable
    @Override
    public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new ProjectViewCodeStyleMainPanel(getCurrentSettings(), settings);
            }
        };
    }

    private static class ProjectViewCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        ProjectViewCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(ProjectViewLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        @RequiredUIAccess
        protected void initTabs(CodeStyleSettings settings) {
            addIndentOptionsTab(settings);
        }
    }
}
