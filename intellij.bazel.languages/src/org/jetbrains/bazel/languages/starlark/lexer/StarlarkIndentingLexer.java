package org.jetbrains.bazel.languages.starlark.lexer;

import consulo.language.ast.IElementType;
import consulo.language.lexer.FlexAdapter;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class PendingToken {
    private @Nullable IElementType myType;
    private final int myStart;
    private final int myEnd;

    PendingToken(@Nullable IElementType type, int start, int end) {
        myType = type;
        myStart = start;
        myEnd = end;
    }

    public @Nullable IElementType getType() {
        return myType;
    }

    public void setType(@Nullable IElementType type) {
        myType = type;
    }

    public int getStart() {
        return myStart;
    }

    public int getEnd() {
        return myEnd;
    }

    @Override
    public String toString() {
        return myType + ":" + myStart + "-" + myEnd;
    }
}

class PendingCommentToken extends PendingToken {
    private final int myIndent;

    PendingCommentToken(@Nullable IElementType type, int start, int end, int indent) {
        super(type, start, end);
        myIndent = indent;
    }

    public int getIndent() {
        return myIndent;
    }
}

public class StarlarkIndentingLexer extends FlexAdapter {
    private final IntList myIndentStack = IntLists.newArrayList();
    private int myBraceLevel = 0;
    private boolean myLineHasSignificantTokens = false;
    private int myLastNewLineIndent = -1;
    private List<PendingToken> myTokenQueue = new ArrayList<>();
    private int myLineBreakBeforeFirstCommentIndex = -1;
    private boolean myProcessSpecialTokensPending = false;

    public StarlarkIndentingLexer() {
        super(new _StarlarkLexer(null));
    }

    @Override
    public @Nullable IElementType getTokenType() {
        PendingToken token = myTokenQueue.isEmpty() ? null : myTokenQueue.get(0);
        IElementType type = token == null ? null : token.getType();
        return type != null ? type : super.getTokenType();
    }

    @Override
    public int getTokenStart() {
        PendingToken token = myTokenQueue.isEmpty() ? null : myTokenQueue.get(0);
        return token != null ? token.getStart() : super.getTokenStart();
    }

    @Override
    public int getTokenEnd() {
        PendingToken token = myTokenQueue.isEmpty() ? null : myTokenQueue.get(0);
        return token != null ? token.getEnd() : super.getTokenEnd();
    }

    @Override
    public void start(
        CharSequence buffer,
        int startOffset,
        int endOffset,
        int initialState
    ) {
        super.start(buffer, startOffset, endOffset, initialState);
        setStartState();
    }

    @Override
    public void advance() {
        if (!myTokenQueue.isEmpty()) {
            myTokenQueue.remove(0);
            if (myProcessSpecialTokensPending) {
                myProcessSpecialTokensPending = false;
                processSpecialTokens();
            }
        }
        else {
            advanceBase();
            processSpecialTokens();
        }
        adjustBraceLevel();
    }

    private void advanceBase() {
        super.advance();
        checkSignificantTokens();
    }

    private void setStartState() {
        myIndentStack.clear();
        myIndentStack.add(0);
        myBraceLevel = 0;
        adjustBraceLevel();
        myLineHasSignificantTokens = false;
        checkSignificantTokens();
        if (isBaseAt(StarlarkTokenTypes.SPACE)) {
            processIndent(0, StarlarkTokenTypes.SPACE);
        }
    }

    private void adjustBraceLevel() {
        IElementType tokenType = getTokenType();
        if (StarlarkTokenSets.OPEN_BRACKETS.contains(tokenType)) {
            myBraceLevel++;
        }
        else if (StarlarkTokenSets.CLOSE_BRACKETS.contains(tokenType)) {
            myBraceLevel--;
        }
    }

    private void checkSignificantTokens() {
        IElementType tokenType = super.getTokenType();
        if (!StarlarkTokenSets.WHITESPACE.contains(tokenType) && tokenType != StarlarkTokenTypes.COMMENT) {
            myLineHasSignificantTokens = true;
        }
    }

