package org.jetbrains.bazel.server;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// consulo: holder class for the top-level declarations of upstream BazelQuery.kt
public final class BazelQuery {
    private BazelQuery() {
    }

    public interface BazelQueryOutput<T> {
        @Nullable String getFlag();

        T parse(byte[] stdout);

        // --output=label
        final class Labels implements BazelQueryOutput<List<Label>> {
            public static final Labels INSTANCE = new Labels();

            private final String myFlag = BazelFlag.OutputFormat.label();

            private Labels() {
            }

            @Override
            public String getFlag() {
                return myFlag;
            }

            @Override
            public List<Label> parse(byte[] stdout) {
                return new String(stdout, StandardCharsets.UTF_8)
                    .lines()
                    .filter(it -> !it.isBlank())
                    .map(it -> Label.parse(it.trim()))
                    .toList();
            }
        }

        // --output=streamed_proto
        final class Targets implements BazelQueryOutput<List<Build.Target>> {
            public static final Targets INSTANCE = new Targets();

            private final String myFlag = BazelFlag.OutputFormat.streamed_proto();

            private Targets() {
            }

            @Override
            public String getFlag() {
                return myFlag;
            }

            @Override
            public List<Build.Target> parse(byte[] stdout) {
                InputStream stream = new ByteArrayInputStream(stdout);
                List<Build.Target> result = new ArrayList<>();
                try {
                    while (true) {
                        Build.Target target = Build.Target.parseDelimitedFrom(stream);
                        if (target == null) {
                            break;
                        }
                        result.add(target);
                    }
                }
                catch (IOException e) {
                    // consulo: Kotlin has no checked exceptions
                    throw new UncheckedIOException(e);
                }
                return result;
            }
        }

        // --output=proto
        final class Proto implements BazelQueryOutput<Build.QueryResult> {
            public static final Proto INSTANCE = new Proto();

            private final String myFlag = BazelFlag.OutputFormat.proto();

            private Proto() {
            }

            @Override
            public String getFlag() {
                return myFlag;
            }

            @Override
            public Build.QueryResult parse(byte[] stdout) {
                try {
                    return Build.QueryResult.parseFrom(stdout);
                }
                catch (IOException e) {
                    // consulo: Kotlin has no checked exceptions
                    throw new UncheckedIOException(e);
                }
            }
        }

        record Raw(@Nullable String format) implements BazelQueryOutput<String> {
            public Raw() {
                this(null);
            }

            @Override
            public @Nullable String getFlag() {
                return format != null ? "--output=" + format : null;
            }

            @Override
            public String parse(byte[] stdout) {
                return new String(stdout, StandardCharsets.UTF_8);
            }
        }
    }

    public record BazelQueryParams<T>(
        String expression,
        BazelQueryOutput<T> output,
        @Nullable TaskId taskId,
        boolean keepGoing,
        List<String> flags
    ) {
        public BazelQueryParams(String expression, BazelQueryOutput<T> output) {
            this(expression, output, null);
        }

        public BazelQueryParams(String expression, BazelQueryOutput<T> output, @Nullable TaskId taskId) {
            this(expression, output, taskId, true);
        }

        public BazelQueryParams(String expression, BazelQueryOutput<T> output, @Nullable TaskId taskId, boolean keepGoing) {
            this(expression, output, taskId, keepGoing, List.of());
        }
    }

    public record BazelQueryResult<T>(
        T result,
        BazelStatus status,
        List<String> stderrLines
    ) {
    }

    public static <T> CompletableFuture<BazelQueryResult<T>> runBazelQuery(
        BazelRunner bazelRunner,
        ProjectView projectView,
        BazelQueryParams<T> params
    ) {
        BazelCommand command =
            bazelRunner.buildBazelCommand(projectView, builder ->
                builder.queryExpression(params.expression(), queryExpression -> {
                    String flag = params.output().getFlag();
                    if (flag != null) {
                        queryExpression.getOptions().add(flag);
                    }
                    if (params.keepGoing()) {
                        queryExpression.getOptions().add(BazelFlag.keepGoing());
                    }
                    queryExpression.getOptions().addAll(params.flags());
                })
            );
        return bazelRunner
            .runBazelCommand(command, params.taskId(), false)
            .waitAndGetResult()
            .thenApply(processResult -> new BazelQueryResult<>(
                params.output().parse(processResult.getStdout()),
                processResult.getBazelStatus(),
                processResult.getStderrLines()
            ));
    }
}
