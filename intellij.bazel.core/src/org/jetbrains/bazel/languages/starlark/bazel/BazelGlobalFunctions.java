package org.jetbrains.bazel.languages.starlark.bazel;

import consulo.application.Application;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BazelGlobalFunctions {
    // consulo: upstream public top-level enum of BazelGlobalFunctions.kt is nested here
    public enum Environment {
        BZL,
        BUILD,
        MODULE,
        REPO,
        VENDOR,
    }

    // consulo: upstream public top-level data class of BazelGlobalFunctions.kt is nested here
    public record BazelGlobalFunctionParameter(
        String name,
        @Nullable String doc,
        @Nullable String defaultValue,
        boolean named,
        boolean positional,
        boolean required
    ) {
    }

    public static boolean isKwArgs(BazelGlobalFunctionParameter receiver) {
        return receiver.name().startsWith("**");
    }

    public static boolean isVarArgs(BazelGlobalFunctionParameter receiver) {
        return receiver.name().startsWith("*");
    }

    // consulo: upstream public top-level data class of BazelGlobalFunctions.kt is nested here
    public record BazelGlobalFunction(
        String name,
        @Nullable String doc,
        List<Environment> environment,
        List<BazelGlobalFunctionParameter> params,
        @Nullable String returnType
    ) {
        // consulo: overload for the upstream default argument (returnType = null)
        public BazelGlobalFunction(
            String name,
            @Nullable String doc,
            List<Environment> environment,
            List<BazelGlobalFunctionParameter> params
        ) {
            this(name, doc, environment, params, null);
        }
    }

    // consulo: the upstream interface StarlarkGlobalFunctionProvider of BazelGlobalFunctions.kt is an extension point,
    // it has its own file (public extension API used by language integrations)

    // These properties read from the extension point dynamically to support test isolation.
    // Extension points can be masked/replaced during tests, so caching the initial value
    // would cause subsequent tests to see stale data.
    public static Map<String, BazelGlobalFunction> globalFunctions(Project project) {
        // consulo: upstream mapExtensionSafe, collectMapped() runs every extension safely
        List<List<BazelGlobalFunction>> functions = Application.get()
            .getExtensionPoint(StarlarkGlobalFunctionProvider.class)
            .collectMapped(it -> it.functions(project));
        Map<String, BazelGlobalFunction> result = new LinkedHashMap<>();
        for (List<BazelGlobalFunction> list : functions) {
            for (BazelGlobalFunction function : list) {
                result.put(function.name(), function);
            }
        }
        return result;
    }

    public static Map<String, BazelGlobalFunction> buildGlobalFunctions(Project project) {
        return globalFunctions(project).entrySet()
            .stream()
            .filter(it -> it.getValue().environment().contains(Environment.BUILD))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public static Map<String, BazelGlobalFunction> moduleGlobalFunctions(Project project) {
        return globalFunctions(project).entrySet()
            .stream()
            .filter(it -> it.getValue().environment().contains(Environment.MODULE))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public static Map<String, BazelGlobalFunction> extensionGlobalFunctions(Project project) {
        return globalFunctions(project).entrySet()
            .stream()
            .filter(it -> it.getValue().environment().contains(Environment.BZL))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public static Map<String, BazelGlobalFunction> starlarkGlobalFunctions(Project project) {
        return globalFunctions(project).entrySet()
            .stream()
            .filter(it -> it.getValue().environment().containsAll(List.of(Environment.values())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public static @Nullable BazelGlobalFunction getFunctionByName(String name, Project project) {
        return globalFunctions(project).get(name);
    }
}
