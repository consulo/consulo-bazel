package org.jetbrains.bazel.golang.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.ImportPathId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@ExtensionImpl
public class BazelGoCoverageReportParser implements BazelCoverageReportParser {
    // consulo: upstream private top-level val of BazelGoCoverageReportParser.kt
    private static final Logger LOG = Logger.getInstance(BazelGoCoverageReportParser.class);

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
        Consumer<String> warningReporter = message -> {
            LOG.warn(message);
            reporter.reportWarning(message);
        };
        GoCoverageSourceResolver goSourceResolver = new GoCoverageSourceResolver(project, projectRoot, warningReporter);
        Set<String> unresolvedFilePaths = new HashSet<>();
        for (Path coverageFile : coverageFiles) {
            boolean consumed =
                GoCoverageReportParser.parse(
                    coverageFile,
                    /* onMalformedLine = */ line ->
                        warningReporter.accept(BazelPluginLocalize.coverageGoReportMalformedLine(line).get()),
                    (filePath, lineNumber, hits) -> {
                        VirtualFile file = goSourceResolver.resolve(filePath);
                        if (file == null) {
                            if (unresolvedFilePaths.add(filePath)) {
                                warningReporter.accept(BazelPluginLocalize.coverageGoReportUnresolvedFile(filePath).get());
                            }
                            return;
                        }
                        consumer.addCoverage(file, lineNumber, hits);
                    }
                );
            if (consumed) {
                consumedCoverageFiles.add(coverageFile);
            }
        }
        return consumedCoverageFiles;
    }
}

class GoCoverageSourceResolver {
    private final Project myProject;
    private final VirtualFile myProjectRoot;
    private final Consumer<String> myWarningReporter;

    private final Map<String, @Nullable VirtualFile> myResolvedFiles = new HashMap<>();

    GoCoverageSourceResolver(
        Project project,
        VirtualFile projectRoot,
        Consumer<String> warningReporter
    ) {
        myProject = project;
        myProjectRoot = projectRoot;
        myWarningReporter = warningReporter;
    }

    public @Nullable VirtualFile resolve(String filePath) {
        if (myResolvedFiles.containsKey(filePath)) {
            return myResolvedFiles.get(filePath);
        }
        VirtualFile file = resolveGoSource(filePath);
        if (file == null) {
            file = resolveFromRootOrRelative(myProjectRoot, filePath);
        }
        myResolvedFiles.put(filePath, file);
        return file;
    }

    private @Nullable VirtualFile resolveGoSource(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        String importPath = lastSlash < 0 ? "" : filePath.substring(0, lastSlash);
        String fileName = filePath.substring(lastSlash + 1);
        // consulo: upstream resolves the ImportPathId symbolic id in the workspace model
        BazelGoPackageEntity goPackage = GoPackageEntities.getInstance(myProject).resolve(new ImportPathId(importPath));
        if (goPackage == null) {
            return null;
        }
        VirtualFile result = null;
        for (String source : goPackage.sources()) {
            ProgressManager.checkCanceled();
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(source);
            if (file == null) {
                continue;
            }
            if (!file.getName().equals(fileName)) {
                continue;
            }
            VirtualFile existingFile = result;
            if (existingFile == null) {
                result = file;
            }
            else if (!existingFile.equals(file)) {
                myWarningReporter.accept(
                    BazelPluginLocalize.coverageGoReportPathCollision(filePath, existingFile.getPath(), file.getPath()).get()
                );
            }
        }
        return result;
    }

    // consulo: IntelliJ VirtualFile.resolveFromRootOrRelative(path)
    private static @Nullable VirtualFile resolveFromRootOrRelative(VirtualFile root, String path) {
        if (path.startsWith("/")) {
            return root.getFileSystem().findFileByPath(path);
        }
        return root.findFileByRelativePath(path);
    }
}
