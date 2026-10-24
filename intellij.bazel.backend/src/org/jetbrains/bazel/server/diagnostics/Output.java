package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Output {
    private final List<String> myLines;
    private final Label myTargetLabel;
    private int myPointer = 0;

    Output(List<String> lines, Label targetLabel) {
        myLines = lines;
        myTargetLabel = targetLabel;
    }

    public Label getTargetLabel() {
        return myTargetLabel;
    }

    public boolean nonEmpty() {
        return myPointer < myLines.size();
    }

    public @Nullable String peek() {
        return myPointer < myLines.size() ? myLines.get(myPointer) : null;
    }

    public String take() {
        return myLines.get(myPointer++);
    }

    public List<String> peek(int limit) {
        return myLines.subList(myPointer, Math.min(myPointer + limit, myLines.size()));
    }

    public List<String> peekAll() {
        return myLines.subList(myPointer, myLines.size());
    }

    public List<String> take(int count) {
        List<String> result = peek(count);
        myPointer += count;
        return result;
    }

    public @Nullable String tryTake() {
        String result = peek();
        if (result != null) {
            take();
        }
        return result;
    }

    // consulo: the Kotlin MatchResult is a Matcher on which matches() succeeded
    public @Nullable Matcher tryTake(Pattern regex) {
        String line = peek();
        if (line == null) {
            return null;
        }
        Matcher matcher = regex.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        take();
        return matcher;
    }

    public String fullOutput() {
        return String.join(System.lineSeparator(), myLines);
    }
}
