package org.jetbrains.bazel.bazelrunner;

import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// See https://bazel.build/reference/command-line-reference#commands
public abstract class BazelCommand {
    // consulo: upstream top-level interfaces and the descriptor class of BazelCommand.kt are nested here

    public interface HasProgramArguments {
        // Will be forwarded directly to `bazel run <target> -- <arguments>` or set using `--test_arg=<arg>` for `bazel test`
        List<String> getProgramArguments();
    }

    public interface HasAdditionalBazelOptions {
        // Will be forwarded directly to `bazel <command> <options>`
        List<String> getAdditionalBazelOptions();
    }

    public interface HasEnvironment {
        // In case of `bazel test`, it will be set using `--test_env=<name=value>`
        // In case of `bazel build`, it will be set using `--action_env=<name=value>`
        // `bazel run` needs to be set up inside the [org.jetbrains.bazel.bazelrunner.BazelRunner]
        Map<String, String> getEnvironment();

        // In case of `bazel test, it will be set using `--test_env=<name>`
        // In case of `bazel build`, it will be set using `--action_env=<name>`
        // In case of `bazel run` the environment is always inherited
        // TODO: this is not used in the current implementation
        List<String> getInheritedEnvironment();
    }

    public interface HasMultipleTargets {
        // Will be added as `bazel <command> -- target1 target2 ...`
        List<Label> getTargets();

        // Will be added as `bazel <command> -- <targets> -target1 -target2 ...`
        List<Label> getExcludedTargets();

        default void addTargetsFromExcludableList(List<ExcludableValue<Label>> targets) {
            targets.forEach(excludableTarget -> {
                if (excludableTarget.isIncluded()) {
                    this.getTargets().add(excludableTarget.value());
                }
                else {
                    this.getExcludedTargets().add(excludableTarget.value());
                }
            });
        }

        // no need to quote targets here - they are passed as separate strings to the command builder
        default List<String> targetCommandLine() {
            List<String> commandLine = new ArrayList<>(List.of("--"));
            commandLine.addAll(getTargets().stream().map(Label::toString).toList());
            commandLine.addAll(getExcludedTargets().stream().map(it -> "-" + it).toList());
            return commandLine;
        }
    }

    public interface HasSingleTarget {
        Label getTarget();
    }

    /**
     * Immutable in contrast to {@link BazelCommand}
     */
    public record BazelCommandExecutionDescriptor(
        List<String> command,
        boolean enablePty,
        Map<String, String> environment,
        boolean alwaysShowStdout,
        Runnable finishCallback
    ) {
        // consulo: overload for the upstream default arguments
        public BazelCommandExecutionDescriptor(List<String> command, boolean enablePty) {
            this(command, enablePty, Map.of(), false, () -> {
            });
        }
    }

    private final String myBazelBinary;

    // See https://bazel.build/reference/command-line-reference#startup-options
    private final List<String> myStartupOptions = new ArrayList<>();

    // See https://bazel.build/reference/command-line-reference#options-common-to-all-commands and command-specific options
    private final List<String> myOptions = new ArrayList<>(List.of(BazelFlag.toolTag()));

    private boolean myEnablePty = false;

    public BazelCommand(String bazelBinary) {
        myBazelBinary = bazelBinary;
    }

    public String getBazelBinary() {
        return myBazelBinary;
    }

    public List<String> getStartupOptions() {
        return myStartupOptions;
    }

    public List<String> getOptions() {
        return myOptions;
    }

    public boolean getEnablePty() {
        return myEnablePty;
    }

    public void setEnablePty(boolean enablePty) {
        myEnablePty = enablePty;
    }

    public abstract BazelCommandExecutionDescriptor buildExecutionDescriptor();

    // See https://bazel.build/reference/command-line-reference#flag--build_event_binary_file
    public void useBes(Path besOutputFile) {
        myOptions.addAll(
            List.of(
                BazelFlag.buildEventBinaryFile(besOutputFile.toAbsolutePath().toString()),
                BazelFlag.buildEventBinaryFileWait(),
                "--bes_outerr_buffer_size=10"
                // No --build_event_publish_all_actions to save Bazel memory: https://github.com/bazelbuild/bazel/issues/21540#issuecomment-3505368753
            )
        );
    }

