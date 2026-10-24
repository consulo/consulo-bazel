package org.jetbrains.bazel.bazelrunner.params;

import java.util.List;

import static org.jetbrains.bazel.commons.constants.Constants.NAME;
import static org.jetbrains.bazel.commons.constants.Constants.VERSION;

public final class BazelFlag {
    private BazelFlag() {
    }

    public static String runUnder(String command) {
        return arg("run_under", command);
    }

    public static String color(boolean enabled) {
        return yesNoArg("color", enabled);
    }

    public static String keepGoing() {
        return flag("keep_going");
    }

    public static String skipIncompatibleExplicitTargets() {
        return flag("skip_incompatible_explicit_targets");
    }

    public static String noRunValidations() {
        return flag("norun_validations");
    }

    public static String javaTestDebug() {
        return flag("java_debug");
    }

    public static String outputGroups(List<String> groups) {
        return arg("output_groups", String.join(",", groups));
    }

    public static String aspect(String name) {
        return arg("aspects", name);
    }

    public static String buildManualTests() {
        return flag("build_manual_tests");
    }

    // Use file:// uri scheme for output paths in the build events.
    public static String buildEventBinaryPathConversion(boolean enabled) {
        return arg(
            "build_event_binary_file_path_conversion",
            Boolean.toString(enabled)
        );
    }

    public static String curses(boolean enabled) {
        return yesNoArg("curses", enabled);
    }

    // consulo: overload for the upstream default argument
    public static String enableWorkspace() {
        return enableWorkspace(true);
    }

    public static String enableWorkspace(boolean enabled) {
        return arg("enable_workspace", Boolean.toString(enabled));
    }

    public static String testOutputAll() {
        return arg("test_output", "all");
    }

    public static String testOutputStreamed() {
        return arg("test_output", "streamed");
    }

    public static String device(String device) {
        return arg("device", device);
    }

    public static String start(String startType) {
        return arg("start", startType);
    }

    public static String adb(String adbPath) {
        return arg("adb", adbPath);
    }

    public static String testFilter(String filterExpression) {
        return arg("test_filter", filterExpression);
    }

    public static String toolTag() {
        return arg("tool_tag", NAME + ":" + VERSION);
    }

    public static String starlarkDebug() {
        return flag("experimental_skylark_debug");
    }

    public static String starlarkDebugPort(int port) {
        return arg("experimental_skylark_debug_server_port", Integer.toString(port));
    }

    public static String noBuild() {
        return flag("nobuild");
    }

    public static String combinedReportLcov() {
        return arg("combined_report", "lcov");
    }

    public static String instrumentationFilter(String filter) {
        return arg("instrumentation_filter", filter);
    }

    public static String checkVisibility(boolean enabled) {
        return arg("check_visibility", Boolean.toString(enabled));
    }

    public static String orderOutput(boolean enabled) {
        return yesNoArg("order_output", enabled);
    }

    public static String universeScope(String scope) {
        return arg("universe_scope", scope);
    }

    public static String consistentLabels(boolean enabled) {
        return arg("consistent_labels", Boolean.toString(enabled));
    }

    public static String buildEventBinaryFile(String file) {
        return arg("build_event_binary_file", file);
    }

    public static String buildEventBinaryFileWait() {
        return arg("build_event_binary_file_upload_mode", "wait_for_upload_complete");
    }

    /**
     * https://bazel.build/reference/command-line-reference#flag--target_pattern_file
     */
    public static String targetPatternFile(String file) {
        return arg("target_pattern_file", file);
    }

    public static String queryFile(String file) {
        return arg("query_file", file);
    }

    public static String remoteDownloadOutputsTopLevel() {
        return arg("remote_download_outputs", "toplevel");
    }

    public static String remoteDownloadRegex(String pattern) {
        return arg("remote_download_regex", pattern);
    }

    private static String yesNoArg(String name, boolean enable) {
        return arg(name, enable ? "yes" : "no");
    }

    private static String arg(String name, String value) {
        return String.format("--%s=%s", name, value);
    }

    private static String flag(String name) {
        return "--" + name;
    }

    public static final class OutputFormat {
        private OutputFormat() {
        }

        public static String label() {
            return outputFlag("label");
        }

        public static String json() {
            return outputFlag("json");
        }

        public static String xml() {
            return outputFlag("xml");
        }

        public static String proto() {
            return outputFlag("proto");
        }

        public static String streamed_proto() {
            return outputFlag("streamed_proto");
        }

        private static String outputFlag(String format) {
            return arg("output", format);
        }
    }
}
