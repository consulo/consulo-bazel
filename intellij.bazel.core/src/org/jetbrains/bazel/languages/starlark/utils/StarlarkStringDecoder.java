package org.jetbrains.bazel.languages.starlark.utils;

import org.jspecify.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class StarlarkStringDecoder {
    private StarlarkStringDecoder() {
    }

    /**
     * Decodes a Starlark string literal value according to Bazel's lexer behavior:
     * https://github.com/bazelbuild/bazel/blob/6df8716a2497d1d084c2149b12719f8c4b7aff4e/src/main/java/net/starlark/java/syntax/Lexer.java
     * <p>
     * This method returns the token value that Bazel's lexer would assign. It intentionally preserves
     * invalid escape sequences as '\' + escaped character, because Bazel reports a lexer error but still
     * stores that value in the STRING token.
     */
    public static @Nullable String decodeLiteral(String text) {
        StringLiteral literal = parseLiteral(text);
        if (literal == null) {
            return null;
        }
        return decodeContent(
            literal.content(),
            literal.quote().getQuote().charAt(0),
            literal.isRaw(),
            isTripleQuoted(literal.quote())
        );
    }

    private static @Nullable StringLiteral parseLiteral(String text) {
        boolean isRaw = text.startsWith("r") && isQuote(getOrNull(text, 1));
        String quotedText = text.substring(isRaw ? 1 : 0);
        StarlarkQuote quote = StarlarkQuote.ofString(quotedText);

        if (quote == StarlarkQuote.UNQUOTED) {
            return null;
        }
        if (quotedText.length() < quote.getQuote().length() * 2) {
            return null;
        }
        if (!quotedText.endsWith(quote.getQuote())) {
            return null;
        }

        return new StringLiteral(quote.unwrap(quotedText), quote, isRaw);
    }

    private static boolean isTripleQuoted(StarlarkQuote receiver) {
        return receiver == StarlarkQuote.TRIPLE_SINGLE || receiver == StarlarkQuote.TRIPLE_DOUBLE;
    }

    private static boolean isQuote(@Nullable Character receiver) {
        return receiver != null && (receiver == '\'' || receiver == '"');
    }

    private record StringLiteral(String content, StarlarkQuote quote, boolean isRaw) {
    }

    private static String decodeContent(String text, char quote, boolean isRaw, boolean isTripleQuoted) {
        StringBuilder literal = new StringBuilder();
        int index = 0;

        while (index < text.length()) {
            char c = text.charAt(index);
            index++;

            switch (c) {
                case '\n' -> {
                    if (isTripleQuoted) {
                        appendByte(literal, '\n');
                    }
                    else {
                        return literal.toString();
                    }
                }

                case '\\' -> {
                    if (index == text.length()) {
                        return literal.toString();
                    }

                    if (isRaw) {
                        appendByte(literal, '\\');
                        if (equalsChar(getOrNull(text, index), '\r') && equalsChar(getOrNull(text, index + 1), '\n')) {
                            appendByte(literal, '\n');
                            index += 2;
                        }
                        else if (text.charAt(index) == '\r' || text.charAt(index) == '\n') {
                            appendByte(literal, '\n');
                            index += 1;
                        }
                        else {
                            index = appendUtf8CodePoint(literal, text, index);
                        }
                    }
                    else {
                        char escaped = text.charAt(index);
                        index++;
                        switch (escaped) {
                            case '\r' -> {
                                if (equalsChar(getOrNull(text, index), '\n')) {
                                    index += 1;
                                }
                            }
                            case '\n' -> {
                            }
                            case 'a' -> appendByte(literal, 0x07);
                            case 'b' -> appendByte(literal, '\b');
                            case 'f' -> appendByte(literal, '\u000C');
                            case 'n' -> appendByte(literal, '\n');
                            case 'r' -> appendByte(literal, '\r');
                            case 't' -> appendByte(literal, '\t');
                            case 'v' -> appendByte(literal, 0x0B);
                            case '\\' -> appendByte(literal, '\\');
                            case '\'' -> appendByte(literal, '\'');
                            case '"' -> appendByte(literal, '"');
                            case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                                OctalEscape octal = readOctalEscape(text, escaped, index);
                                appendByte(literal, octal.value() & BYTE_MAX_VALUE);
                                index = octal.endOffset();
                            }
                            default -> {
                                appendByte(literal, '\\');
                                index = appendUtf8CodePoint(literal, text, index - 1);
                            }
                        }
                    }
                }

                case '\'', '"' -> {
                    if (!isTripleQuoted && c == quote) {
                        return literal.toString();
                    }
                    else {
                        index = appendUtf8CodePoint(literal, text, index - 1);
                    }
                }

                default -> index = appendUtf8CodePoint(literal, text, index - 1);
            }
        }

        return literal.toString();
    }

    private static int appendUtf8CodePoint(StringBuilder receiver, String text, int offset) {
        int codePoint = Character.codePointAt(text, offset);
        for (byte it : new String(Character.toChars(codePoint)).getBytes(UTF_8)) {
            appendByte(receiver, it & BYTE_MAX_VALUE);
        }
        return offset + Character.charCount(codePoint);
    }

    private static StringBuilder appendByte(StringBuilder receiver, int value) {
        return receiver.append((char) (value & BYTE_MAX_VALUE));
    }

    private static OctalEscape readOctalEscape(String text, char firstDigit, int offset) {
        int value = firstDigit - '0';
        int index = offset;
        for (int i = 0; i < MAX_OCTAL_ESCAPE_LENGTH - 1; i++) {
            if (index < text.length() && text.charAt(index) >= '0' && text.charAt(index) <= '7') {
                value = (value << OCTAL_SHIFT) | (text.charAt(index) - '0');
                index++;
            }
        }
        return new OctalEscape(value, index);
    }

    private record OctalEscape(int value, int endOffset) {
    }

    // consulo: Kotlin String.getOrNull(index)
    private static @Nullable Character getOrNull(String text, int index) {
        return index >= 0 && index < text.length() ? text.charAt(index) : null;
    }

    // consulo: Kotlin Char? == Char
    private static boolean equalsChar(@Nullable Character c, char expected) {
        return c != null && c == expected;
    }

    private static final int OCTAL_SHIFT = 3;
    private static final int MAX_OCTAL_ESCAPE_LENGTH = 3;
    private static final int BYTE_MAX_VALUE = 0xff;
}
