package org.jetbrains.bazel.languages.projectview.checker;

import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: the inspection message is a LocalizeValue (Consulo AnnotationHolder.newAnnotation takes a LocalizeValue)
public record ProjectViewProblem(
    LocalizeValue message,
    Severity severity,
    Type type,
    List<QuickFix> quickFixes,
    @Nullable PsiElement element
) {
    // consulo: Kotlin default parameter values (type = Type.Message, quickFixes = emptyList(), element = null)
    public ProjectViewProblem(LocalizeValue message, Severity severity) {
        this(message, severity, Type.Message, List.of(), null);
    }

    public enum Severity {
        Error, Warning, WeakWarning
    }

    public enum Type {
        Message, Deprecation
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
