package org.jetbrains.bazel.languages.projectview;

import consulo.application.ReadAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jetbrains.bazel.languages.projectview.imports.ImportFactory;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.jetbrains.bazel.config.project.getRootDir;

/**
 * The project view is parsed using light PSI rather than regular PSI.
 * <p>
 * Using regular PSI was leading to problems when used in `BazelProjectStorePathCustomizer`. Also, using regular PSI is not needed.
 * <p>
 * Regular PSI might lead to FileDocumentManagerListener.fileContentLoaded publishing,
 * which reaches the listeners of every project - including the one that might be currently initialized.
 * In our case BreakpointInstrumentationFileDocumentManagerListener, could be triggered resulting in some project-level service access,
 * but it's not allowed before the project is fully initialized.
 * <p>
 * It is hard to reproduce because in regular usage `BazelProjectStorePathCustomizer` is called multiple times e.g. before the project init.
 * `BazelProjectStorePathCustomizer` call before actual project init is safe, and it keeps the project view file cached,
 * so the later call during the project init does not produce the event (and therefore the error).
 * However, if the GC clears the loaded file, then the call during project init needs to load the file again, and it leads to exception.
 */
public final class ProjectViewFactory {
    private ProjectViewFactory() {
    }

    /**
     * Parses the project view against {@code root}. Use this function only when the project root is not set,
     * for example in `BazelProjectStorePathCustomizer`. The checkers do not run, because they need the project root.
     */
    public static @Nullable ProjectView fromOrNull(
        Project project,
        Path source,
        Path root
    ) {
        return loadOrNull(source, () -> parse(project, readContent(source), root, source, null));
    }

    /**
     * Parses the project view from {@code source} against the {@code project} root, and calls {@code onEachProblem} for each problem.
     * The checkers resolve the imports against the {@code project} root, so the parse uses the same root.
     */
    public static @Nullable ProjectView fromOrNull(
        Project project,
        Path source,
        @Nullable Consumer<ProjectViewProblem> onEachProblem
    ) {
        return loadOrNull(source, () -> from(project, source, onEachProblem));
    }

    // consulo: overload for the upstream default argument (onEachProblem = null)
    public static @Nullable ProjectView fromOrNull(
        Project project,
        Path source
    ) {
        return fromOrNull(project, source, (Consumer<ProjectViewProblem>) null);
    }

    public static ProjectView fromDefault(Project project) {
        return fromDefault(project, false);
    }

    public static ProjectView fromDefault(
        Project project,
        boolean forceDefaultTemplate
    ) {
        return from(
            project,
            ProjectViewFileUtils.projectViewTemplate(getRootDir(project), null, forceDefaultTemplate)
        );
    }

    /**
     * Parses project view from {@code source} against the {@code project} root and calls {@code onEachProblem} for each problem.
     */
    public static ProjectView from(
        Project project,
        Path source,
        @Nullable Consumer<ProjectViewProblem> onEachProblem
    ) {
        return parse(
            project,
            readContent(source),
            getRootDir(project).toNioPath(),
            source,
            onEachProblem
        );
    }

    // consulo: overload for the upstream default argument (onEachProblem = null)
    public static ProjectView from(
        Project project,
        Path source
    ) {
        return from(project, source, (Consumer<ProjectViewProblem>) null);
    }

    /**
     * Parses {@code content} against the {@code project} root, and calls {@code onEachProblem} for each problem.
     * {@code source} is the file of the content, if it has one.
     */
    public static ProjectView from(
        Project project,
        CharSequence content,
        @Nullable Path source,
        @Nullable Consumer<ProjectViewProblem> onEachProblem
    ) {
        return parse(project, content, getRootDir(project).toNioPath(), source, onEachProblem);
    }

    // consulo: overloads for the upstream default arguments (source = null, onEachProblem = null)
    public static ProjectView from(
        Project project,
        CharSequence content
    ) {
        return from(project, content, null, null);
    }

