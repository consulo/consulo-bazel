package org.jetbrains.bsp.protocol;

import com.google.common.collect.Iterables;
import com.google.devtools.intellij.aspect.Common;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bsp.protocol.OutputLocation.External;
import org.jetbrains.bsp.protocol.OutputLocation.Output;
import org.jetbrains.bsp.protocol.OutputLocation.Workspace;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OutputLocationParser {
    private final BazelPathsResolver myBazelPathsResolver;
    private final BazelOutFileHardLinks myHardLinks;

    public OutputLocationParser(BazelPathsResolver bazelPathsResolver, BazelOutFileHardLinks hardLinks) {
        myBazelPathsResolver = bazelPathsResolver;
        myHardLinks = hardLinks;
    }

    /**
     * Parses a raw path that Bazel spells in the execroot coordinates,
     * for example a toolchain path or an include directory.
     * <p>
     * A workspace convenience symlink name, for example {@code bazel-bin}, maps to {@link Workspace}.
     * The workspace root then resolves it through the symlink.
     * An empty input maps to {@code Workspace("")}.
     * <p>
     * No hard linking happens here; we assume the target that produces the artifacts will call {@link #parse} to do it.
     */
    public List<OutputLocation> parseExecrootPath(List<String> raw) {
        return raw.stream()
            .map(OutputLocationParserWithoutHardlink::parseExecrootPath)
            .toList();
    }

    public OutputLocation parseExecrootPath(String raw) {
        return OutputLocationParserWithoutHardlink.parseExecrootPath(raw);
    }

    /**
     * Parses an aspect {@link Common.ArtifactLocation}.
     * <p>
     * The aspect has two constructors with different guarantees, see {@code common/artifact_location.bzl}:
     * - {@code from_file} derives the fields from a {@code File}. The root path of a generated file
     * starts with {@code bazel-out} and the flags are reliable.
     * - {@code from_execpath} wraps a raw string, for example {@code java_home}. It always sets
     * {@code is_source = false}, and the relative path can be absolute, for example a {@code local_jdk} home.
     * <p>
     * So the root path decides {@link Output}, the {@code is_external} flag decides {@link External},
     * and an {@code from_execpath} leftover is an execroot spelling for {@link #parseExecrootPath}.
     */
    public CompletableFuture<List<OutputLocation>> parse(List<Common.ArtifactLocation> locations) {
        List<OutputLocation> parsed = locations.stream().map(OutputLocationParserWithoutHardlink::parse).toList();
        return hardLinkOutputs(parsed).thenApply(unused -> parsed);
    }

    private CompletableFuture<Void> hardLinkOutputs(List<OutputLocation> parsed) {
        List<Path> outputArtifacts = parsed.stream().filter(OutputLocation::isGenerated)
            .map(output -> myBazelPathsResolver.relativePathToExecRootAbsolute(Path.of(OutputLocation.toExecrootPath(output))).normalize())
            .toList();
        return myHardLinks.createOutputFileHardLinks(outputArtifacts).thenAccept(links -> {
        });
    }

    /**
     * Prefer to use the override that takes a {@code List} for parallel IO during hardlink creation
     */
    public CompletableFuture<OutputLocation> parse(Common.ArtifactLocation location) {
        return parse(List.of(location)).thenApply(Iterables::getOnlyElement);
    }
}
