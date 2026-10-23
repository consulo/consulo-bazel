package org.jetbrains.bazel.languages.projectview;

import org.jspecify.annotations.Nullable;

public class ProjectViewSectionKey<T extends @Nullable Object> {
    private final String myName;
    private final T myDefault;

    public ProjectViewSectionKey(String name, T defaultValue) {
        myName = name;
        myDefault = defaultValue;
    }

    public String getName() {
        return myName;
    }

    public T getDefault() {
        return myDefault;
    }
}
