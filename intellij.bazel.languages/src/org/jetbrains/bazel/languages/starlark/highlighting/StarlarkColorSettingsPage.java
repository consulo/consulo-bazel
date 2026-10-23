package org.jetbrains.bazel.languages.starlark.highlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@ExtensionImpl
public class StarlarkColorSettingsPage implements ColorSettingsPage {
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return new AttributesDescriptor[]{
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextKeyword(), StarlarkHighlightingColors.KEYWORD),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextString(), StarlarkHighlightingColors.STRING),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextNumber(), StarlarkHighlightingColors.NUMBER),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextLinecomment(), StarlarkHighlightingColors.LINE_COMMENT),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextSemicolon(), StarlarkHighlightingColors.SEMICOLON),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextComma(), StarlarkHighlightingColors.COMMA),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextDot(), StarlarkHighlightingColors.DOT),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextParentheses(), StarlarkHighlightingColors.PARENTHESES),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextBrackets(), StarlarkHighlightingColors.BRACKETS),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextIdentifier(), StarlarkHighlightingColors.IDENTIFIER),
            new AttributesDescriptor(
                StarlarkLocalize.highlighterDescriptorTextFunctiondeclaration(),
                StarlarkHighlightingColors.FUNCTION_DECLARATION
            ),
            new AttributesDescriptor(StarlarkLocalize.highlighterDescriptorTextNamedargument(), StarlarkHighlightingColors.NAMED_ARGUMENT),
        };
    }

    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(StarlarkLanguage.INSTANCE.getID());
    }

    // TODO consulo: ColorSettingsPage has no getIcon()
    // override fun getIcon(): Icon = BazelLanguagesIcons.bazel

    @Override
    public SyntaxHighlighter getHighlighter() {
        return StarlarkSyntaxHighlighter.INSTANCE;
    }

    @Override
    public String getDemoText() {
        return """
            # Line comment

            def fun(param, optional_param = "string", *varargs, **kwargs):
                return 42

            if x > 0:
                result = +1
            elif x < 0:
                result = -1
            else:
                result = 0

            for x in 1, 2, 3:
                print(x)
               \s
            def func(a, b, c):
                return a + b * c
               \s
            func(c = 3, a = 2, b = 1); func(1, 2, 3)

            "banana".count("a")
            numbers = {"one": 1, "two": 2}""";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
