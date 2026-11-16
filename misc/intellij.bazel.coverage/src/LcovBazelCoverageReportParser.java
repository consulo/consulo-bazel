package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.coverage.LcovSerializationUtils;
import consulo.bazel.execution.coverage.LcovSerializationUtils.LcovCoverageReport;
import consulo.bazel.execution.coverage.LcovSerializationUtils.LineHits;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ControlFlowException;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser.BazelCoverageLineConsumer;
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser.CoverageLoadErrorReporter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ExtensionImpl(order = "last")
public class LcovBazelCoverageReportParser implements BazelCoverageReportParser {
    // consulo: upstream private top-level val of LcovBazelCoverageReportParser.kt
    private static final Logger LOG = Logger.getInstance(LcovBazelCoverageReportParser.class);

    @Override
    public Collection<Path> parse(
        List<Path> coverageFiles,
        Project project,
        VirtualFile projectRoot,
        CoverageLoadErrorReporter reporter,
        BazelCoverageLineConsumer consumer
    ) {
        if (coverageFiles.isEmpty()) {
            return List.of();
        }

        List<Path> consumedCoverageFiles = new ArrayList<>();
        for (Path coverageFile : coverageFiles) {
            try {
                // consulo: com.intellij.coverage.lcov.LcovSerializationUtils is missing, a Consulo-only port of its reader is used
                LcovCoverageReport lcovCoverageReport = LcovSerializationUtils.readLCOVFromPaths(List.of(coverageFile));
                if (lcovCoverageReport.getInfo().isEmpty()) {
                    reportWarning(reporter, BazelPluginLocalize.coverageLcovReportEmpty(coverageFile).get());
                    continue;
                }
                for (Map.Entry<String, List<LineHits>> entry : lcovCoverageReport.getInfo().entrySet()) {
                    String filePath = entry.getKey();
                    List<LineHits> lineHints = entry.getValue();
                    VirtualFile file = resolveFromRootOrRelative(projectRoot, filePath);
                    if (file == null) {
                        continue;
                    }
                    for (LineHits lineHint : lineHints) {
                        consumer.addCoverage(file, lineHint.getLineNumber(), lineHint.getHits());
                    }
                }
                consumedCoverageFiles.add(coverageFile);
            }
            catch (Exception e) {
                if (e instanceof ControlFlowException) {
                    throw (RuntimeException) e;
                }
                reportWarning(reporter, BazelPluginLocalize.coverageLcovReportFailed(coverageFile).get(), e);
            }
        }
        return consumedCoverageFiles;
    }

    // consulo: upstream private top-level fun of LcovBazelCoverageReportParser.kt (with the default argument exception = null)
    private static void reportWarning(CoverageLoadErrorReporter reporter, String message) {
        reportWarning(reporter, message, null);
    }

    private static void reportWarning(CoverageLoadErrorReporter reporter, String message, @Nullable Exception exception) {
        LOG.warn(message, exception);
        reporter.reportWarning(message, exception);
    }

    // consulo: IntelliJ VirtualFile.resolveFromRootOrRelative(path)
    private static @Nullable VirtualFile resolveFromRootOrRelative(VirtualFile root, String path) {
        if (path.startsWith("/")) {
            return root.getFileSystem().findFileByPath(path);
        }
        return root.findFileByRelativePath(path);
    }
}
