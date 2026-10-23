package org.jetbrains.bazel.commons;

public sealed interface ExcludableValue<T> permits ExcludableValue.Included, ExcludableValue.Excluded {
    T value();

    record Included<T>(T value) implements ExcludableValue<T> {
        @Override
        public String toString() {
            return value.toString();
        }
    }

    record Excluded<T>(T value) implements ExcludableValue<T> {
        @Override
        public String toString() {
            return "-" + value;
        }
    }

    default boolean isIncluded() {
        return this instanceof Included;
    }

    default boolean isExcluded() {
        return this instanceof Excluded;
    }

    static <T> ExcludableValue<T> included(T value) {
        return new Included<>(value);
    }

    static <T> ExcludableValue<T> excluded(T value) {
        return new Excluded<>(value);
    }
}
