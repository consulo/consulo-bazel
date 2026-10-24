package org.jetbrains.bazel.languages.projectview;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

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

    public static @Nullable ProjectView fromOrNull(
        Project project,
        Path path
    ) {
        return fromOrNull(project, path, getRootDir(project).toNioPath());
    }

    public static @Nullable ProjectView fromOrNull(
        Project project,
        Path path,
        Path root
    ) {
        try {
            return from(project, path, root);
        }
        // In principle, project view parse should not throw.
        // However, in case it does, it might break some code paths e.g. project opening, so it's better to keep it in a try-catch block.
        catch (Throwable e) {
            // consulo: upstream getOrHandleException rethrows control flow and cancellation exceptions
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }
            LOG.error("Failed to load project view file at " + path + ".", e);
            return null;
        }
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

    public static ProjectView from(
        Project project,
        Path source
    ) {
        return from(project, source, getRootDir(project).toNioPath());
    }

    public static ProjectView from(
        Project project,
        Path source,
        Path root
    ) {
        return from(project, readContent(source), root, source);
    }

    public static ProjectView from(
        Project project,
        CharSequence content
    ) {
        return from(project, content, getRootDir(project).toNioPath());
    }

    public static ProjectView from(
        Project project,
        CharSequence content,
        Path root
    ) {
        return from(project, content, root, null);
    }

    public static ProjectView from(
        Project project,
        CharSequence content,
        Path root,
        @Nullable Path source
    ) {
        Path fileName = source != null ? source.getFileName() : null;
        String name = fileName != null ? fileName.toString() : Constants.DEFAULT_PROJECT_VIEW_FILE_NAME;
        return fromPsiFile(createLightPsiFile(project, name, content), source, root, source != null ? Set.of(source) : Set.of());
    }

    private static ProjectView fromPsiFile(
        ProjectViewPsiFile psi,
        @Nullable Path source,
        Path root,
        Set<Path> visited
    ) {
        List<Import> imports = new ArrayList<>();
        Map<ProjectViewSectionKey<?>, Object> sections = new LinkedHashMap<>();
        for (PsiElement it : psi.getChildren()) {
            if (it instanceof ProjectViewPsiSection psiSection) {
                Pair<ProjectViewSection<?>, Object> sectionWithValue = toSectionWithValue(psiSection);
                if (sectionWithValue == null) {
                    continue;
                }
                ProjectViewSection<?> section = sectionWithValue.getFirst();
                Object value = sectionWithValue.getSecond();
                mergeSection(sections, section.getKey(), value);
            }
            else if (it instanceof ProjectViewPsiImportBase psiImport) {
                Import anImport = ImportFactory.from(root, psiImport, source);
                imports.add(anImport);
                // skip if recursion detected
                if (anImport instanceof Import.Resolved resolved && !visited.contains(resolved.path())) {
                    parseImport(
                        psi.getProject(),
                        resolved,
                        sections,
                        root,
                        visited
                    );
                }
            }
        }
        return new ProjectView(sections, imports);
    }

    private static void mergeSection(
        Map<ProjectViewSectionKey<?>, Object> target,
        ProjectViewSectionKey<?> sectionKey,
        Object value
    ) {
        Object existing = target.get(sectionKey);
        if (existing instanceof Collection<?> existingCollection && value instanceof Collection<?> valueCollection) {
            List<Object> merged = new ArrayList<>(existingCollection);
            merged.addAll(valueCollection);
            target.put(sectionKey, merged);
        }
        else {
            target.put(sectionKey, value);
        }
    }

    private static void parseImport(
        Project project,
        Import.Resolved anImport,
        Map<ProjectViewSectionKey<?>, Object> into,
        Path root,
        Set<Path> visited
    ) {
        ProjectViewPsiFile psiFile;
        try {
            psiFile = createLightPsiFile(project, anImport.path().getFileName().toString(), readContent(anImport.path()));
        }
        catch (Throwable e) {
            // consulo: upstream getOrHandleException rethrows control flow and cancellation exceptions
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }
            LOG.warn("Failed to parse imported project view file " + anImport.path() + ". Skipping the import.", e);
            return;
        }
        Set<Path> newVisited = new LinkedHashSet<>(visited);
        newVisited.add(anImport.path());
        ProjectView otherProjectView = fromPsiFile(psiFile, anImport.path(), root, newVisited);
        for (Map.Entry<ProjectViewSectionKey<?>, Object> entry : otherProjectView.getSections().entrySet()) {
            mergeSection(into, entry.getKey(), entry.getValue());
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
