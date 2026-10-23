package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;
import org.jspecify.annotations.Nullable;

/**
 * Maps between the source text of a Starlark string literal and its decoded value, so that
 * languages injected into the literal see the actual string value and can map offsets back.
 * <p>
 * Handles the escape sequences supported by Starlark: `\n`, `\t`, `\r`, `\a`, `\b`, `\f`, `\v`,
 * `\\`, `\"`, `\'`, `\<newline>` (line continuation), `\ooo`, `\xhh`, `\&#117;hhhh` and `\Uhhhhhhhh`.
 * Unknown escape sequences are kept as-is. Raw strings (`r"..."`) are not decoded at all.
 * <p>
 * The companion also provides the inverse operation, {@link #escape}, which is used when the contents of a literal are
 * replaced.
 */
public class StarlarkStringLiteralEscaper extends LiteralTextEscaper<StarlarkStringLiteralExpression> {
    /**
     * For every offset in the decoded text, the corresponding offset in the (sub-)text of the host.
     */
    private int[] myOutSourceOffsets = new int[0];
    private int myDecodedLength = 0;

    public StarlarkStringLiteralEscaper(StarlarkStringLiteralExpression host) {
        super(host);
    }

    @Override
    public boolean decode(TextRange rangeInsideHost, StringBuilder outChars) {
        String subText = rangeInsideHost.substring(myHost.getText());
        int outStart = outChars.length();
        myOutSourceOffsets = new int[subText.length() + 1];
        if (myHost.isRaw()) {
            outChars.append(subText);
            for (int i = 0; i <= subText.length(); i++) {
                myOutSourceOffsets[i] = i;
            }
        }
        else {
            decodeEscapes(subText, outChars, myOutSourceOffsets);
        }
        myDecodedLength = outChars.length() - outStart;
        return true;
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, TextRange rangeInsideHost) {
        if (offsetInDecoded < 0 || offsetInDecoded > myDecodedLength) {
            return -1;
        }
        int result = myOutSourceOffsets[offsetInDecoded];
        return rangeInsideHost.getStartOffset() + Math.min(result, rangeInsideHost.getLength());
    }

    @Override
    public boolean isOneLine() {
        return !myHost.getQuote().isTriple();
    }

    private static final char BELL = '\u0007';
    private static final char BACKSPACE = '\u0008';
    private static final char FORM_FEED = '\u000C';
    private static final char VERTICAL_TAB = '\u000B';

    /**
     * Decodes {@code source} into {@code outChars}, recording for every decoded character (and for the end of the decoded text)
     * the offset in {@code source} it originates from in {@code sourceOffsets}. The decoded text is never longer than {@code source},
     * so {@code sourceOffsets} needs to have at least `source.length + 1` entries.
     */
    public static void decodeEscapes(
        String source,
        StringBuilder outChars,
        int[] sourceOffsets
    ) {
        // consulo: Kotlin local functions sharing the mutable `index` are moved into EscapesDecoder
        new EscapesDecoder(source, outChars, sourceOffsets).decode();
    }

    private static final class EscapesDecoder {
        private final String mySource;
        private final StringBuilder myOutChars;
        private final int[] mySourceOffsets;
        private final int myOutStart;
        private int myIndex = 0;

        private EscapesDecoder(String source, StringBuilder outChars, int[] sourceOffsets) {
            mySource = source;
            myOutChars = outChars;
            mySourceOffsets = sourceOffsets;
            myOutStart = outChars.length();
        }

        private void record(int sourceIndex) {
            mySourceOffsets[myOutChars.length() - myOutStart] = sourceIndex;
        }

        private void appendCodePoint(int codePoint, int sourceIndex) {
            myOutChars.appendCodePoint(codePoint);
            if (Character.charCount(codePoint) == 2) {
                // Both chars of the surrogate pair originate from the same escape sequence.
                mySourceOffsets[myOutChars.length() - myOutStart - 1] = sourceIndex;
            }
        }

        private boolean tryDecodeHex(int digits) {
            int start = myIndex + 2;
            int end = start + digits;
            if (end > mySource.length()) {
                return false;
            }
            String hex = mySource.substring(start, end);
            for (int i = 0; i < hex.length(); i++) {
                if (!isHexDigit(hex.charAt(i))) {
                    return false;
                }
            }
            long codePoint = Long.parseLong(hex, 16);
            if (codePoint > Character.MAX_CODE_POINT) {
                return false;
            }
            appendCodePoint((int) codePoint, myIndex);
            myIndex = end;
            return true;
        }

        private void keepBackslash() {
            // The backslash is kept literally, the following char is handled by the next iteration.
            myOutChars.append('\\');
            myIndex++;
        }

