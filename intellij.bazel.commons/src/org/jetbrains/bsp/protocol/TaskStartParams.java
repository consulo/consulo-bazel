package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

// consulo: not a record, `data` is mutable (`var`) upstream
public final class TaskStartParams {
    private final TaskId myTaskId;
    private final @Nullable Long myEventTime;
    private final @Nullable String myMessage;
    private @Nullable TaskStartData myData;

    public TaskStartParams(TaskId taskId, @Nullable Long eventTime, @Nullable String message, @Nullable TaskStartData data) {
        myTaskId = taskId;
        myEventTime = eventTime;
        myMessage = message;
        myData = data;
    }

    public TaskStartParams(TaskId taskId) {
        this(taskId, null, null, null);
    }

    public TaskId getTaskId() {
        return myTaskId;
    }

    public @Nullable Long getEventTime() {
        return myEventTime;
    }

    public @Nullable String getMessage() {
        return myMessage;
    }

    public @Nullable TaskStartData getData() {
        return myData;
    }

    public void setData(@Nullable TaskStartData data) {
        myData = data;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof TaskStartParams that
            && myTaskId.equals(that.myTaskId)
            && Objects.equals(myEventTime, that.myEventTime)
            && Objects.equals(myMessage, that.myMessage)
            && Objects.equals(myData, that.myData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myTaskId, myEventTime, myMessage, myData);
    }

    @Override
    public String toString() {
        return "TaskStartParams(taskId=" + myTaskId + ", eventTime=" + myEventTime + ", message=" + myMessage + ", data=" + myData + ")";
    }

    public sealed interface TaskStartData {
    }

    public record TestStart(String displayName, boolean isSuit, @Nullable String locationHint) implements TaskStartData {
        public TestStart(String displayName, boolean isSuit) {
            this(displayName, isSuit, null);
        }
    }

    public record TestTask(Label target) implements TaskStartData {
    }

    public record CompileTask(Label target) implements TaskStartData {
    }
}
