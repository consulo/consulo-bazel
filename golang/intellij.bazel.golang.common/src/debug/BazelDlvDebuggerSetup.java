package org.jetbrains.bazel.golang.debug;

import com.goide.sdk.GoSdkService;
import consulo.module.Module;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// consulo: upstream maps the debugger paths of a Bazel Go binary with BspDlvPositionConverter (a com.goide.dlv
// DlvPositionConverter, see BazelDlvPosConverterFactory), which resolves each path through GoLanguagePlugin.resolveRemoteToLocal /
// resolveLocalToRemote. Delve over DAP takes prefix rules instead, the `substitutePath` launch argument; these are the rules of
// that algorithm:
// - `GOROOT/...` is under the Go SDK,
// - `external/...` is under the output base,
// - any other relative path is under the workspace root,
// - an absolute path is kept.
// The rules are ordered: the Go SDK of rules_go is itself under the output base.
// TODO consulo: the remote prefixes `/build/work/<4 segments>/` and `/tmp/go-build-release/buildroot/<3 segments>/` of
//  GoLanguagePlugin.normalizeRemotePath have a variable segment, a substitutePath rule cannot express them
// TODO consulo: GoDlvNativeDebuggerProvider.createLaunchArguments (consulo-google-go) does not read the NativeDebuggerSetup of the
//  target, thus getSubstitutePath() is not passed to Delve yet; it needs a setup API for DAP path mappings
class BazelDlvDebuggerSetup implements NativeDebuggerSetup {
    // Delve takes the empty path as "any relative path" on the debugger side
    private static final String RELATIVE = "";

    private final List<Map<String, String>> mySubstitutePath;

    private BazelDlvDebuggerSetup(List<Map<String, String>> substitutePath) {
        mySubstitutePath = substitutePath;
    }

    static NativeDebuggerSetup create(Project project, @Nullable Module module) {
        WorkspaceSnapshot snapshot = WorkspaceHelpers.getWorkspaceSnapshotOrNull(project);
        if (snapshot == null) {
            return NativeDebuggerSetup.EMPTY;
        }
        BazelInfo bazelInfo = snapshot.bazelInfo();
        // consulo: GoSdkService.getSdk(module).homePath -> GoSdkService.getSdkHomePath(module), like BazelDlvPosConverterFactory
        String goRoot = GoSdkService.getInstance(project).getSdkHomePath(module);

        List<Map<String, String>> substitutePath = new ArrayList<>();
        if (goRoot != null && !goRoot.isEmpty()) {
            substitutePath.add(rule(Path.of(goRoot).toString(), "GOROOT"));
        }
        substitutePath.add(rule(bazelInfo.outputBase().resolve("external").toString(), "external"));
        substitutePath.add(rule(bazelInfo.workspaceRoot().toString(), RELATIVE));
        return new BazelDlvDebuggerSetup(List.copyOf(substitutePath));
    }

    /**
     * The {@code substitutePath} launch argument of Delve: {@code from} is the IDE path, {@code to} the path the binary records.
     */
    List<Map<String, String>> getSubstitutePath() {
        return mySubstitutePath;
    }

    private static Map<String, String> rule(String from, String to) {
        Map<String, String> rule = new LinkedHashMap<>();
        rule.put("from", from);
        rule.put("to", to);
        return rule;
    }
}
