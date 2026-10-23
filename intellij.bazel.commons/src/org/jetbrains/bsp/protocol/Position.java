package org.jetbrains.bsp.protocol;

public record Position(
    // 0-based
    int line,
    // 0-based
    int character
) {
    public static final Position NONE = new Position(-1, -1);

    // Converts 1-based numbers to 0-based
    public static Position fromHumanReadable(int line, int character) {
        return new Position(line - 1, character - 1);
    }
}
