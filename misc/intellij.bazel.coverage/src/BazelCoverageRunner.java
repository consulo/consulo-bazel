package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.data.CoverageLine;
import consulo.execution.coverage.data.CoverageLineImpl;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageProjectDataImpl;
import consulo.execution.coverage.data.LineStatus;
import consulo.logging.Logger;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser.BazelCoverageLineConsumer;
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser.CoverageLoadErrorReporter;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtensionImpl
public class BazelCoverageRunner extends CoverageRunner {
    private static final Logger LOG = Logger.getInstance(BazelCoverageRunner.class);

    public static BazelCoverageRunner getInstance() {
        return getInstance(BazelCoverageRunner.class);
    }

    public static String getNameInCoverageData(VirtualFile sourceFile) {
        return sourceFile.getPath();
    }

    public static @Nullable VirtualFile getFileByCoverageDataName(String name) {
        return LocalFileSystem.getInstance().findFileByPath(name);
    }

    private static final String ID = "BazelCoverageRunner";

    // consulo: the Consulo CoverageRunner.loadCoverageData(File, CoverageSuite) has no CoverageLoadErrorReporter and returns the
    //  project data (null on failure) instead of a CoverageLoadingResult
    @Override
    public @Nullable CoverageProjectData loadCoverageData(File sessionDataFile, @Nullable CoverageSuite suite) {
        Path coverageOutputDirectory = sessionDataFile.toPath();
        // TODO consulo: com.intellij.coverage.CoverageLoadErrorReporter is missing, the reported problems are only logged
        CoverageLoadErrorReporter reporter = (message, exception) -> LOG.warn(message, exception);
        if (!(suite instanceof BazelCoverageSuite bazelCoverageSuite)) {
            // TODO consulo: FailedCoverageLoadingResult is missing, the message is only logged
            LOG.warn(BazelPluginLocalize.coverageBazelLoadingFailedInvalidSuite().get());
            return null;
        }
        if (Application.get().isDispatchThread()) {
            // consulo: the progress supplier can't return null, the nullable result is wrapped into Optional
            return ProgressManager.getInstance().<Optional<CoverageProjectData>, RuntimeException>runProcessWithProgressSynchronously(
                () -> Optional.ofNullable(doLoadCoverageData(coverageOutputDirectory, bazelCoverageSuite, reporter)),
                BazelPluginLocalize.coverageBazelLoadingProgress(),
                true,
                bazelCoverageSuite.getProject()
            ).orElse(null);
        }
        return doLoadCoverageData(coverageOutputDirectory, bazelCoverageSuite, reporter);
    }

    private @Nullable CoverageProjectData doLoadCoverageData(
        Path coverageOutputDirectory,
        BazelCoverageSuite suite,
        CoverageLoadErrorReporter reporter
    ) {
        List<Path> coverageFiles;
        if (Files.isDirectory(coverageOutputDirectory)) {
            try (Stream<Path> entries = Files.list(coverageOutputDirectory)) {
                coverageFiles = entries.filter(Files::isRegularFile).collect(Collectors.toList());
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        else {
            coverageFiles = List.of();
        }
        if (coverageFiles.isEmpty()) {
            // TODO consulo: FailedCoverageLoadingResult is missing, the message is only logged
            LOG.warn(BazelPluginLocalize.coverageBazelLoadingFailedNoFiles(coverageOutputDirectory).get());
            return null;
        }
        CoverageProjectData projectData = new CoverageProjectDataImpl();
        VirtualFile projectRoot = org.jetbrains.bazel.config.project.getRootDir(suite.getProject());
        Map<VirtualFile, Map<Integer, Long>> coverageByFile = new HashMap<>();

        BazelCoverageLineConsumer coverageConsumer =
            (file, lineNumber, hits) -> {
                Map<Integer, Long> lineHits = coverageByFile.computeIfAbsent(file, it -> new HashMap<>());
                lineHits.merge(lineNumber, hits, (oldHits, newHits) -> Math.max(oldHits, newHits));
            };

        Set<Path> remainingCoverageFiles = new LinkedHashSet<>(coverageFiles);
        BazelCoverageReportParser.getEp().forEachExtensionSafe(parser -> {
            Collection<Path> consumedCoverageFiles =
                parser.parse(new ArrayList<>(remainingCoverageFiles), suite.getProject(), projectRoot, reporter, coverageConsumer);
            remainingCoverageFiles.removeAll(consumedCoverageFiles);
        });

        for (Map.Entry<VirtualFile, Map<Integer, Long>> entry : coverageByFile.entrySet()) {
            VirtualFile file = entry.getKey();
            Map<Integer, Long> lineHits = entry.getValue();
            // consulo: CoverageUnit.setLines() takes the lines indexed by line number (null where there is no code),
            //  and the Consulo CoverageLineImpl status is not derived from the hits
            int maxLineNumber = Collections.max(lineHits.keySet());
            List<@Nullable CoverageLine> lineData = new ArrayList<>(Collections.nCopies(maxLineNumber + 1, (CoverageLine) null));
            lineHits.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(lineEntry -> {
                    int lineNumber = lineEntry.getKey();
                    CoverageLineImpl line = new CoverageLineImpl(lineNumber, null);
                    line.setHits(toCoverageHits(lineEntry.getValue()));
                    line.setStatus(line.getHits() > 0 ? LineStatus.COVERED : LineStatus.NOT_COVERED);
                    if (lineNumber >= 0) {
                        lineData.set(lineNumber, line);
                    }
                });
            projectData.getOrCreateUnit(getNameInCoverageData(file)).setLines(lineData);
        }

        return projectData;
    }

    @Override
    public String getPresentableName() {
        return BazelPluginLocalize.coverageBazelRunnerPresentableName().get();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDataFileExtension() {
        return "lcov";
    }

    @Override
    public boolean acceptsCoverageEngine(CoverageEngine engine) {
        return engine instanceof BazelCoverageEngine;
    }

    // consulo: upstream private top-level extension fun Long.toCoverageHits() of BazelCoverageRunner.kt
    private static int toCoverageHits(long receiver) {
        return (int) Math.min(receiver, Integer.MAX_VALUE);
    }
}