    public static ProjectView from(
        Project project,
        CharSequence content,
        @Nullable Path source
    ) {
        return from(project, content, source, null);
    }

    // In principle, project view parse should not throw.
    // However, in case it does, it might break some code paths e.g. project opening, so it's better to keep it in a try-catch block.
    private static @Nullable ProjectView loadOrNull(Path path, Supplier<ProjectView> load) {
        try {
            return load.get();
        }
        catch (Throwable e) {
            // consulo: upstream getOrHandleException rethrows control flow and cancellation exceptions
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }
            LOG.error("Failed to load project view file at " + path + ".", e);
            return null;
        }
    }

    private static ProjectView parse(
        Project project,
        CharSequence content,
        Path root,
        @Nullable Path source,
        @Nullable Consumer<ProjectViewProblem> onEachProblem
    ) {
        Path fileName = source != null ? source.getFileName() : null;
        String name = fileName != null ? fileName.toString() : Constants.DEFAULT_PROJECT_VIEW_FILE_NAME;
        // consulo: Consulo asserts read access for light PSI too (except ReadOnlyLightVirtualFile), the parse runs in a read action
        return ReadAction.compute(() ->
            new Parser(
                project,
                root,
                onEachProblem,
                source
            ).parse(createLightPsiFile(project, name, content), source)
        );
    }

    // consulo: upstream private top-level class Parser of ProjectViewFactory.kt
    private static final class Parser {
        private final Project myProject;
        private final Path myRoot;
        private final @Nullable Consumer<ProjectViewProblem> myOnEachProblem;

        private final Map<Path, @Nullable ProjectView> myParsed = new HashMap<>();
        private final Set<Path> myInProgress = new HashSet<>();

        Parser(
            Project project,
            Path root,
            @Nullable Consumer<ProjectViewProblem> onEachProblem,
            @Nullable Path mainFile
        ) {
            myProject = project;
            myRoot = root;
            myOnEachProblem = onEachProblem;
            if (mainFile != null) {
                myInProgress.add(PathUtil.toRealPathOrSelf(mainFile));
            }
        }

        ProjectView parse(ProjectViewPsiFile psi, @Nullable Path source) {
            Set<Import.Resolved> imports = new LinkedHashSet<>();
            Map<ProjectViewSectionKey<?>, Object> sections = new LinkedHashMap<>();
            if (myOnEachProblem != null) {
                ProjectViewChecker.withSourcePath(ProjectViewChecker.checkChildren(ProjectViewChecker.Main, psi), source)
                    .forEach(myOnEachProblem);
            }
            for (PsiElement it : psi.getChildren()) {
                if (it instanceof ProjectViewPsiSection psiSection) {
                    Pair<ProjectViewSection<?>, Object> sectionWithValue = toSectionWithValue(psiSection);
                    if (sectionWithValue != null) {
                        mergeSection(sections, sectionWithValue.getFirst().getKey(), sectionWithValue.getSecond());
                    }
                }
                else if (it instanceof ProjectViewPsiImportBase psiImport) {
                    Import anImport = ImportFactory.from(myRoot, psiImport);
                    if (anImport instanceof Import.Resolved resolved) {
                        imports.add(resolved);
                        ProjectView imported = importFile(resolved.path());
                        if (imported != null) {
                            for (Map.Entry<ProjectViewSectionKey<?>, Object> entry : imported.getSections().entrySet()) {
                                mergeSection(sections, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    // Import.Unresolved, Import.OutsideOfProjectRoot -> Unit
                }
            }
            return new ProjectView(sections, List.copyOf(imports));
        }

        // consulo: upstream `import(path)`, `import` is a Java keyword
        private @Nullable ProjectView importFile(Path path) {
            Path key = PathUtil.toRealPathOrSelf(path);
            if (myInProgress.contains(key)) {
                return null;
            }
            if (myParsed.containsKey(key)) {
                return myParsed.get(key);
            }
            myInProgress.add(key);
            try {
                ProjectViewPsiFile psiFile = readLightPsiFile(myProject, path);
                ProjectView result = psiFile != null ? parse(psiFile, path) : null;
                myParsed.put(key, result);
                return result;
            }
            finally {
                myInProgress.remove(key);
            }
        }
    }

    // consulo: upstream private top-level functions of ProjectViewFactory.kt

    private static void mergeSection(
        Map<ProjectViewSectionKey<?>, Object> receiver,
        ProjectViewSectionKey<?> key,
        Object value
    ) {
        receiver.merge(key, value, (existing, newValue) -> {
            if (existing instanceof Collection<?> existingCollection && newValue instanceof Collection<?> newCollection) {
                List<Object> merged = new ArrayList<>(existingCollection);
                merged.addAll(newCollection);
                return merged;
            }
            else {
                return newValue;
            }
        });
    }

    private static @Nullable ProjectViewPsiFile readLightPsiFile(
        Project project,
        Path path
    ) {
        try {
            return createLightPsiFile(project, path.getFileName().toString(), readContent(path));
        }
        catch (Throwable e) {
            // consulo: upstream getOrHandleException rethrows control flow and cancellation exceptions
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }
            LOG.warn("Failed to parse imported project view file " + path + ". Skipping the import.", e);
            return null;
        }
    }

    /**
     * - the file already has a document - take its text, so that unsaved editor changes are not lost.
     * {@link FileDocumentManager#getCachedDocument} returns an already loaded document and never loads one.
     * - otherwise - read the file itself.
     * <p>
     * LoadTextUtil decodes the bytes, so the charset of the file is respected and its byte order mark is stripped.
     */
    private static CharSequence readContent(Path path) {
        VirtualFile virtualFile = PathUtil.findVirtualFile(path);
        Document document = virtualFile != null ? FileDocumentManager.getInstance().getCachedDocument(virtualFile) : null;
        if (document != null) {
            return document.getImmutableCharSequence();
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        }
        catch (IOException e) {
            // consulo: Kotlin does not have checked exceptions
            throw new UncheckedIOException(e);
        }
        // consulo: LoadTextUtil.getTextByBinaryPresentation(bytes, UTF_8) is internal in Consulo; the same steps with public API:
        // the charset is overridden by the byte order mark, the mark is skipped, line separators are converted to '\n'
        Charset bomCharset = CharsetToolkit.guessFromBOM(bytes);
        Charset charset = bomCharset != null ? bomCharset : StandardCharsets.UTF_8;
        return StringUtil.convertLineSeparators(CharsetToolkit.decodeString(bytes, charset));
    }

    private static ProjectViewPsiFile createLightPsiFile(Project project, String name, CharSequence content) {
        // consulo: upstream creates a ReadOnlyLightVirtualFile(name, ProjectViewLanguage, content) and calls
        // (PsiFileFactory as PsiFileFactoryImpl).trySetupPsiForFile(lightFile, ProjectViewLanguage, false, false)
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(name, ProjectViewLanguage.INSTANCE, content, false, false);
        if (psiFile instanceof ProjectViewPsiFile projectViewPsiFile) {
            return projectViewPsiFile;
        }
        throw new IllegalStateException(
            "Expected " + ProjectViewPsiFile.class.getSimpleName() + " for " + name + ", got " + psiFile.getClass().getName()
        );
    }

    private static @Nullable Pair<ProjectViewSection<?>, Object> toSectionWithValue(ProjectViewPsiSection receiver) {
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(receiver);
        if (section == null) {
            return null;
        }
        Object value = ProjectViewSectionType.read(receiver, section.getType());
        if (value == null) {
            return null;
        }
        return Pair.create(section, value);
    }

    private static final Logger LOG = Logger.getInstance(ProjectViewFactory.class);
}
