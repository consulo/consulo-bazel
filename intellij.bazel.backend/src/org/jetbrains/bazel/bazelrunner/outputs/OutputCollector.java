package org.jetbrains.bazel.bazelrunner.outputs;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class OutputCollector {
    private final ByteArrayOutputStream myRaw = new ByteArrayOutputStream();

    public void append(int b) {
        myRaw.write(b);
    }

    // consulo: overload for the upstream default arguments
    public void append(byte[] bytes) {
        append(bytes, 0, bytes.length);
    }

    public void append(byte[] bytes, int offset, int length) {
        myRaw.write(bytes, offset, length);
    }

    public byte[] raw() {
        return myRaw.toByteArray();
    }

    // consulo: Kotlin String.lines() splits on \r\n, \n and \r and keeps the trailing empty line
    public List<String> lines() {
        return Arrays.asList(new String(raw(), StandardCharsets.UTF_8).split("\r\n|\n|\r", -1));
    }
}
