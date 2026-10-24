package org.jetbrains.bazel.languages.projectview.lexer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A base class responsible for tokenizing a given {@code input} project view into a list of tokens.
 */
public class ProjectViewLexerBase {
    private final List<Token> myTokens = new ArrayList<>();
    private String myBuffer;
    private int myPos = 0;
    private @Nullable Integer myIdentifierStart = null;
    private boolean myIsPosAfterNonWhitespaceCharInLine = false;
    private @Nullable ProjectViewTokenType myLastTokenType = null;

    /**
     * Creates a lexer which returns a list of tokens for the given {@code input}.
     *
     * @param input The input to tokenize.
     */
    public ProjectViewLexerBase(CharSequence input) {
        myBuffer = input.toString();
        tokenize();
    }

    public List<Token> getTokens() {
        return myTokens;
    }

    private void tokenize() {
        char c;

        while (myPos < myBuffer.length()) {
            c = myBuffer.charAt(myPos);
            myPos++;

            if (c == '\n') {
                addPrecedingIdentifier(myPos - 1);
                myTokens.add(new Token(ProjectViewTokenType.NEWLINE, myPos - 1, myPos));
                myLastTokenType = ProjectViewTokenType.NEWLINE;
                myIsPosAfterNonWhitespaceCharInLine = false;
            }
            else if (isHorizontalWhitespace(c)) {
                addPrecedingIdentifier(myPos - 1);
                handleWhiteSpace();
            }
            else if (c == ':') {
                addPrecedingIdentifier(myPos - 1);
                myTokens.add(new Token(ProjectViewTokenType.COLON, myPos - 1, myPos));
            }
            else if (c == '#' && !myIsPosAfterNonWhitespaceCharInLine) {
                addPrecedingIdentifier(myPos - 1);
                addCommentLine(myPos - 1);
            }
            else {
                myIsPosAfterNonWhitespaceCharInLine = true;
                if (myIdentifierStart == null) {
                    myIdentifierStart = myPos - 1;
                }
            }
        }

        addPrecedingIdentifier(myPos);
    }

    private void addPrecedingIdentifier(int end) {
        Integer it = myIdentifierStart;
        if (it != null) {
            myTokens.add(new Token(getIdentifierToken(it, end), it, end));
            myIdentifierStart = null;
        }
    }

    private void addCommentLine(int start) {
        while (myPos < myBuffer.length() && myBuffer.charAt(myPos) != '\n') {
            myPos++;
        }
        myTokens.add(new Token(ProjectViewTokenType.COMMENT, start, myPos));
    }

    private void handleWhiteSpace() {
        // Every whitespace followed by a single-line comment or a newline is fused with the following token.
        int oldPos = myPos - 1;
        while (myPos < myBuffer.length()) {
            if (isHorizontalWhitespace(myBuffer.charAt(myPos))) {
                myPos++;
                continue;
            }

            if (myIsPosAfterNonWhitespaceCharInLine || myBuffer.charAt(myPos) == '#' || myBuffer.charAt(myPos) == '\n') {
                myTokens.add(new Token(ProjectViewTokenType.WHITESPACE, oldPos, myPos));
            }
            else {
                myTokens.add(new Token(ProjectViewTokenType.INDENT, oldPos, myPos));
            }

            return;
        }

        myTokens.add(new Token(ProjectViewTokenType.WHITESPACE, oldPos, myPos));
    }

    private ProjectViewTokenType getIdentifierToken(int start, int end) {
        String identifier = myBuffer.substring(start, end);
        Character prevChar = start - 1 >= 0 && start - 1 < myBuffer.length() ? myBuffer.charAt(start - 1) : null;
        if (identifier.equals("import")) {
            return ProjectViewTokenType.IMPORT_KEYWORD;
        }
        else if (identifier.equals("try_import")) {
            return ProjectViewTokenType.TRY_IMPORT_KEYWORD;
        }
        else if ((Objects.equals(prevChar, '\n') || prevChar == null) && myBuffer.length() > end && myBuffer.charAt(end) == ':') {
            return ProjectViewTokenType.SECTION_KEYWORD;
        }
        else {
            return ProjectViewTokenType.IDENTIFIER;
        }
    }

    private boolean isHorizontalWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r';
    }

    /**
     * Represents a token in the project view file.
     *
     * @param type  The type of the token.
     * @param start Position of the first character of the token in the buffer passed to the {@link ProjectViewLexerBase} constructor.
     * @param end   Sum of {@code start} and the length of the token.
     */
    public record Token(
        ProjectViewTokenType type,
        int start,
        int end
    ) {
    }
}
