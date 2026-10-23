package org.jetbrains.bsp.protocol;

import java.util.List;

record NonModuleTargetsResult(List<BuildTarget> nonModuleTargets) {
}
