package org.jetbrains.bazel.server;

public enum BazelTestFileNames {
    LOG("test.log"),
    COVERAGE("test.lcov"),
    XML("test.xml");

    private final String myFilename;

    BazelTestFileNames(String filename) {
        myFilename = filename;
    }

    public String getFilename() {
        return myFilename;
    }

    public String getFilenameRegex() {
        return ".*/" + myFilename.replace("\\", "\\\\").replace(".", "\\.");
    }
}
