package org.jetbrains.bsp.protocol;

import java.nio.file.Path;

public record CachedTestLog(TaskId taskId, Path testLog) {
}
