package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelCoverageReportParser {
    /**
     * Parses matching coverage files and returns files that should not be passed to later parsers.
     */
    Collection<Path> parse(
        List<Path> coverageFiles,
        Project project,
        VirtualFile projectRoot,
        CoverageLoadErrorReporter reporter,
        BazelCoverageLineConsumer consumer
    );

    // consulo: upstream `ep`, served by the Consulo extension point
    static ExtensionPoint<BazelCoverageReportParser> getEp() {
        return Application.get().getExtensionPoint(BazelCoverageReportParser.class);
    }

    // consulo: upstream public top-level fun interface of BazelCoverageReportParser.kt
    @FunctionalInterface
    interface BazelCoverageLineConsumer {
        void addCoverage(VirtualFile file, int lineNumber, long hits);
    }

    // TODO consulo: com.intellij.coverage.CoverageLoadErrorReporter is missing in Consulo (CoverageRunner.loadCoverageData has
    //  no reporter); this stand-in keeps the upstream signatures, BazelCoverageRunner only logs the reported problems
    interface CoverageLoadErrorReporter {
        void reportWarning(String message, @Nullable Throwable exception);

        default void reportWarning(String message) {
            reportWarning(message, null);
        }
    }
}
