package org.jetbrains.bazel.server.sync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.ExecUtils;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bsp.protocol.JvmToolchainInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class JvmToolchainQuery {
    private JvmToolchainQuery() {
    }

    public static CompletableFuture<JvmToolchainInfo> jvmToolchainQueryForTarget(
        BazelRunner bazelRunner,
        ProjectView projectView,
        Label target
    ) {
        String aqueryExpression = javacMnemonic(target);
        BazelCommand command =
            bazelRunner.buildBazelCommand(projectView, true, builder ->
                builder.aquery(aquery -> {
                    aquery.getOptions().addAll(List.of("--output=jsonproto", aqueryExpression));
                })
            );
        return bazelRunner
            .runBazelCommand(command, null, false)
            .waitAndGetResult()
            .thenApply(aqueryResult -> {
                if (aqueryResult.isNotSuccess()) {
                    throw new RuntimeException("Could not query target '" + target + "' for jvm toolchain info");
                }

                return parseJavaHomeAndToolchainPathFromAquery(aqueryResult.getStdout());
            });
    }

    public static String javacMnemonic(Label target) {
        return "mnemonic(\"Javac\", \"" + target + "\")";
    }

    /**
     * Extract java_home, toolchain_path, and jvm_opts from aquery result.
     * Uses -jar as delimiter: everything before -jar are JVM options, everything after are compiler options.
     */
    private static JvmToolchainInfo parseJavaHomeAndToolchainPathFromAquery(byte[] stdout) {
        Gson gson = new Gson();
        JsonObject jsonObject =
            gson.fromJson(new InputStreamReader(new ByteArrayInputStream(stdout), StandardCharsets.UTF_8), JsonObject.class);
        JsonArray actions = jsonObject.getAsJsonArray("actions");

        if (actions.isEmpty()) {
            throw new RuntimeException("No Javac actions found in aquery result");
        }

        JsonElement firstAction = actions.size() > 0 ? actions.get(0) : null;
        if (firstAction == null) {
            throw new RuntimeException("No Javac actions found in aquery result");
        }
        JsonObject javacAction = firstAction.getAsJsonObject();
        JsonArray arguments = javacAction.getAsJsonArray("arguments");

        String javaHome = "";
        String toolchainPath = "";
        List<String> jvmOpts = new ArrayList<>();

        // Find -jar position to use as delimiter
        int jarIndex = -1;
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i).getAsString().equals("-jar")) {
                jarIndex = i;
                break;
            }
        }

        if (jarIndex == -1) {
            throw new RuntimeException("No -jar argument found in javac command");
        }

        // Extract java_home from first argument
        if (arguments.size() > 0) {
            String javaExec = arguments.get(0).getAsString();
            String javaExecName = ExecUtils.calculateExecutableName("java");
            if (javaExec.endsWith("/bin/" + javaExecName) || javaExec.endsWith("/" + javaExecName)) {
                javaHome = javaExec.substring(0, javaExec.lastIndexOf("/bin/" + javaExecName));
            }
            else if (javaExec.contains("/bin/" + javaExecName)) {
                javaHome = javaExec.substring(0, javaExec.indexOf("/bin/" + javaExecName));
            }
        }

        // Extract toolchain_path from argument after -jar
        if (jarIndex + 1 < arguments.size()) {
            toolchainPath = arguments.get(jarIndex + 1).getAsString();
        }

        // Extract JVM options: everything between java executable and -jar
        for (int i = 1; i < jarIndex; i++) {
            String arg = arguments.get(i).getAsString();
            jvmOpts.add(arg);
        }

        return new JvmToolchainInfo(
            javaHome,
            toolchainPath,
            jvmOpts
        );
    }
}
