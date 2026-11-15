package org.jetbrains.bazel.languages.starlark.bazel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.SemVer;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Provides Bazel builtin function signatures for Starlark code completion and documentation.
 * <p>
 * Reads {@code builtins@{version}.json} (generated offline by
 * {@code //plugins/bazel/tools/bazel_signatures/builtins:generator}) and
 * deserializes the JSON into {@link BazelGlobalFunction} objects via Gson.
 * <p>
 * Multiple versions are shipped. The provider selects the closest version that does not
 * exceed the project's Bazel version, obtained from the workspace snapshot of the last sync.
 * Before the first sync, falls back to {@code .bazelversion}; if absent, uses the latest available signatures.
 * <p>
 * See the KDoc on the generator's {@code main} function for the full generation pipeline and
 * usage instructions.
 *
 * <h3>Updating</h3>
 * Run given command with wanted version
 * <pre>{@code
 * # Last generated version: 9.1.1
 * bazel run //plugins/bazel/tools/bazel_signatures/builtins:generator -- --version $version
 * }</pre>
 * If the version introduces any changes in the doc, comparing with the last generated version, the new JSON file will be generated.
 * Update {@link #BUILTIN_AVAILABLE_VERSIONS} list, when new JSON is added.
 */
@ExtensionImpl
public class BazelBuiltinFunctionProvider implements StarlarkGlobalFunctionProvider {

    private volatile @Nullable Pair<SemVer, List<BazelGlobalFunction>> myCached = null;

    @Override
    public List<BazelGlobalFunction> functions(Project project) {
        // Only a synced snapshot has a real release. Before the first sync, the fallbacks apply.
        WorkspaceSnapshot snapshot = project.getInstance(WorkspaceSnapshotService.class).getSnapshot().get();
        BazelRelease syncedRelease = snapshot.syncConfigs().stream().anyMatch(it -> it instanceof CommonWorkspaceSyncConfig)
            ? snapshot.bazelInfo().release()
            : null;
        BazelRelease release = orFromBazelVersionFileIn(syncedRelease, project);
        SemVer currentVersion = release == null ? null : BazelInfo.toSemVer(release);
        SemVer selectedVersion;
        if (currentVersion == null) {
            // version not detected, use latest
            selectedVersion = BUILTIN_AVAILABLE_VERSIONS.isEmpty()
                ? null
                : BUILTIN_AVAILABLE_VERSIONS.get(BUILTIN_AVAILABLE_VERSIONS.size() - 1);
        }
        else {
            selectedVersion = null;
            for (int i = BUILTIN_AVAILABLE_VERSIONS.size() - 1; i >= 0; i--) {
                SemVer it = BUILTIN_AVAILABLE_VERSIONS.get(i);
                if (it.compareTo(currentVersion) <= 0) {
                    selectedVersion = it;
                    break;
                }
            }
            if (selectedVersion == null) {
                // version too old, use earliest
                selectedVersion = BUILTIN_AVAILABLE_VERSIONS.isEmpty() ? null : BUILTIN_AVAILABLE_VERSIONS.get(0);
            }
        }
        return selectedVersion != null ? loadBuiltinFunctions(selectedVersion) : List.of();
    }

    private List<BazelGlobalFunction> loadBuiltinFunctions(SemVer version) {
        synchronized (this) {
            Pair<SemVer, List<BazelGlobalFunction>> cached = myCached;
            if (cached != null && cached.getFirst().equals(version)) {
                return cached.getSecond();
            }
            String path = "/bazelSignatures/builtins/builtins@" + version + ".json";
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                return List.of();
            }
            List<BazelGlobalFunction> functions;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                functions = new Gson().fromJson(reader, myBazelGlobalFunctionListTypeToken);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            myCached = Pair.create(version, functions);
            return functions;
        }
    }

    private final TypeToken<List<BazelGlobalFunction>> myBazelGlobalFunctionListTypeToken = new TypeToken<List<BazelGlobalFunction>>() {
    };

    private static @Nullable BazelRelease orFromBazelVersionFileIn(
        @Nullable BazelRelease receiver,
        Project project
    ) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        Path workspacePath = Path.of(basePath);
        return BazelInfo.orFromBazelVersionFile(receiver, workspacePath);
    }

    private static final List<SemVer> BUILTIN_AVAILABLE_VERSIONS = List.of(
        semVer(7, 5),
        semVer(7, 6),
        semVer(7, 7),
        semVer(8),
        semVer(8, 1),
        semVer(8, 2),
        semVer(8, 3),
        semVer(8, 4),
        semVer(8, 4, 1),
        semVer(8, 5),
        semVer(8, 6),
        semVer(9),
        semVer(9, 0, 1),
        semVer(9, 1)
    );

    // consulo: overloads for the upstream default arguments (minor = 0, patch = 0)
    private static SemVer semVer(int major) {
        return semVer(major, 0, 0);
    }

    private static SemVer semVer(int major, int minor) {
        return semVer(major, minor, 0);
    }

    private static SemVer semVer(
        int major,
        int minor,
        int patch
    ) {
        return new SemVer(major + "." + minor + "." + patch, major, minor, patch);
    }
}
