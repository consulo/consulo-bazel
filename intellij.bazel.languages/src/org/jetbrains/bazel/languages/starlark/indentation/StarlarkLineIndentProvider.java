package org.jetbrains.bazel.languages.starlark.indentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.IndentInfo;
import consulo.language.codeStyle.lineIndent.LineIndentProvider;
import consulo.language.codeStyle.lineIndent.SemanticEditorPositionFactory;
import consulo.project.Project;
import consulo.util.lang.CharArrayUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

// consulo: the Consulo LineIndentProvider receives the Document and a SemanticEditorPositionFactory instead of the Editor
@ExtensionImpl
public class StarlarkLineIndentProvider implements LineIndentProvider {
    private static final List<IElementType> WHITE_SPACE_OR_COMMENT =
        Stream.concat(
            Arrays.stream(StarlarkTokenSets.WHITESPACE.getTypes()),
            Arrays.stream(StarlarkTokenSets.COMMENT.getTypes())
        ).toList();

    private static final List<IElementType> BLOCK_OPENING =
        Stream.concat(
            Arrays.stream(StarlarkTokenSets.OPEN_BRACKETS.getTypes()),
            Stream.of(StarlarkTokenTypes.COLON)
        ).toList();

    @Override
    public @Nullable String getLineIndent(
        Project project,
        Document document,
        SemanticEditorPositionFactory factory,
        Language language,
        int offset
    ) {
        return offset > 0 ? getIndent(project, document, factory, offset) : "";
    }

    private @Nullable String getIndent(Project project, Document document, SemanticEditorPositionFactory factory, int offset) {
        StarlarkSemanticEditorPosition currentPosition = new StarlarkSemanticEditorPosition(factory, offset);
        StarlarkSemanticEditorPosition previousPosition = currentPosition.getPreviousPositionExcluding(WHITE_SPACE_OR_COMMENT);
        if (previousPosition.isAtAnyOf(BLOCK_OPENING)) {
            return getIndentString(project, document, previousPosition.startOffset());
        }
        else {
            return null;
        }
    }

    private String getIndentString(Project project, Document document, int offset) {
        // consulo: upstream used CodeStyle.getSettings(editor)
        CodeStyleSettings settings = CodeStyle.getSettings(project, document);
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions(StarlarkFileType.INSTANCE);
        CharSequence docChars = document.getCharsSequence();

        String baseIndent = "";
        int indentStart = CharArrayUtil.shiftBackwardUntil(docChars, offset, "\n") + 1;
        if (indentStart >= 0) {
            int indentEnd = CharArrayUtil.shiftForward(docChars, indentStart, " \t");
            if (indentEnd > indentStart) {
                baseIndent = docChars.subSequence(indentStart, indentEnd).toString();
            }
        }
        baseIndent += new IndentInfo(0, indentOptions.INDENT_SIZE, 0).generateNewWhiteSpace(indentOptions);
        return baseIndent;
    }

    @Override
    public boolean isSuitableFor(@Nullable Language language) {
        return language instanceof StarlarkLanguage;
    }
}
