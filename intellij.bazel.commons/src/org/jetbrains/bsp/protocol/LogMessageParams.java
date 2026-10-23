package org.jetbrains.bsp.protocol;

public record LogMessageParams(
    TaskId task,
    MessageType type,
    String message
) {
}
