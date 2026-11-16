package org.jetbrains.bazel.languages.projectview;

import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Immutable representation of a ProjectView: a map of section keys to parsed values.
 */
public class ProjectView {
    private final Map<ProjectViewSectionKey<?>, Object> mySections;
    private final List<Import.Resolved> myImports;

    public ProjectView(
        Map<ProjectViewSectionKey<?>, Object> sections,
        List<Import.Resolved> imports
    ) {
        mySections = sections;
        myImports = imports;
    }

    public Map<ProjectViewSectionKey<?>, Object> getSections() {
        return mySections;
    }

    public List<Import.Resolved> getImports() {
        return myImports;
    }

    public boolean isEmpty() {
        return mySections.isEmpty() && myImports.isEmpty();
    }

    public <T extends @Nullable Object> T getSection(ProjectViewSectionKey<T> key) {
        Object value = mySections.get(key);
        if (value != null) {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        }
        return key.getDefault();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ProjectView that = (ProjectView) other;

        if (!mySections.equals(that.mySections)) {
            return false;
        }
        if (!myImports.equals(that.myImports)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mySections.hashCode();
        result = 31 * result + myImports.hashCode();
        return result;
    }

    public static final ProjectView EMPTY = new ProjectView(Map.of(), List.of());
}
