package org.jetbrains.bazel.languages.projectview.lexer;

import consulo.language.ast.IElementType;
import consulo.language.lexer.LexerBase;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexerBase.Token;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

/**
 * A lexer that extends {@link LexerBase} using {@link ProjectViewLexerBase} to tokenize text.
 * <p>
 * Create an empty project view lexer.
 */
public class ProjectViewLexer extends LexerBase {
    private int myEndOffset = 0;
    private int myOffsetStart = 0;
    private String myBuffer = "";
    private Iterator<Token> myTokens = Collections.emptyIterator();
    private @Nullable Token myCurrentToken;

    @Override
    public void start(
        CharSequence buffer,
        int startOffset,
        int endOffset,
        int initialState
    ) {
        myBuffer = buffer.toString();
        myOffsetStart = startOffset;
        myEndOffset = endOffset;

        ProjectViewLexerBase lexer = new ProjectViewLexerBase(buffer.subSequence(startOffset, endOffset));
        myTokens = lexer.getTokens().iterator();
        myCurrentToken = myTokens.hasNext() ? myTokens.next() : null;
    }

    /**
     * ProjectViewLexer doesn't use states, so we return 0 as described in the Lexer docs.
     */
    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        Token currentToken = myCurrentToken;
        return currentToken != null ? currentToken.type() : null;
    }

    @Override
    public int getTokenStart() {
        Token currentToken = myCurrentToken;
        return currentToken != null ? currentToken.start() + myOffsetStart : 0;
    }

    @Override
    public int getTokenEnd() {
        Token currentToken = myCurrentToken;
        return currentToken != null ? currentToken.end() + myOffsetStart : 0;
    }

    @Override
    public void advance() {
        myCurrentToken =
            myCurrentToken != null && myTokens.hasNext()
                ? myTokens.next()
                : null;
    }

    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }
}
