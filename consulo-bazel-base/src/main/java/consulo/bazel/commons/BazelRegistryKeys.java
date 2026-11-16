/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.bazel.commons;

import consulo.application.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry keys of the Bazel plugin with their default values.
 * <p>
 * Upstream declares them as {@code <registryKey>} in {@code intellij.bazel.commons.xml}. Consulo's registry cannot be
 * extended by plugins, so the defaults live here; a value can still be overridden with {@code -D<key>=<value>}.
 * Upstream {@code Registry.is(key)} / {@code Registry.intValue(key)} / {@code Registry.stringValue(key)} calls become
 * {@code BazelRegistryKeys.is(key)} / {@code intValue(key)} / {@code stringValue(key)}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@SuppressWarnings("deprecation")
public final class BazelRegistryKeys {
    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        // Build project on initial project sync
        DEFAULTS.put("bazel.build.project.on.sync", "false");
        // On sync, also request the SYNC output group which contains only source files in the sense of bazel.
        DEFAULTS.put("bazel.sync.request.sources", "false");
        // Shorten module/library names by hashing the first parts and retaining the others
        DEFAULTS.put("bsp.shorten.module.library.names", "true");
        // Execute second phase of the phased sync. `bsp.use.phased.sync` has to be enabled to use this flag. It should be used only for testing purposes.
        DEFAULTS.put("bsp.execute.second.phase.on.sync", "true");
        // Add dependencies to so-called "dummy" modules to make Java resolve work correctly. Set to either true, false, or leave empty for the default value.
        DEFAULTS.put("bsp.add.dummy.module.dependencies", "");
        // Exclude class files inside jars (and source files inside source jars) if they match up with a source file in the current project.
        DEFAULTS.put("bsp.exclude.compiled.source.code.inside.jars", "true");
        // Enables a fast compile mechanism for Java by delegating directly to the Bazel builder
        DEFAULTS.put("bazel.enable.jvm.fastbuild", "false");
        // Maximum number of attempts to connect with the Starlark debugger socket
        DEFAULTS.put("bazel.starlark.debug.socket.attempts", "25");
        // Interval (in milliseconds) between attempts to connect with the Starlark debugger socket
        DEFAULTS.put("bazel.starlark.debug.socket.interval", "400");
        // How many levels to go down when scanning the project for Bazel symlinks to exclude
        DEFAULTS.put("bazel.symlink.scan.max.depth", "2");
        // Force Bazel to shut down before each shard build to prevent Bazel memory leak issue
        DEFAULTS.put("bazel.shutdown.before.shard.build", "true");
        // Add the whole directory to IntelliJ project model instead of individual files when it's possible
        DEFAULTS.put("bazel.merge.source.roots", "true");
        // Whether the Bazel plugin should import automatically even if .idea or another project model (e.g., Gradle) exists already
        DEFAULTS.put("bazel.project.auto.open.if.present", "false");
        // If enabled, sets the default value of "Run with Bazel" to 'true' for new run configurations. This can be overridden with a project view flag.
        DEFAULTS.put("bazel.run.config.run.with.bazel", "true");
        // If true, IDEA will run Bazel commands in a pseudo-terminal, providing more rich formatting and progress reporting
        DEFAULTS.put("bazel.use.pty", "true");
        // If true, results of Bazel invocations (possibly long and unreadable) will be shown in the build console. Does not affect log messages like loading information, warnings and errors.
        DEFAULTS.put("bazel.show.stdout", "false");
        // If true, IDEA will terminate bazel client process tree if the bazel process was cancelled
        DEFAULTS.put("bazel.kill.tree.on.cancel", "true");
        // If true, IDEA will terminate bazel server daemon if the bazel process was cancelled
        DEFAULTS.put("bazel.kill.server.on.cancel", "false");
        // If true, log bazel process output to bazel.log file
        DEFAULTS.put("bazel.enable.log", "false");
        // If true, the Bazel command in the Build tool window and in bazel.log masks passwords, tokens and other secrets. If false, these logs show secrets in plain text. Turn it off only to debug.
        DEFAULTS.put("bazel.log.mask.secrets", "true");
        // Enable running non-binary bazel targets using synthetically generated targets
        DEFAULTS.put("bazel.run.synthetic.enable", "false");
        // Disable bazel visibility checks when running syhthetic binary targets
        DEFAULTS.put("bazel.run.synthetic.disable.visibility.check", "false");
        // Create hard links to Bazel output files (e.g., jars) during sync to protect against Bazel garbage-collecting files from disk with Remote Build Execution or when running bazel commands from terminal
        DEFAULTS.put("bazel.hard.link.output.files", "true");
        // Allow Bazel query invocation when processing file events
        DEFAULTS.put("bazel.query.on.file.events", "false");
        // Highlight unsynced source files
        DEFAULTS.put("bazel.highlight.unsynced.source.files", "true");
        // Highlight Bazel ignored source files
        DEFAULTS.put("bazel.highlight.ignored.source.files", "true");
    }

    private BazelRegistryKeys() {
    }

    public static boolean is(String key) {
        return Registry.get(key).asBoolean(Boolean.parseBoolean(defaultValue(key)));
    }

    public static int intValue(String key) {
        return Registry.get(key).asInteger(Integer.parseInt(defaultValue(key)));
    }

    public static String stringValue(String key) {
        String value = Registry.get(key).asString();
        return value.isEmpty() ? defaultValue(key) : value;
    }

    private static String defaultValue(String key) {
        String value = DEFAULTS.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown Bazel registry key: " + key);
        }
        return value;
    }
}
