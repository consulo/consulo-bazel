package org.jetbrains.bazel.languages.starlark.bazel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides external Bazel rule signatures for Starlark code completion and documentation.
 * <p>
 * Reads {@code *.json} stardoc resources (generated offline by
 * {@code //plugins/bazel/tools/bazel_signatures/stardoc:generator}) and
 * deserializes the JSON into {@link BazelGlobalFunction} objects via Gson.
 * <p>
 * See the KDoc on the generator's {@code main} function for the full generation pipeline and
 * usage instructions.
 *
 * <h3>Last regeneration commands</h3>
 * <pre>{@code
 * # rules_kotlin (v2.4.0)
 * bazel run //plugins/bazel/tools/bazel_signatures/stardoc:generator -- \
 *     --repo-url https://github.com/bazelbuild/rules_kotlin.git --ref v2.4.0 \
 *     --bzl-file //kotlin:core.bzl --dep //kotlin/internal \
 *     --bzl-file //kotlin:jvm.bzl --dep //kotlin/internal \
 *     --bzl-file //kotlin:lint.bzl --dep //kotlin/internal --dep //kotlin/internal/lint \
 *     --bzl-file //kotlin:repositories.doc.bzl --dep //kotlin/internal \
 *     --output rules_kotlin
 *
 * # rules_go (v0.61.1)
 * bazel run //plugins/bazel/tools/bazel_signatures/stardoc:generator -- \
 *     --repo-url https://github.com/bazelbuild/rules_go.git --ref v0.61.1 \
 *     --bzl-file //go/private/rules:library.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:binary.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:test.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:source.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:cross.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:transition.bzl --dep //go:def \
 *     --bzl-file //go/private/tools:path.bzl --dep //go:def \
 *     --output rules_go
 * }</pre>
 */
@ExtensionImpl
public class BazelStardocFunctionProvider implements StarlarkGlobalFunctionProvider {

    private final List<String> myStardocToLoad = List.of(
        "/bazelSignatures/rules_kotlin@v2.4.0.json",
        "/bazelSignatures/rules_go@v0.61.1.json"
    );

    // consulo: Kotlin `by lazy` is synchronized, atomicNotNull keeps that
    private final LazyValue<List<BazelGlobalFunction>> myCachedFunctions = LazyValue.atomicNotNull(this::loadStardocFunctions);

    @Override
    public List<BazelGlobalFunction> functions(Project project) {
        return myCachedFunctions.get();
    }

    private List<BazelGlobalFunction> loadStardocFunctions() {
        Class<?> cls = getClass();
        Gson gson = new Gson();
        List<BazelGlobalFunction> result = new ArrayList<>();
        for (String path : myStardocToLoad) {
            InputStream stream = cls.getResourceAsStream(path);
            if (stream == null) {
                continue;
            }
            String json;
            try (stream) {
                json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            List<BazelGlobalFunction> functions = gson.fromJson(json, myBazelGlobalFunctionListTypeToken);
            if (functions != null) {
                result.addAll(functions);
            }
        }
        return result;
    }

    private final TypeToken<List<BazelGlobalFunction>> myBazelGlobalFunctionListTypeToken = new TypeToken<List<BazelGlobalFunction>>() {
    };
}
