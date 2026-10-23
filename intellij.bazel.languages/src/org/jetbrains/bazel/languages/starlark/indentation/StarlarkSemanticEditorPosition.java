package org.jetbrains.bazel.languages.starlark.indentation;

import java.util.Objects;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.lineIndent.SemanticEditorPosition;
import consulo.language.codeStyle.lineIndent.SemanticEditorPositionFactory;

import java.util.List;

// consulo: LineIndentProvider gets no EditorEx, so upstream's editor highlighter iterator is replaced by the
// platform SemanticEditorPosition (created from the editor highlighter by SemanticEditorPositionFactory)
class StarlarkSemanticEditorPosition {
    private final SemanticEditorPositionFactory myFactory;
    private final SemanticEditorPosition myIterator;
    private final CharSequence myChars;

    StarlarkSemanticEditorPosition(SemanticEditorPositionFactory factory, int offset) {
        myFactory = factory;
        myIterator = factory.create(offset, TokenElement::new);
        myChars = myIterator.getChars();
    }

    public StarlarkSemanticEditorPosition getPreviousPositionExcluding(List<IElementType> elements) {
        StarlarkSemanticEditorPosition position = copy();
        position.moveBeforeAnyOf(elements);
        return position;
    }

    private StarlarkSemanticEditorPosition copy() {
        return new StarlarkSemanticEditorPosition(myFactory, startOffset());
    }

    public int startOffset() {
        return myIterator.getStartOffset();
    }

    private void moveBeforeAnyOf(List<IElementType> elements) {
        if (!myIterator.isAtEnd()) {
            myIterator.moveBefore();
        }
        while (isAtAnyOf(elements)) {
            myIterator.moveBefore();
        }
    }

    public boolean isAtAnyOf(List<IElementType> elements) {
        return !myIterator.isAtEnd() && elements.stream().anyMatch(it -> it == getType());
    }

    private IElementType getType() {
        return ((TokenElement) Objects.requireNonNull(myIterator.getCurrElement())).type();
    }

    @Override
    public String toString() {
        if (myIterator.isAtEnd()) {
            return "atEnd";
        }
        else {
            return getType().toString() + "=>" +
                myChars.subSequence(myIterator.getStartOffset(), Integer.min(myIterator.getStartOffset() + 255, myChars.length()));
        }
    }

    // consulo: maps every token to itself, so that the current token type can be read back from SemanticEditorPosition
    private record TokenElement(IElementType type) implements SemanticEditorPosition.SyntaxElement {
    }
}
