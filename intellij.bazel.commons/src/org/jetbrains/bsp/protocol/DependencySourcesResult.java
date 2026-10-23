package org.jetbrains.bsp.protocol;

import java.util.List;

record DependencySourcesResult(List<DependencySourcesItem> items) {
}
