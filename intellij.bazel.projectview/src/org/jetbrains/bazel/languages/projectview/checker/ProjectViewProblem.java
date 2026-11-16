package org.jetbrains.bazel.languages.projectview.checker;

import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

// consulo: the inspection message is a LocalizeValue (Consulo AnnotationHolder.newAnnotation takes a LocalizeValue)
public record ProjectViewProblem(
    Id id,
    LocalizeValue message,
    Severity severity,
    List<QuickFix> quickFixes,
    @Nullable PsiElement element,
    @Nullable Path sourcePath
) {
    // consulo: overloads for the upstream default arguments (quickFixes = emptyList(), element = null, sourcePath = null)
    public ProjectViewProblem(Id id, LocalizeValue message, Severity severity) {
        this(id, message, severity, List.of(), null, null);
    }

    public ProjectViewProblem(Id id, LocalizeValue message, Severity severity, @Nullable PsiElement element) {
        this(id, message, severity, List.of(), element, null);
    }

    public ProjectViewProblem(Id id, LocalizeValue message, Severity severity, List<QuickFix> quickFixes, @Nullable PsiElement element) {
        this(id, message, severity, quickFixes, element, null);
    }

    // consulo: Kotlin data class copy(element = ...)
    public ProjectViewProblem withElement(@Nullable PsiElement element) {
        return new ProjectViewProblem(id, message, severity, quickFixes, element, sourcePath);
    }

    // consulo: Kotlin data class copy(sourcePath = ...)
    public ProjectViewProblem withSourcePath(@Nullable Path sourcePath) {
        return new ProjectViewProblem(id, message, severity, quickFixes, element, sourcePath);
    }

    /**
     * The entries go from the lowest to the highest severity.
     */
    public enum Severity {
        WeakWarning, Warning, Error
    }

    /**
     * {@code value} names the problem in a suppression comment, so it has only letters, digits and underscores.
     * Do not change a value, because the comments in project view files use it.
     */
    public record Id(String value) {
        /**
         * The pattern of {@code value}. The suppression comments build their patterns from it.
         */
        static final String PATTERN = "\\w+";
        private static final Pattern regex = Pattern.compile(PATTERN);

        public Id {
            if (!regex.matcher(value).matches()) {
                throw new IllegalArgumentException("A problem ID has only letters, digits and underscores: '" + value + "'");
            }
        }

        public static final Id UnsupportedSection = new Id("unsupported_section");
        public static final Id InvalidValue = new Id("invalid_value");
        public static final Id MissingPath = new Id("missing_path");
        public static final Id UnknownFlag = new Id("unknown_flag");
        public static final Id FlagNotAllowed = new Id("flag_not_allowed");
        public static final Id UnresolvedImport = new Id("unresolved_import");
        public static final Id Deprecation = new Id("deprecation");
    }

    /**
     * Quick fix descriptor which will be turned into appropriate action.
     */
    public sealed interface QuickFix {

        /**
         * Merge the deprecated section values to the section with {@code targetSectionKey}.
         * If target section does not exist, it will be created.
         * <p>
         * Can only be applied to section PSI element.
         */
        record MergeIntoSection(ProjectViewSectionKey<?> targetSectionKey) implements QuickFix {
        }

        /**
         * Special quick fix to replace deprecated `index_...` sections with `index`.
         */
        final class ReplaceWithIndex implements QuickFix {
            public static final ReplaceWithIndex INSTANCE = new ReplaceWithIndex();

            private ReplaceWithIndex() {
            }

            @Override
            public String toString() {
                return "ReplaceWithIndex";
            }
        }
    }
}