    public static class Run extends BazelCommand
        implements HasProgramArguments, HasEnvironment, HasSingleTarget, HasAdditionalBazelOptions {
        private final Label myTarget;
        private final List<String> myProgramArguments = new ArrayList<>();
        private final Map<String, String> myEnvironment = new LinkedHashMap<>();
        private final List<String> myInheritedEnvironment = new ArrayList<>();
        private final List<String> myAdditionalBazelOptions = new ArrayList<>();

        public Run(String bazelBinary, Label target) {
            super(bazelBinary);
            myTarget = target;
        }

        @Override
        public Label getTarget() {
            return myTarget;
        }

        @Override
        public List<String> getProgramArguments() {
            return myProgramArguments;
        }

        @Override
        public Map<String, String> getEnvironment() {
            return myEnvironment;
        }

        @Override
        public List<String> getInheritedEnvironment() {
            return myInheritedEnvironment;
        }

        @Override
        public List<String> getAdditionalBazelOptions() {
            return myAdditionalBazelOptions;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>();

            commandLine.add(getBazelBinary());
            commandLine.addAll(getStartupOptions());
            commandLine.add("run");
            commandLine.addAll(getOptions());
            commandLine.addAll(myAdditionalBazelOptions);
            commandLine.add(myTarget.toString());

            if (!myProgramArguments.isEmpty()) {
                commandLine.add("--");
                commandLine.addAll(myProgramArguments);
            }

            // Pass environment variables here to be set inside org.jetbrains.bazel.bazelrunner.BazelRunner
            // Run needs to be handled separately because the resulting process is not run in the sandbox
            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty(), myEnvironment, true, () -> {
            });
        }
    }

    public static class Build extends BazelCommand
        implements HasEnvironment, HasMultipleTargets, HasAdditionalBazelOptions {
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();
        private final Map<String, String> myEnvironment = new LinkedHashMap<>();
        private final List<String> myInheritedEnvironment = new ArrayList<>();
        private final List<String> myAdditionalBazelOptions = new ArrayList<>();

        public Build(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public Map<String, String> getEnvironment() {
            return myEnvironment;
        }

        @Override
        public List<String> getInheritedEnvironment() {
            return myInheritedEnvironment;
        }

        @Override
        public List<String> getAdditionalBazelOptions() {
            return myAdditionalBazelOptions;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("build");
            commandLine.addAll(getOptions());
            commandLine.addAll(myAdditionalBazelOptions);
            commandLine.addAll(myEnvironment.entrySet().stream().map(e -> "--action_env=" + e.getKey() + "=" + e.getValue()).toList());
            commandLine.addAll(myInheritedEnvironment.stream().map(it -> "--action_env=" + it).toList());
            Path targetPatternFile = prepareTargetPatternFile();
            commandLine.add(BazelFlag.targetPatternFile(targetPatternFile.toString()));

            return new BazelCommandExecutionDescriptor(
                commandLine,
                getEnablePty(),
                Map.of(),
                false,
                () -> deleteBazelCommandFile(TARGET_PATTERN_FILE, targetPatternFile)
            );
        }

        public Path prepareTargetPatternFile() {
            return createBazelCommandFile(
                TARGET_PATTERN_FILE,
                Stream.concat(myTargets.stream().map(Label::toString), myExcludedTargets.stream().map(it -> "-" + it)).toList()
            );
        }
    }

    public static class Test extends BazelCommand
        implements HasEnvironment, HasMultipleTargets, HasProgramArguments, HasAdditionalBazelOptions {
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();
        private final Map<String, String> myEnvironment = new LinkedHashMap<>();
        private final List<String> myInheritedEnvironment = new ArrayList<>();
        private final List<String> myProgramArguments = new ArrayList<>();
        private final List<String> myAdditionalBazelOptions = new ArrayList<>();

        public Test(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public Map<String, String> getEnvironment() {
            return myEnvironment;
        }

        @Override
        public List<String> getInheritedEnvironment() {
            return myInheritedEnvironment;
        }

        @Override
        public List<String> getProgramArguments() {
            return myProgramArguments;
        }

        @Override
        public List<String> getAdditionalBazelOptions() {
            return myAdditionalBazelOptions;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("test");
            commandLine.addAll(getOptions());
            commandLine.addAll(myAdditionalBazelOptions);
            commandLine.addAll(myEnvironment.entrySet().stream().map(e -> "--test_env=" + e.getKey() + "=" + e.getValue()).toList());
            commandLine.addAll(myInheritedEnvironment.stream().map(it -> "--test_env=" + it).toList());
            commandLine.addAll(myProgramArguments.stream().map(it -> "--test_arg=" + it).toList());
            commandLine.addAll(targetCommandLine());

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty(), Map.of(), true, () -> {
            });
        }
    }

    public static class Coverage extends BazelCommand
        implements HasEnvironment, HasMultipleTargets, HasProgramArguments, HasAdditionalBazelOptions {
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();
        private final Map<String, String> myEnvironment = new LinkedHashMap<>();
        private final List<String> myInheritedEnvironment = new ArrayList<>();
        private final List<String> myProgramArguments = new ArrayList<>();
        private final List<String> myAdditionalBazelOptions = new ArrayList<>();

        public Coverage(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public Map<String, String> getEnvironment() {
            return myEnvironment;
        }

        @Override
        public List<String> getInheritedEnvironment() {
            return myInheritedEnvironment;
        }

        @Override
        public List<String> getProgramArguments() {
            return myProgramArguments;
        }

        @Override
        public List<String> getAdditionalBazelOptions() {
            return myAdditionalBazelOptions;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("coverage");
            commandLine.addAll(getOptions());
            commandLine.addAll(myEnvironment.entrySet().stream().map(e -> "--test_env=" + e.getKey() + "=" + e.getValue()).toList());
            commandLine.addAll(myAdditionalBazelOptions);
            commandLine.addAll(myInheritedEnvironment.stream().map(it -> "--test_env=" + it).toList());
            commandLine.addAll(myProgramArguments.stream().map(it -> "--test_arg=" + it).toList());
            commandLine.addAll(targetCommandLine());

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty(), Map.of(), true, () -> {
            });
        }
    }

    // TODO: perhaps it's possible to install multiple targets at once?
    public static class MobileInstall extends BazelCommand implements HasSingleTarget {
        private final Label myTarget;

        public MobileInstall(String bazelBinary, Label target) {
            super(bazelBinary);
            myTarget = target;
        }

        @Override
        public Label getTarget() {
            return myTarget;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("mobile-install");
            commandLine.addAll(getOptions());
            commandLine.add(myTarget.toString());

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }
    }

    public static class Query extends BazelCommand implements HasMultipleTargets {
        private final boolean myAllowManualTargetsSync;
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();

        public Query(String bazelBinary, boolean allowManualTargetsSync) {
            super(bazelBinary);
            myAllowManualTargetsSync = allowManualTargetsSync;
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("query");
            commandLine.addAll(getOptions());
            commandLine.add(queryString(myAllowManualTargetsSync));

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }

        public String queryString(boolean allowManualTargetsSync) {
            if (myTargets.isEmpty()) {
                return "";
            }
            String targetString = targetQueryExpression(this);
            if (allowManualTargetsSync) {
                return targetString;
            }
            else {
                return excludeManualTargetsQueryString(targetString);
            }
        }

        private String excludeManualTargetsQueryString(String targetString) {
            if (Platform.current().os().isWindows()) {
                return "attr('tags', '^((?!manual).)*$', " + targetString + ")";
            }
            else {
                return "attr(\"tags\", \"^((?!manual).)*$\", " + targetString + ")";
            }
        }
    }

    public static class CQuery extends BazelCommand implements HasMultipleTargets {
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();

        public CQuery(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("cquery");
            commandLine.addAll(getOptions());
            commandLine.addAll(targetQueryCommandLine(this));

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }
    }

    public static class AQuery extends BazelCommand implements HasMultipleTargets {
        private final List<Label> myTargets = new ArrayList<>();
        private final List<Label> myExcludedTargets = new ArrayList<>();

        public AQuery(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public List<Label> getTargets() {
            return myTargets;
        }

        @Override
        public List<Label> getExcludedTargets() {
            return myExcludedTargets;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("aquery");
            commandLine.addAll(getOptions());
            commandLine.addAll(targetQueryCommandLine(this));

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }
    }

    public static class Config extends BazelCommand {
        public Config(String bazelBinary) {
            super(bazelBinary);
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("config");
            commandLine.addAll(getOptions());

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }

    }

    public abstract static class SimpleCommand extends BazelCommand {
        private final List<String> myCommand;

        public SimpleCommand(String bazelBinary, List<String> command) {
            super(bazelBinary);
            myCommand = command;
        }

        public List<String> getCommand() {
            return myCommand;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.addAll(myCommand);
            commandLine.addAll(getOptions());

            return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
        }
    }

    public static class Info extends SimpleCommand {
        public Info(String bazelBinary) {
            super(bazelBinary, List.of("info"));
        }
    }

    public static class Clean extends SimpleCommand {
        public Clean(String bazelBinary) {
            super(bazelBinary, List.of("clean"));
        }
    }

    public static class ShutDown extends SimpleCommand {
        public ShutDown(String bazelBinary) {
            super(bazelBinary, List.of("shutdown"));
        }
    }

    public static class ModGraph extends SimpleCommand {
        public ModGraph(String bazelBinary) {
            super(bazelBinary, List.of("mod", "graph"));
        }
    }

    public static class ModPath extends SimpleCommand {
        public ModPath(String bazelBinary) {
            super(bazelBinary, List.of("mod", "path"));
        }
    }

    public static class ModShowRepo extends SimpleCommand {
        public ModShowRepo(String bazelBinary) {
            super(bazelBinary, List.of("mod", "show_repo"));
        }
    }

    public static class ModDumpRepoMapping extends SimpleCommand {
        public ModDumpRepoMapping(String bazelBinary) {
            super(bazelBinary, List.of("mod", "dump_repo_mapping"));
        }
    }

    public static class QueryExpression extends BazelCommand {
        private final String myExpression;

        public QueryExpression(String bazelBinary, String expression) {
            super(bazelBinary);
            myExpression = expression;
        }

        @Override
        public BazelCommandExecutionDescriptor buildExecutionDescriptor() {
            List<String> commandLine = new ArrayList<>(List.of(getBazelBinary()));

            commandLine.addAll(getStartupOptions());
            commandLine.add("query");
            commandLine.addAll(getOptions());

            // large query might not fit into command line, spill to file when needed
            if (myExpression.length() <= MAX_INLINE_QUERY_EXPRESSION_LENGTH) {
                commandLine.add(myExpression);
                return new BazelCommandExecutionDescriptor(commandLine, getEnablePty());
            }

            Path queryFile = createBazelCommandFile(QUERY_FILE, List.of(myExpression));
            commandLine.add(BazelFlag.queryFile(queryFile.toString()));

            return new BazelCommandExecutionDescriptor(
                commandLine,
                getEnablePty(),
                Map.of(),
                false,
                () -> deleteBazelCommandFile(QUERY_FILE, queryFile)
            );
        }
    }

    // consulo: upstream private top-level declarations of BazelCommand.kt

    private static String targetQueryExpression(HasMultipleTargets receiver) {
        String includesString = receiver.getTargets().stream().map(it -> "\"" + it + "\"").collect(Collectors.joining(" + "));
        String excludesString = receiver.getExcludedTargets().stream().map(it -> "\"" + it + "\"").collect(Collectors.joining(" - "));
        return excludesString.isEmpty() ? includesString : includesString + " - " + excludesString;
    }

    private static List<String> targetQueryCommandLine(HasMultipleTargets receiver) {
        return receiver.getTargets().isEmpty() ? List.of("--") : List.of("--", targetQueryExpression(receiver));
    }

    private static final Logger LOG = Logger.getInstance(BazelCommand.class);

    private static final int MAX_INLINE_QUERY_EXPRESSION_LENGTH = 8_000;

    private static final BazelCommandFile TARGET_PATTERN_FILE = new BazelCommandFile("target pattern", "targets-");

    private static final BazelCommandFile QUERY_FILE = new BazelCommandFile("query", "query-");

    private record BazelCommandFile(String what, String prefix) {
    }

    private static Path createBazelCommandFile(BazelCommandFile kind, List<String> lines) {
        @Nullable Path file = null;
        try {
            Path tmpDir = Files.createDirectories(Path.of(ContainerPathManager.get().getTempPath()));
            file = Files.createTempFile(tmpDir, kind.prefix(), "");
            file.toFile().deleteOnExit();
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
        }
        catch (IOException e) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                }
                catch (IOException deleteException) {
                    throw new IllegalStateException("Couldn't delete file after creation failure", deleteException);
                }
            }
            throw new IllegalStateException("Couldn't create " + kind.what() + " file", e);
        }
        return file;
    }

    private static void deleteBazelCommandFile(BazelCommandFile kind, Path file) {
        try {
            Files.deleteIfExists(file);
        }
        catch (IOException e) {
            LOG.warn("Failed to delete " + kind.what() + " file", e);
        }
    }
}
