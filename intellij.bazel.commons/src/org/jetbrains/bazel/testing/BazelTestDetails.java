package org.jetbrains.bazel.testing;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record BazelTestDetails(
    String displayName,
    boolean isSuite,
    @Nullable String classname,
    @Nullable String file,
    @Nullable Integer oneBasedLine,
    List<String> parentSuiteNames
) {
    public static Builder testCase(String name) {
        return new Builder(name, false);
    }

    public static Builder testSuite(String name) {
        return new Builder(name, true);
    }

    public static class Builder {
        private final String myName;
        private final boolean myIsSuite;
        private @Nullable String myClassname = null;
        private @Nullable String myFile = null;
        private @Nullable Integer myOneBasedLine = null;
        private List<String> myParentSuiteNames = List.of();

        public Builder(String name, boolean isSuite) {
            myName = name;
            myIsSuite = isSuite;
        }

        public String getName() {
            return myName;
        }

        public boolean isSuite() {
            return myIsSuite;
        }

        public @Nullable String getClassname() {
            return myClassname;
        }

        public void setClassname(@Nullable String classname) {
            myClassname = classname;
        }

        public @Nullable String getFile() {
            return myFile;
        }

        public void setFile(@Nullable String file) {
            myFile = file;
        }

        public @Nullable Integer getOneBasedLine() {
            return myOneBasedLine;
        }

        public void setOneBasedLine(@Nullable Integer oneBasedLine) {
            myOneBasedLine = oneBasedLine;
        }

        public List<String> getParentSuiteNames() {
            return myParentSuiteNames;
        }

        public void setParentSuiteNames(List<String> parentSuiteNames) {
            myParentSuiteNames = parentSuiteNames;
        }

        public Builder withClassname(String classname) {
            myClassname = classname;
            return this;
        }

        public Builder withFileAndZeroBasedLine(@Nullable String file, @Nullable Integer line) {
            myFile = file;
            myOneBasedLine = line != null ? line + 1 : null;
            return this;
        }

        public Builder withParentSuites(List<String> parentSuiteNames) {
            myParentSuiteNames = parentSuiteNames;
            return this;
        }

        public Builder withParentSuite(String parentSuiteName) {
            myParentSuiteNames = List.of(parentSuiteName);
            return this;
        }

        public BazelTestDetails build() {
            return new BazelTestDetails(
                myName,
                myIsSuite,
                myClassname,
                myFile,
                myOneBasedLine,
                myParentSuiteNames
            );
        }
    }
}
