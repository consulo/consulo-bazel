package org.jetbrains.bazel.python.debug;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.impl.debugger.PyDebugRunner;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.platform.Platform;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PythonDebugUtils {
    private PythonDebugUtils() {
    }

    /**
     * consulo: upstream data class with default arguments
     */
    public record PythonDebugInfo(
        Path pythonFile,
        @Nullable Path pythonBinary,
        Map<String, String> environmentVariables,
        List<Path> libraryRoots,
        Path workingDirectory,
        List<String> targetArgs
    ) {
    }

    public static @Nullable PythonDebugInfo preparePythonDebug(Project project, Label target) {
        PythonBuildTarget pythonTargetData = getPythonTargetData(project, target);
        if (pythonTargetData == null) {
            return null;
        }
        Path runnerScript = pythonTargetData.runnerScript();
        if (runnerScript == null) {
            return null;
        }
        if (!Files.isRegularFile(runnerScript)) {
            return null;
        }

        Path runfiles = findRunfilesWorkspaceRoot(pythonTargetData);
        if (runfiles == null) {
            return null;
        }
        Path runfilesRoot = runfiles.getParent();
        // consulo: EnvironmentUtil.getValue -> Platform.current().os().getEnvironmentVariable
        String pythonPath = buildPythonPathEnv(
            runfiles,
            pythonTargetData.imports().asList(),
            Platform.current().os().getEnvironmentVariable("PYTHONPATH")
        );
        Path pythonBinary = findPythonBinary(pythonTargetData, project, runfiles, target);
        List<Path> projectRoots = new ArrayList<>();
        Path rootDirPath = toNioPathOrNull(org.jetbrains.bazel.config.project.getRootDir(project));
        if (rootDirPath != null) {
            projectRoots.add(rootDirPath);
        }
        projectRoots.add(runfiles);
        OutputLocation mainFileLocation = pythonTargetData.mainFile();
        Path mainFile = mainFileLocation != null
            ? TargetStorage.getTargetStorage(project).resolveExecrootOutputLocation(mainFileLocation)
            : null;
        List<Path> libraryRoots = new ArrayList<>();
        if (!runnerScript.equals(mainFile)) {
            libraryRoots.add(runnerScript);
        }
        if (runfilesRoot != null) {
            libraryRoots.add(runfilesRoot);
        }
        Path stage2Bootstrap = findStage2Bootstrap(runfiles, runnerScript);
        if (stage2Bootstrap != null) {
            libraryRoots.add(stage2Bootstrap);
        }
        Path interpreterRoot = pythonBinary != null ? findRunfilesInterpreterRoot(pythonBinary, runfilesRoot) : null;
        if (interpreterRoot != null) {
            libraryRoots.add(interpreterRoot);
        }
        Map<String, String> envs = new LinkedHashMap<>();
        envs.put("BAZEL_TARGET", target.toString());
        envs.put("BAZEL_WORKSPACE", "_main");
        envs.put("BAZEL_TARGET_NAME", target.getTargetName());
        envs.put("TEST_SRCDIR", runfilesRoot != null ? runfilesRoot.toString() : "");
        envs.put("TEST_WORKSPACE", "_main");
        envs.put("PYTHONPATH", pythonPath);
        envs.put(PyDebugRunner.IDE_PROJECT_ROOTS, toPathList(projectRoots)); // ignore generated Bazel scripts when "Ignore library files" is enabled
        Path workingDirectory = findWorkingDirectory(runfiles, target);

        Path pythonFile;
        if (isPythonScript(runnerScript)) {
            pythonFile = runnerScript;
        }
        else {
            if (mainFile == null) {
                return null;
            }
            pythonFile = mainFile;
        }
        return new PythonDebugInfo(
            pythonFile,
            pythonBinary,
            envs,
            new ArrayList<>(new LinkedHashSet<>(libraryRoots)),
            workingDirectory,
            pythonTargetData.targetArgs().asList()
        );
    }

    private static @Nullable Path findPythonBinary(PythonBuildTarget receiver, Project project, Path runfilesWorkspaceRoot, Label target) {
        Path venvPythonBinary = findVenvPythonBinary(receiver, runfilesWorkspaceRoot, target);
        if (venvPythonBinary != null) {
            return venvPythonBinary;
        }
        OutputLocation interpreter = receiver.interpreter();
        return interpreter != null ? TargetStorage.getTargetStorage(project).resolveExecrootOutputLocation(interpreter) : null;
    }

    private static @Nullable Path findVenvPythonBinary(PythonBuildTarget receiver, Path runfilesWorkspaceRoot, Label target) {
        Path runfilesRoot = runfilesWorkspaceRoot.getParent();
        if (runfilesRoot == null) {
            return null;
        }
        Path manifest = runfilesRoot.resolve(RUNFILES_MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }

        OutputLocation interpreter = receiver.interpreter();
        String interpreterName = interpreter != null ? name(OutputLocation.getRelativeNioPath(interpreter)) : null;
        if (interpreterName != null && interpreterName.isEmpty()) {
            interpreterName = null;
        }
        Set<String> pythonBinaryNames = new LinkedHashSet<>();
        if (interpreterName != null) {
            pythonBinaryNames.add(interpreterName);
        }
        pythonBinaryNames.addAll(DEFAULT_PYTHON_BINARY_NAMES);
        // consulo: Kotlin runCatching { }.getOrNull()
        try (BufferedReader reader = Files.newBufferedReader(manifest)) {
            for (String line : (Iterable<String>) reader.lines()::iterator) {
                RunfilesManifestEntry entry = toRunfilesManifestEntry(line);
                if (entry == null) {
                    continue;
                }
                if (!isTargetVenvPythonBinary(entry.runfilesPath(), target, pythonBinaryNames)) {
                    continue;
                }
                Path candidate = runfilesRoot.resolve(entry.runfilesPath());
                if (Files.isExecutable(candidate)) {
                    return candidate;
                }
            }
            return null;
        }
        catch (Throwable e) {
            return null;
        }
    }

    private static @Nullable RunfilesManifestEntry toRunfilesManifestEntry(String receiver) {
        if (receiver.isBlank()) {
            return null;
        }
        int separatorIndex = receiver.indexOf(' ');
        if (separatorIndex < 0) {
            return new RunfilesManifestEntry(receiver, null);
        }
        else {
            return new RunfilesManifestEntry(
                receiver.substring(0, separatorIndex),
                Path.of(receiver.substring(separatorIndex + 1))
            );
        }
    }

    private static boolean isTargetVenvPythonBinary(String receiver, Label target, Set<String> pythonBinaryNames) {
        String packagePath = target.packagePath().toString();
        String pathInWorkspace = removePrefix(receiver, MAIN_WORKSPACE_RUNFILES_PATH + "/");
        if (pathInWorkspace.equals(receiver)) {
            return false;
        }
        String pathInPackage;
        if (packagePath.isEmpty()) {
            pathInPackage = pathInWorkspace;
        }
        else {
            pathInPackage = removePrefix(pathInWorkspace, packagePath + "/");
        }
        if (pathInPackage.equals(pathInWorkspace) && !packagePath.isEmpty()) {
            return false;
        }

        String[] segments = pathInPackage.split("/", -1);
        if (segments.length != 3 || !segments[1].equals("bin")) {
            return false;
        }
        String venvName = removePrefix(removeSuffix(segments[0], ".venv"), "_");
        return venvName.equals(removePrefix(target.getTargetName(), "_")) && pythonBinaryNames.contains(segments[2]);
    }

    private static boolean isPythonScript(Path receiver) {
        return FileTypeRegistry.getInstance().getFileTypeByFileName(name(receiver)) instanceof PythonFileType || hasPythonHashBang(receiver);
    }

    private static boolean hasPythonHashBang(Path receiver) {
        // consulo: Kotlin runCatching { }.getOrDefault(false)
        try (InputStream input = Files.newInputStream(receiver)) {
            byte[] bytes = new byte[HASH_BANG_MAX_BYTES];
            int length = input.read(bytes);
            if (length <= 0) {
                return false;
            }
            String firstChars = new String(bytes, 0, length, StandardCharsets.UTF_8);
            int newLine = firstChars.indexOf('\n');
            String firstLine = newLine >= 0 ? firstChars.substring(0, newLine) : firstChars;
            return firstLine.startsWith("#!") && firstLine.indexOf(PYTHON_HASH_BANG_MARKER, 2) >= 0;
        }
        catch (Throwable e) {
            return false;
        }
    }

    /**
     * Assembles a new value for PYTHONPATH environment variable, with single paths separated with ':' (UNIX) or ';' (Windows)
     */
    public static String buildPythonPathEnv(Path runfilesWorkspaceRoot, List<String> imports, @Nullable String existingPythonPath) {
        List<Path> importRoots = new ArrayList<>();
        for (String it : imports) {
            importRoots.add(runfilesWorkspaceRoot.resolve(it));
        }
        List<Path> sitePackageRoots = findSitePackagesRoots(runfilesWorkspaceRoot.getParent());
        List<Path> allRoots = new ArrayList<>();
        allRoots.addAll(List.of(runfilesWorkspaceRoot));
        allRoots.addAll(importRoots);
        allRoots.addAll(sitePackageRoots);

        // consulo: com.intellij.execution.Platform.current().pathSeparator -> Platform.current().fs().getPathListSeparator()
        String pathSeparator = Platform.current().fs().getPathListSeparator();
        String newPythonPath = allRoots.stream().distinct().map(Path::toString).collect(Collectors.joining(pathSeparator));
        if (existingPythonPath == null || existingPythonPath.isBlank()) {
            return newPythonPath;
        }
        else {
            return newPythonPath + pathSeparator + existingPythonPath;
        }
    }

    // consulo: overload for the upstream default argument (existingPythonPath = null)
    public static String buildPythonPathEnv(Path runfilesWorkspaceRoot, List<String> imports) {
        return buildPythonPathEnv(runfilesWorkspaceRoot, imports, null);
    }

    public static Path findWorkingDirectory(Path runfilesWorkspaceRoot, Label target) {
        Path packageWorkingDirectory = runfilesWorkspaceRoot.resolve(target.packagePath().toString());
        return Files.isDirectory(packageWorkingDirectory) ? packageWorkingDirectory : runfilesWorkspaceRoot;
    }

    public static @Nullable String findRealSourceFile(
        Project project,
        Label target,
        String file
    ) {
        Path rootDir = toNioPathOrNull(org.jetbrains.bazel.config.project.getRootDir(project));
        if (rootDir == null) {
            return file;
        }
        Path filePath;
        try {
            // if the file string is not a valid path, or the file does not exist, it should not cause a debugger failure
            // throwing an exception might cause debugger freeze, returning null will simply show "frame not available" in stack trace
            filePath = Path.of(file);
        }
        catch (InvalidPathException e) {
            return null;
        }
        filePath = PathUtil.toRealPathOrNull(filePath);
        if (filePath == null) {
            return null;
        }
        if (PathUtil.isUnder(filePath, Set.of(rootDir))) {
            // after resolving symlinks, path is inside the project - no need to search elsewhere
            return filePath.toString();
        }

        PythonBuildTarget pythonTargetData = getPythonTargetData(project, target);
        Path runFileSourceRoot = pythonTargetData != null ? findRunfilesWorkspaceRoot(pythonTargetData) : null;
        if (runFileSourceRoot != null) {
            Path rebased = PathUtil.rebaseOrNull(filePath, runFileSourceRoot, rootDir);
            if (rebased != null) {
                return rebased.toString();
            }
        }

        return file;
    }

    private static @Nullable PythonBuildTarget getPythonTargetData(Project project, Label target) {
        return TargetStorage.getTargetStorage(project).getTargetDataForLabel(target, PythonBuildTarget.class);
    }

    private static @Nullable Path findRunfilesWorkspaceRoot(PythonBuildTarget receiver) {
        Path runnerScript = receiver.runnerScript();
        if (runnerScript == null) {
            return null;
        }
        Path parent = runnerScript.getParent();
        return parent != null ? parent.resolve(name(runnerScript) + ".runfiles").resolve("_main") : null;
    }

    private static @Nullable Path findStage2Bootstrap(Path runfilesWorkspaceRoot, Path runnerScript) {
        Path path = runfilesWorkspaceRoot.resolve("_" + removeSuffix(name(runnerScript), ".exe") + "_stage2_bootstrap.py");
        return Files.isRegularFile(path) ? path : null;
    }

    private static @Nullable Path findRunfilesInterpreterRoot(Path receiver, @Nullable Path runfilesRoot) {
        if (runfilesRoot == null || !receiver.startsWith(runfilesRoot)) {
            return null;
        }
        Path binaryDirectory = receiver.getParent();
        if (binaryDirectory == null) {
            return null;
        }
        if (name(binaryDirectory).equals("bin") || name(binaryDirectory).equals("Scripts")) {
            return binaryDirectory.getParent();
        }
        else {
            return binaryDirectory;
        }
    }

    private static String toPathList(List<Path> receiver) {
        return receiver.stream().distinct().map(Path::toString).collect(Collectors.joining(Platform.current().fs().getPathListSeparator()));
    }

    private static List<Path> findSitePackagesRoots(@Nullable Path runfilesRoot) {
        if (runfilesRoot == null || !Files.isDirectory(runfilesRoot)) {
            return List.of();
        }

        // consulo: Kotlin runCatching { }.getOrElse { emptyList() }
        try (Stream<Path> paths = Files.walk(runfilesRoot, SITE_PACKAGES_MAX_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            return paths
                .filter(it -> Files.isDirectory(it) && it.getFileName() != null && it.getFileName().toString().equals("site-packages"))
                .toList();
        }
        catch (Throwable e) {
            return List.of();
        }
    }

    public static List<String> extractPythonTargetArgs(TargetIdeInfo target) {
        return List.copyOf(target.getPyIdeInfo().getArgsList());
    }

    public static List<String> buildPythonDebugBazelArguments(List<String> debugFlags, @Nullable String additionalBazelParams) {
        List<String> result = new ArrayList<>(debugFlags);
        result.addAll(Utils.parseAsProgramArguments(additionalBazelParams));
        return result;
    }

    public static @Nullable String buildPythonDebugScriptParameters(List<String> targetArgs, @Nullable String runConfigArguments) {
        List<String> parts = new ArrayList<>();
        String joinedTargetArgs = ParametersListUtil.join(targetArgs);
        if (!joinedTargetArgs.isBlank()) {
            parts.add(joinedTargetArgs);
        }
        if (runConfigArguments != null && !runConfigArguments.isBlank()) {
            parts.add(runConfigArguments);
        }
        String result = String.join(" ", parts);
        return result.isEmpty() ? null : result;
    }

    public static String buildPythonLibraryRoots(Sdk sdk, List<Path> additionalRoots) {
        List<String> allRoots = new ArrayList<>();
        // consulo: OrderRootType.CLASSES -> BinariesOrderRootType
        for (VirtualFile it : sdk.getRootProvider().getFiles(BinariesOrderRootType.ID)) {
            allRoots.add(it.getPath());
        }
        for (Path it : additionalRoots) {
            allRoots.add(it.toString());
        }
        return allRoots.stream().distinct().collect(Collectors.joining(Platform.current().fs().getPathListSeparator()));
    }

    // consulo: upstream private top-level declarations of PythonDebugUtils.kt

    private static final int SITE_PACKAGES_MAX_DEPTH = 4;
    private static final int HASH_BANG_MAX_BYTES = 256;
    private static final String PYTHON_HASH_BANG_MARKER = "python";
    private static final String RUNFILES_MANIFEST_NAME = "MANIFEST";
    private static final String MAIN_WORKSPACE_RUNFILES_PATH = "_main";
    private static final List<String> DEFAULT_PYTHON_BINARY_NAMES = List.of("python3", "python");

    private record RunfilesManifestEntry(
        String runfilesPath,
        @Nullable Path realPath
    ) {
    }

    // consulo: Kotlin Path.name
    private static String name(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }

    // consulo: Kotlin String.removePrefix / removeSuffix
    private static String removePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String removeSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