    private void processSpecialTokens() {
        if (isBaseAt(StarlarkTokenTypes.LINE_BREAK)) {
            processLineBreak(super.getTokenStart());
            if (isBaseAt(StarlarkTokenTypes.COMMENT)) {
                processComment();
            }
        }
        else if (isBaseAt(StarlarkTokenTypes.SPACE)) {
            processSpace();
        }

        int tokenStart = super.getTokenStart();
        if (super.getTokenType() == null) {
            pushToken(StarlarkTokenTypes.STATEMENT_BREAK, tokenStart, tokenStart);
        }
    }

    private void processSpace() {
        int start = super.getTokenStart();
        int end = super.getTokenEnd();
        while (super.getTokenType() == StarlarkTokenTypes.SPACE) {
            end = super.getTokenEnd();
            advanceBase();
        }
        if (super.getTokenType() == StarlarkTokenTypes.LINE_BREAK) {
            processLineBreak(start);
        }
        else {
            pushToken(StarlarkTokenTypes.SPACE, start, end);
        }
    }

    private void processLineBreak(int startPos) {
        if (myBraceLevel == 0) {
            if (myLineHasSignificantTokens) {
                pushToken(StarlarkTokenTypes.STATEMENT_BREAK, startPos, startPos);
            }
            myLineHasSignificantTokens = false;
            advanceBase();
            processIndent(startPos, StarlarkTokenTypes.LINE_BREAK);
        }
        else {
            processInsignificantLineBreak(startPos);
        }
    }

    private void processInsignificantLineBreak(int startPos) {
        // merge whitespace following the line break character into the line break token
        int end = super.getTokenEnd();
        advanceBase();
        while (StarlarkTokenSets.WHITESPACE.contains(super.getTokenType())) {
            end = super.getTokenEnd();
            advanceBase();
        }
        pushToken(StarlarkTokenTypes.LINE_BREAK, startPos, end);
        myProcessSpecialTokensPending = true;
    }

    private void processComment() {
        myLineBreakBeforeFirstCommentIndex = myTokenQueue.size() - 1;
        while (isBaseAt(StarlarkTokenTypes.COMMENT)) {
            int commentEnd = super.getTokenEnd();
            myTokenQueue.add(
                new PendingCommentToken(
                    super.getTokenType(),
                    super.getTokenStart(),
                    commentEnd,
                    myLastNewLineIndent
                )
            );
            advanceBase();
            if (isBaseAt(StarlarkTokenTypes.LINE_BREAK)) {
                processLineBreak(super.getTokenStart());
            }
            else if (super.getTokenType() == null) {
                closeDanglingSuitesWithComments(0, commentEnd);
            }
            else {
                break;
            }
        }
        myLineBreakBeforeFirstCommentIndex = -1;
    }