        private void decode() {
            while (myIndex < mySource.length()) {
                record(myIndex);
                char c = mySource.charAt(myIndex);
                if (c != '\\' || myIndex + 1 >= mySource.length()) {
                    myOutChars.append(c);
                    myIndex++;
                    continue;
                }
                char next = mySource.charAt(myIndex + 1);
                switch (next) {
                    case 'n' -> {
                        myOutChars.append('\n');
                        myIndex += 2;
                    }
                    case 't' -> {
                        myOutChars.append('\t');
                        myIndex += 2;
                    }
                    case 'r' -> {
                        myOutChars.append('\r');
                        myIndex += 2;
                    }
                    case 'a' -> {
                        myOutChars.append(BELL);
                        myIndex += 2;
                    }
                    case 'b' -> {
                        myOutChars.append(BACKSPACE);
                        myIndex += 2;
                    }
                    case 'f' -> {
                        myOutChars.append(FORM_FEED);
                        myIndex += 2;
                    }
                    case 'v' -> {
                        myOutChars.append(VERTICAL_TAB);
                        myIndex += 2;
                    }
                    case '\\', '"', '\'' -> {
                        myOutChars.append(next);
                        myIndex += 2;
                    }
                    // A backslash followed by a newline is a line continuation: both characters are dropped.
                    case '\n' -> myIndex += 2;
                    case 'x' -> {
                        if (!tryDecodeHex(2)) {
                            keepBackslash();
                        }
                    }
                    case 'u' -> {
                        if (!tryDecodeHex(4)) {
                            keepBackslash();
                        }
                    }
                    case 'U' -> {
                        if (!tryDecodeHex(8)) {
                            keepBackslash();
                        }
                    }
                    case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                        int end = myIndex + 2;
                        while (end < mySource.length() && end < myIndex + 4 && mySource.charAt(end) >= '0' && mySource.charAt(end) <= '7') {
                            end++;
                        }
                        appendCodePoint(Integer.parseInt(mySource.substring(myIndex + 1, end), 8), myIndex);
                        myIndex = end;
                    }

                    default -> keepBackslash();
                }
            }
            record(mySource.length());
        }
    }

    private static boolean isHexDigit(char receiver) {
        return (receiver >= '0' && receiver <= '9') || (receiver >= 'a' && receiver <= 'f') || (receiver >= 'A' && receiver <= 'F');
    }

    /**
     * Whether {@code content} can be the contents of a raw string literal with the given {@code quote} without changing its value.
     * <p>
     * In a raw string, a backslash before a quote prevents the quote from terminating the literal, but both characters
     * remain part of the value. Hence, a quote that would terminate the literal cannot be represented, and neither can
     * a trailing backslash (which would escape the closing quote) or, in single-quoted literals, a line break.
     */
    public static boolean isRepresentableAsRaw(String content, StarlarkQuote quote) {
        if (quote.getQuote().isEmpty()) {
            return false;
        }
        char quoteChar = quote.getQuote().charAt(0);
        if (content.endsWith("\\") || content.endsWith(String.valueOf(quoteChar))) {
            return false;
        }
        if (!quote.isTriple() && (content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0)) {
            return false;
        }
        int index = 0;
        int consecutiveQuotes = 0;
        while (index < content.length()) {
            char c = content.charAt(index);
            if (c == '\\' && index + 1 < content.length() && content.charAt(index + 1) == '\\') {  // The first backslash escapes the second
                index += 2;
                consecutiveQuotes = 0;
                continue;
            }
            if (c == '\\' && index + 1 < content.length() && content.charAt(index + 1) == quoteChar) {  // The quote is escaped
                index += 2;
                consecutiveQuotes = 0;
                continue;
            }
            consecutiveQuotes = c == quoteChar ? consecutiveQuotes + 1 : 0;
            if (consecutiveQuotes == quote.getQuote().length()) {
                return false;
            }
            index++;
        }
        return true;
    }

    /**
     * Escapes {@code content} so that it can be placed between the given {@code quote}s of a non-raw string literal.
     */
    public static String escape(String content, StarlarkQuote quote) {
        @Nullable Character quoteChar = quote.getQuote().isEmpty() ? null : quote.getQuote().charAt(0);
        StringBuilder result = new StringBuilder(content.length());
        for (int index = 0; index < content.length(); index++) {
            char c = content.charAt(index);
            if (c == '\\') {
                result.append("\\\\");
            }
            else if (c == '\n' && !quote.isTriple()) {
                result.append("\\n");
            }
            else if (c == '\r' && !quote.isTriple()) {
                result.append("\\r");
            }
            else if (quoteChar != null && c == quoteChar && (!quote.isTriple() || closesTripleQuote(content, index, quoteChar))) {
                result.append('\\').append(c);
            }
            else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Inside a triple-quoted string, a quote character only needs escaping if it starts a run of three quotes
     * or if it is the last character (as it would then merge with the closing triple quote).
     */
    private static boolean closesTripleQuote(
        String content,
        int index,
        char quoteChar
    ) {
        return index == content.length() - 1 ||
            (index + 2 < content.length() && content.charAt(index + 1) == quoteChar && content.charAt(index + 2) == quoteChar);
    }
}
