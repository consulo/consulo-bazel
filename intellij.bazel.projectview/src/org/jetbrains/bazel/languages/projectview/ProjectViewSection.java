package org.jetbrains.bazel.languages.projectview;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class ProjectViewSection<T extends @Nullable Object> {
    private final ProjectViewSectionKey<T> myKey;
    private final ProjectViewSectionType<@NonNull T> myType;
    private final LocalizeValue myDocumentation;
    private final @Nullable Deprecation myDeprecation;

    public ProjectViewSection(
        ProjectViewSectionKey<T> key,
        ProjectViewSectionType<@NonNull T> type,
        LocalizeValue documentation
    ) {
        this(key, type, documentation, null);
    }

    public ProjectViewSection(
        ProjectViewSectionKey<T> key,
        ProjectViewSectionType<@NonNull T> type,
        LocalizeValue documentation,
        @Nullable Deprecation deprecation
    ) {
        myKey = key;
        myType = type;
        myDocumentation = documentation;
        myDeprecation = deprecation;
    }

    public ProjectViewSectionKey<T> getKey() {
        return myKey;
    }

    public ProjectViewSectionType<@NonNull T> getType() {
        return myType;
    }

    public LocalizeValue getDocumentation() {
        return myDocumentation;
    }

    public @Nullable Deprecation getDeprecation() {
        return myDeprecation;
    }

    public static class Deprecation {
        private final Function<String, LocalizeValue> myMessage;
        private final Level myLevel;
        private final List<ProjectViewProblem.QuickFix> myQuickFixes;

        /**
         * @param message the message for the section name
         */
        public Deprecation(Function<String, LocalizeValue> message) {
            this(message, Level.Warning);
        }

        public Deprecation(Function<String, LocalizeValue> message, Level level) {
            this(message, level, List.of());
        }

        public Deprecation(
            Function<String, LocalizeValue> message,
            Level level,
            List<ProjectViewProblem.QuickFix> quickFixes
        ) {
            myMessage = message;
            myLevel = level;
            myQuickFixes = quickFixes;
        }

        public Function<String, LocalizeValue> getMessage() {
            return myMessage;
        }

        public Level getLevel() {
            return myLevel;
        }

        public List<ProjectViewProblem.QuickFix> getQuickFixes() {
            return myQuickFixes;
        }

        public enum Level {
            Warning, Error
        }

        public static Deprecation withMergeQuickFix(ProjectViewSectionKey<?> targetKey) {
            return withMergeQuickFix(targetKey, Level.Warning);
        }

        public static Deprecation withMergeQuickFix(
            ProjectViewSectionKey<?> targetKey,
            Level level
        ) {
            return new Deprecation(
                sectionName -> BazelProjectViewLocalize.annotatorDeprecatedSectionWithReplacementWarning(sectionName, targetKey.getName()),
                level,
                List.of(new ProjectViewProblem.QuickFix.MergeIntoSection(targetKey))
            );
        }
    }

    // consulo: upstream caches the map on the extension point with computeIfAbsent(ProjectViewSection::class.java)
    private static final ExtensionPointCacheKey<ProjectViewSectionProvider, Map<String, ProjectViewSection<?>>> BY_NAME =
        ExtensionPointCacheKey.create("ProjectViewSection.byName", walker -> {
            Map<String, ProjectViewSection<?>> map = new LinkedHashMap<>();
            walker.walk(provider -> provider.getSections().forEach(it -> map.putIfAbsent(it.getKey().getName(), it)));
            return map;
        });

    private static Map<String, ProjectViewSection<?>> getByName() {
        return ProjectViewSectionProvider.getEpName().getOrBuildCache(BY_NAME);
    }

    public static Stream<ProjectViewSection<?>> getAllRegistered() {
        return getByName().values().stream();
    }

    public static @Nullable ProjectViewSection<?> findByName(String name) {
        return getByName().get(name);
    }

    public static @Nullable ProjectViewSection<?> findByPsi(PsiElement psi) {
        ProjectViewPsiSection section = PsiTreeUtil.getParentOfType(psi, ProjectViewPsiSection.class, false);
        if (section == null) {
            return null;
        }
        return findByName(ProjectViewPsiSection.sectionName(section));
    }
}
