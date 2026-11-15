package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

// consulo: holder of the upstream top-level declarations of StarlarkStaticValueUtils.kt
// (StarlarkStaticValueKind and StarlarkStaticValueAnalyzer are nested)
public final class StarlarkStaticValueUtils {
    private StarlarkStaticValueUtils() {
    }

    public enum StarlarkStaticValueKind {
        LIST(
            "list",
            /* readOnlyMethods = */ Set.of("index"),
            /* mutatingMethods = */ Set.of("append", "clear", "extend", "insert", "pop", "remove")
        ),

        DICT(
            "dict",
            /* readOnlyMethods = */ Set.of("get", "items", "keys", "values"),
            /* mutatingMethods = */ Set.of("clear", "pop", "popitem", "setdefault", "update")
        ),

        TUPLE("tuple"),

        STRING(
            "string",
            /* readOnlyMethods = */ Set.of(
                "capitalize",
                "count",
                "elems",
                "endswith",
                "find",
                "format",
                "index",
                "isalnum",
                "isalpha",
                "isdigit",
                "islower",
                "isspace",
                "istitle",
                "isupper",
                "join",
                "lower",
                "lstrip",
                "partition",
                "removeprefix",
                "removesuffix",
                "replace",
                "rfind",
                "rindex",
                "rpartition",
                "rsplit",
                "rstrip",
                "split",
                "splitlines",
                "startswith",
                "strip",
                "title",
                "upper"
            ),
            /* mutatingMethods = */ Set.of()
        ),

        INT("int"),
        FLOAT("float"),
        BOOL("bool"),
        NONE("NoneType");

        private final String myTypeName;
        private final Set<String> myReadOnlyMethods;
        private final Set<String> myMutatingMethods;

        // consulo: upstream default arguments (readOnlyMethods = emptySet(), mutatingMethods = emptySet())
        StarlarkStaticValueKind(String typeName) {
            this(typeName, Set.of(), Set.of());
        }

        StarlarkStaticValueKind(String typeName, Set<String> readOnlyMethods, Set<String> mutatingMethods) {
            myTypeName = typeName;
            myReadOnlyMethods = readOnlyMethods;
            myMutatingMethods = mutatingMethods;
        }

        public String getTypeName() {
            return myTypeName;
        }

        public boolean isMutatedBy(String methodName) {
            return myMutatingMethods.contains(methodName);
        }

        public boolean hasMethod(String methodName) {
            return myReadOnlyMethods.contains(methodName) || myMutatingMethods.contains(methodName);
        }

        public static final Set<String> KNOWN_MUTATING_METHODS = knownMutatingMethods();

        private static Set<String> knownMutatingMethods() {
            Set<String> result = new LinkedHashSet<>();
            for (StarlarkStaticValueKind it : values()) {
                result.addAll(it.myMutatingMethods);
            }
            return result;
        }
    }

    public static final class StarlarkStaticValueAnalyzer {
        private StarlarkStaticValueAnalyzer() {
        }

        public static @Nullable StarlarkStaticValueKind valueKind(PsiElement element) {
            if (element instanceof StarlarkListLiteralExpression) {
                return StarlarkStaticValueKind.LIST;
            }
            else if (element instanceof StarlarkDictLiteralExpression) {
                return StarlarkStaticValueKind.DICT;
            }
            else if (element instanceof StarlarkTupleExpression) {
                return StarlarkStaticValueKind.TUPLE;
            }
            else if (element instanceof StarlarkStringLiteralExpression) {
                return StarlarkStaticValueKind.STRING;
            }
            else if (element instanceof StarlarkIntegerLiteralExpression) {
                return StarlarkStaticValueKind.INT;
            }
            else if (element instanceof StarlarkFloatLiteralExpression) {
                return StarlarkStaticValueKind.FLOAT;
            }
            else if (element instanceof StarlarkTrueLiteralExpression || element instanceof StarlarkFalseLiteralExpression) {
                return StarlarkStaticValueKind.BOOL;
            }
            else if (element instanceof StarlarkNoneLiteralExpression) {
                return StarlarkStaticValueKind.NONE;
            }
            else if (element instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
                return parenthesizedValueKind(parenthesizedExpression);
            }
            else {
                return null;
            }
        }

        private static @Nullable StarlarkStaticValueKind parenthesizedValueKind(StarlarkParenthesizedExpression expression) {
            StarlarkTupleExpression tuple = expression.getTuple();
            if (tuple != null) {
                return valueKind(tuple);
            }
            for (PsiElement child : expression.getChildren()) {
                StarlarkStaticValueKind kind = valueKind(child);
                if (kind != null) {
                    return kind;
                }
            }
            return null;
        }
    }
}
