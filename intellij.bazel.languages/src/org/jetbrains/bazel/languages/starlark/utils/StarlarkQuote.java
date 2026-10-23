package org.jetbrains.bazel.languages.starlark.utils;

import consulo.document.util.TextRange;

public enum StarlarkQuote {
    SINGLE("'"),
    DOUBLE("\""),
    TRIPLE_SINGLE("'''"),
    TRIPLE_DOUBLE("\"\"\""),
    UNQUOTED(""),
    ;

    private final String myQuote;

    StarlarkQuote(String quote) {
        myQuote = quote;
    }

    public String getQuote() {
        return myQuote;
    }

    public boolean isTriple() {
        return this == TRIPLE_SINGLE || this == TRIPLE_DOUBLE;
    }

    public TextRange rangeWithinQuotes(String string) {
        boolean hasClosingQuote = string.length() >= myQuote.length() * 2 &&
            string.endsWith(myQuote) &&
            countTrailingBackslashes(string.substring(0, string.length() - myQuote.length())) % 2 == 0;
        int endOffset = hasClosingQuote ? string.length() - myQuote.length() : string.length();
        return new TextRange(myQuote.length(), endOffset);
    }

    public String wrap(String toWrap) {
        return myQuote + toWrap + myQuote;
    }

    public String unwrap(String toUnwrap) {
        // Kotlin removeSurrounding(quote)
        if (toUnwrap.length() >= myQuote.length() * 2 && toUnwrap.startsWith(myQuote) && toUnwrap.endsWith(myQuote)) {
            return toUnwrap.substring(myQuote.length(), toUnwrap.length() - myQuote.length());
        }
        return toUnwrap;
    }

    public static StarlarkQuote ofString(String string) {
        if (string.startsWith(TRIPLE_SINGLE.myQuote)) {
            return TRIPLE_SINGLE;
        }
        else if (string.startsWith(TRIPLE_DOUBLE.myQuote)) {
            return TRIPLE_DOUBLE;
        }
        else if (string.startsWith(SINGLE.myQuote)) {
            return SINGLE;
        }
        else if (string.startsWith(DOUBLE.myQuote)) {
            return DOUBLE;
        }
        else {
            return UNQUOTED;
        }
    }

    // Kotlin takeLastWhile { it == '\\' }.length
    private static int countTrailingBackslashes(String string) {
        int count = 0;
        for (int i = string.length() - 1; i >= 0 && string.charAt(i) == '\\'; i--) {
            count++;
        }
        return count;
    }
}
