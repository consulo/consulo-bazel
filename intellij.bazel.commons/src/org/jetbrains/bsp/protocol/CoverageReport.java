package org.jetbrains.bsp.protocol;

import java.nio.file.Path;

public record CoverageReport(TaskId taskId, Path coverageReport) {
}