    private void processIndent(int whiteSpaceStart, @Nullable IElementType whitespaceTokenType) {
        int lastIndent = topInt(myIndentStack);
        int indent = getNextLineIndent();
        myLastNewLineIndent = indent;
        // don't generate indent/dedent tokens if a line contains only end-of-line comment and whitespace
        if (super.getTokenType() == StarlarkTokenTypes.COMMENT) {
            indent = lastIndent;
        }
        int whiteSpaceEnd = super.getTokenType() == null ? super.getBufferEnd() : super.getTokenStart();
        if (indent > lastIndent) {
            myIndentStack.add(indent);
            pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd);
            int insertIndex = skipPrecedingCommentsWithIndent(indent, myTokenQueue.size() - 1);
            int indentOffset = insertIndex == myTokenQueue.size() ? whiteSpaceEnd : myTokenQueue.get(insertIndex).getStart();
            myTokenQueue.add(insertIndex, new PendingToken(StarlarkTokenTypes.INDENT, indentOffset, indentOffset));
        }
        else if (indent < lastIndent) {
            closeDanglingSuitesWithComments(indent, whiteSpaceStart);
            pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd);
        }
        else {
            pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd);
        }
    }

    private void closeDanglingSuitesWithComments(int indent, int whiteSpaceStart) {
        int lastIndent = topInt(myIndentStack);
        int insertIndex = myLineBreakBeforeFirstCommentIndex == -1 ? myTokenQueue.size() : myLineBreakBeforeFirstCommentIndex;
        int lastSuiteIndent;
        while (indent < lastIndent) {
            lastSuiteIndent = popInt(myIndentStack);
            lastIndent = topInt(myIndentStack);
            int dedentOffset = whiteSpaceStart;
            if (indent > lastIndent) {
                pushToken(StarlarkTokenTypes.INCONSISTENT_DEDENT, whiteSpaceStart, whiteSpaceStart);
                insertIndex = myTokenQueue.size();
            }
            else {
                insertIndex = skipPrecedingCommentsWithSameIndentOnSuiteClose(lastSuiteIndent, insertIndex);
            }
            if (insertIndex != myTokenQueue.size()) {
                dedentOffset = myTokenQueue.get(insertIndex).getStart();
            }
            myTokenQueue.add(insertIndex, new PendingToken(StarlarkTokenTypes.DEDENT, dedentOffset, dedentOffset));
            insertIndex++;
        }
    }

    private int skipPrecedingCommentsWithIndent(int indent, int index) {
        // insert the DEDENT before previous comments that have the same indent as the current token indent
        int idx = index;
        boolean foundComment = false;
        while (idx > 0 && myTokenQueue.get(idx - 1) instanceof PendingCommentToken) {
            PendingCommentToken commentToken = (PendingCommentToken) myTokenQueue.get(idx - 1);
            if (commentToken.getIndent() != indent) {
                break;
            }
            foundComment = true;
            idx--;
            if (isPrecededByCommentWithLineBreak(idx)) {
                idx--;
            }
        }
        return foundComment ? idx : myTokenQueue.size();
    }

    private int skipPrecedingCommentsWithSameIndentOnSuiteClose(int indent, int anchorIndex) {
        int result = anchorIndex;
        for (int i = anchorIndex; i < myTokenQueue.size(); i++) {
            PendingToken token = myTokenQueue.get(i);
            if (token instanceof PendingCommentToken commentToken) {
                if (commentToken.getIndent() < indent) {
                    break;
                }
                result = i + 1;
            }
        }
        return result;
    }

    private int getNextLineIndent() {
        int indent = 0;
        while (super.getTokenType() != null && StarlarkTokenSets.WHITESPACE.contains(super.getTokenType())) {
            if (super.getTokenType() == StarlarkTokenTypes.TAB) {
                indent = (indent / 8 + 1) * 8;
            }
            else if (super.getTokenType() == StarlarkTokenTypes.SPACE) {
                indent++;
            }
            else if (super.getTokenType() == StarlarkTokenTypes.LINE_BREAK) {
                indent = 0;
            }
            advanceBase();
        }
        if (super.getTokenType() == null) {
            return 0;
        }
        else {
            return indent;
        }
    }

    private boolean isBaseAt(IElementType tokenType) {
        return super.getTokenType() == tokenType;
    }

    private boolean pushToken(
        @Nullable IElementType type,
        int start,
        int end
    ) {
        return myTokenQueue.add(new PendingToken(type, start, end));
    }

    private boolean isPrecededByCommentWithLineBreak(int i) {
        return i > 1
            && myTokenQueue.get(i - 1).getType() == StarlarkTokenTypes.LINE_BREAK
            && myTokenQueue.get(i - 2) instanceof PendingCommentToken;
    }

    // fastutil IntArrayList.topInt() replacement
    private static int topInt(IntList stack) {
        return stack.get(stack.size() - 1);
    }

    // fastutil IntArrayList.popInt() replacement
    private static int popInt(IntList stack) {
        return stack.removeByIndex(stack.size() - 1);
    }
}
