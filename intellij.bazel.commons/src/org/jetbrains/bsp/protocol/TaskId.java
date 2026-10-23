package org.jetbrains.bsp.protocol;

import org.jspecify.annotations.Nullable;

import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents hierarchical tasks.
 * Note: the task tree presentation in UI does not match the TaskId hierarchy 1:1,
 * however, respects the parent-child relationship.
 */
public record TaskId(TaskGroupId taskGroupId, String id, @Nullable TaskId parent) {
    public record TaskGroupId(String id) {
        public TaskId task(String taskId) {
            return new TaskId(this, taskId);
        }

        /**
         * Empty group, which explicitly does not reflect in UI
         */
        public static final TaskGroupId EMPTY = new TaskGroupId("");
    }

    public TaskId {
        if (parent != null && !parent.taskGroupId().equals(taskGroupId)) {
            throw new IllegalArgumentException("TaskId parent originId must be equal to originId");
        }
    }

    public TaskId(TaskGroupId taskGroupId, String id) {
        this(taskGroupId, id, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        TaskId id = this;
        while (id != null) {
            if (!sb.isEmpty()) {
                sb.insert(0, '/');
            }
            sb.insert(0, id.id());
            id = id.parent();
        }

        return taskGroupId.id() + ":" + sb.toString();
    }

    public TaskId subTask(String id) {
        return new TaskId(taskGroupId, id, this);
    }

    public TaskId uniqueSubTask(String prefix) {
        byte[] bytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(bytes);
        return subTask(prefix + "-" + HexFormat.of().formatHex(bytes));
    }
}
