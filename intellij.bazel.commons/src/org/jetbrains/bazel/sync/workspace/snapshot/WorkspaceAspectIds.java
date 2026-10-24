package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.util.interner.Interner;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: upstream @JvmInline value class
public final class WorkspaceAspectIds {
    private final List<String> myIds;

    private WorkspaceAspectIds(List<String> ids) {
        myIds = ids;
    }

    public List<String> getIds() {
        return myIds;
    }

    public static final WorkspaceAspectIds EMPTY = new WorkspaceAspectIds(List.of());

    private static final Interner<WorkspaceAspectIds> INTERNER = Interner.createWeakInterner();

    public static WorkspaceAspectIds of(List<String> aspectIds) {
        if (aspectIds.isEmpty() || aspectIds.stream().allMatch(String::isBlank)) {
            return EMPTY;
        }
        else {
            return INTERNER.intern(new WorkspaceAspectIds(aspectIds.stream().filter(it -> !it.isBlank()).toList()));
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof WorkspaceAspectIds that && myIds.equals(that.myIds);
    }

    @Override
    public int hashCode() {
        return myIds.hashCode();
    }

    @Override
    public String toString() {
        return "WorkspaceAspectIds(ids=" + myIds + ")";
    }
}
