package org.jetbrains.bazel.ui.widgets.tool.window.components;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// consulo: kotlin.text.Regex -> java.util.regex.Pattern
public final class QueryHighlighter {
    private QueryHighlighter() {
    }

    public static String highlight(String text, Pattern query) {
        // query.findAll(text).take(text.length).toList()
        List<MatchResult> matches = new ArrayList<>();
        Matcher matcher = query.matcher(text);
        while (matches.size() < text.length() && matcher.find()) {
            matches.add(matcher.toMatchResult());
        }
        if (!query.pattern().isEmpty() && !matches.isEmpty()) {
            return "<html>" + highlightAllOccurrences(text, matches) + "</html>";
        }
        else {
            return text;
        }
    }

    private static String highlightAllOccurrences(String text, List<MatchResult> query) {
        int lastIndex = 0;
        StringBuilder result = new StringBuilder();
        for (MatchResult match : query) {
            int matchStart = match.start();
            int matchEnd = match.end();
            result.append(text, lastIndex, matchStart);
            result.append("<b><u>");
            result.append(text, matchStart, matchEnd);
            result.append("</u></b>");
            lastIndex = matchEnd;
        }
        result.append(text.substring(lastIndex));
        return result.toString();
    }
}
