package org.jetbrains.bazel.label;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import consulo.util.interner.Interner;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// consulo: holder for upstream private top-level constants and the companion's mutable parser (interfaces cannot hold them)
final class LabelInternals {
    static final String SYNTHETIC_TAG = "[synthetic]";
    static final String WILDCARD = "*";
    static final String ALL_TARGETS = "all-targets";
    static final String ALL = "all";
    static final String PATH_SEGMENT_SEPARATOR = "/";
    static final String ALL_PACKAGES_BENEATH = "...";

    static Label.LabelParser parser = new Label.LabelParser();

    private LabelInternals() {
    }
}

/**
 * Represents a Bazel label.
 * See https://bazel.build/concepts/labels
 */
public sealed interface Label extends Comparable<Label> permits Label.ResolvedLabel, Label.SyntheticLabel, Label.RelativeLabel {
    /**
     * See https://bazel.build/run/build#specifying-build-targets
     */
    sealed interface TargetType permits SingleTarget, AllRuleTargetsAndFiles, AllRuleTargets, AmbiguousEmptyTarget {
    }

    record SingleTarget(String targetName) implements TargetType {
        @Override
        public String toString() {
            return targetName;
        }
    }

    /**
     * Either :* or :all-targets (meaning all rules and files)
     */
    final class AllRuleTargetsAndFiles implements TargetType {
        public static final AllRuleTargetsAndFiles INSTANCE = new AllRuleTargetsAndFiles();

        private AllRuleTargetsAndFiles() {
        }

        @Override
        public String toString() {
            return "*";
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof AllRuleTargetsAndFiles;
        }

        @Override
        public int hashCode() {
            return AllRuleTargetsAndFiles.class.getName().hashCode();
        }
    }

    /**
     * All rule targets in the given package (excluding files)
     */
    final class AllRuleTargets implements TargetType {
        public static final AllRuleTargets INSTANCE = new AllRuleTargets();

        private AllRuleTargets() {
        }

        @Override
        public String toString() {
            return "all";
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof AllRuleTargets;
        }

        @Override
        public int hashCode() {
            return AllRuleTargets.class.getName().hashCode();
        }
    }

    /**
     * Label `//src/Hello.java` can mean different things:
     * - target `Hello.java` in the package `src` (same as `//src:Hello.java`)
     * - target `Hello.java` in the package `src/Hello.java` (same as `//src/Hello.java:Hello.java`)
     * - file `src/Hello.java` in the root package (same as `//:src/Hello.java`)
     * <p>
     * Unfortunately the meaning depends on the package structure (i.e. which of the folders `src` and `src/Hello.java` exist and whether
     * there are BUILD files in them).
     * <p>
     * Bazel docs say:
     * > Bazel allows a slash to be used instead of the colon required by the label syntax;
     * > this is often convenient when using Bash filename expansion. For example, foo/bar/wiz is equivalent to
     * > //foo/bar:wiz (if there is a package foo/bar) or to //foo:bar/wiz (if there is a package foo).
     */
    final class AmbiguousEmptyTarget implements TargetType {
        public static final AmbiguousEmptyTarget INSTANCE = new AmbiguousEmptyTarget();

        private AmbiguousEmptyTarget() {
        }

        @Override
        public String toString() {
            return "";
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof AmbiguousEmptyTarget;
        }

        @Override
        public int hashCode() {
            return AmbiguousEmptyTarget.class.getName().hashCode();
        }
    }

    sealed interface PackageType permits Package, AllPackagesBeneath {
        List<String> pathSegments();
    }

    static Path toPath(PackageType receiver) {
        return Path.of(String.join(LabelInternals.PATH_SEGMENT_SEPARATOR, receiver.pathSegments()));
    }

    record Package(List<String> pathSegments) implements PackageType {
        @Override
        public String toString() {
            return String.join(LabelInternals.PATH_SEGMENT_SEPARATOR, pathSegments);
        }

        public Package parent() {
            return new Package(List.copyOf(pathSegments.subList(0, Math.max(pathSegments.size() - 1, 0))));
        }

        public String name() {
            return pathSegments.isEmpty() ? "" : pathSegments.get(pathSegments.size() - 1);
        }
    }

    record AllPackagesBeneath(List<String> pathSegments) implements PackageType {
        @Override
        public String toString() {
            if (pathSegments.isEmpty()) {
                return LabelInternals.ALL_PACKAGES_BENEATH;
            }
            else {
                return String.join(LabelInternals.PATH_SEGMENT_SEPARATOR, pathSegments)
                    + LabelInternals.PATH_SEGMENT_SEPARATOR + LabelInternals.ALL_PACKAGES_BENEATH;
            }
        }
    }

    sealed interface RepoType permits Main, Canonical, Apparent {
        String repoName();
    }

    /**
     * Targets in the main workspace are special-cased because they can be referred to
     * using both syntaxes and there's no need to use a repository mapping to resolve the label.
     */
    final class Main implements RepoType {
        public static final Main INSTANCE = new Main();

        private Main() {
        }

        @Override
        public String repoName() {
            return "";
        }

        @Override
        public String toString() {
            return "@";
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Main;
        }

        @Override
        public int hashCode() {
            return Main.class.getName().hashCode();
        }
    }

    /**
     * See https://bazel.build/external/overview#canonical-repo-name
     */
    // consulo: upstream constructor is internal, a record constructor cannot be less visible than the record
    record Canonical(String repoName) implements RepoType {
        public Canonical {
            if (repoName.isEmpty()) {
                throw new IllegalArgumentException("Failed requirement.");
            }
        }

        @Override
        public String toString() {
            return "@@" + repoName;
        }

        public static RepoType createCanonicalOrMain(String repoName) {
            return repoName.isEmpty() ? Main.INSTANCE : new Canonical(repoName);
        }
    }

    /**
     * See https://bazel.build/external/overview#apparent-repo-name
     */
    record Apparent(String repoName) implements RepoType {
        @Override
        public String toString() {
            return "@" + repoName;
        }
    }

    record ResolvedLabel(RepoType repo, PackageType packagePath, TargetType target) implements Label {
        public String getRepoName() {
            return repo.repoName();
        }

        @Override
        public String toString() {
            return repo + "//" + joinPackagePathAndTarget(packagePath, target);
        }
    }

    /**
     * Synthetic label is a label of a target which is not present in the Bazel target graph.
     */
    record SyntheticLabel(TargetType target) implements Label {
        private static final PackageType EMPTY_PACKAGE_PATH = new Package(List.of());

        @Override
        public PackageType packagePath() {
            return EMPTY_PACKAGE_PATH;
        }

        @Override
        public String toString() {
            return target + LabelInternals.SYNTHETIC_TAG;
        }
    }

    record RelativeLabel(PackageType packagePath, TargetType target) implements Label {
        @Override
        public String toString() {
            return joinPackagePathAndTarget(packagePath, target);
        }

        public ResolvedLabel resolve(ResolvedLabel base) {
            RepoType repo = base.repo();
            List<String> mergedPath = new ArrayList<>(base.packagePath().pathSegments());
            mergedPath.addAll(this.packagePath.pathSegments());
            PackageType packagePath =
                switch (base.packagePath()) {
                    case Package ignored ->
                        switch (this.packagePath) {
                            case Package ignored2 -> new Package(mergedPath);
                            case AllPackagesBeneath ignored2 -> new AllPackagesBeneath(mergedPath);
                        };
                    case AllPackagesBeneath ignored ->
                        throw new IllegalStateException("Cannot resolve a relative label with respect to a wildcard package");
                };
            TargetType target = this.target;
            return new ResolvedLabel(repo, packagePath, target);
        }
    }

    PackageType packagePath();

    TargetType target();

    default String getTargetName() {
        TargetType target = target();
        if (target instanceof AmbiguousEmptyTarget) {
            PackageType packagePath = packagePath();
            return switch (packagePath) {
                case Package pkg -> pkg.name();
                case AllPackagesBeneath ignored -> LabelInternals.ALL_PACKAGES_BENEATH;
            };
        }
        else {
            return target.toString();
        }
    }

    default boolean isMainWorkspace() {
        return (this instanceof ResolvedLabel resolvedLabel && resolvedLabel.repo() instanceof Main) || this instanceof SyntheticLabel;
    }

    default boolean isRelative() {
        return this instanceof RelativeLabel;
    }

    default boolean isSynthetic() {
        return this instanceof SyntheticLabel;
    }

    default boolean isWildcard() {
        return target() instanceof AllRuleTargetsAndFiles
            || target() instanceof AllRuleTargets
            || packagePath() instanceof AllPackagesBeneath;
    }

    default boolean isRecursive() {
        return packagePath() instanceof AllPackagesBeneath;
    }

    default boolean isApparent() {
        return this instanceof ResolvedLabel resolvedLabel && resolvedLabel.repo() instanceof Apparent;
    }

    @Override
    default int compareTo(Label other) {
        return toString().compareTo(other.toString());
    }

    static Label synthetic(String targetName) {
        String name = targetName.endsWith(LabelInternals.SYNTHETIC_TAG)
            ? targetName.substring(0, targetName.length() - LabelInternals.SYNTHETIC_TAG.length())
            : targetName;
        return new SyntheticLabel(new SingleTarget(name));
    }

    static void cleanInternCache() {
        LabelInternals.parser = new LabelParser();
    }

    static Label parse(String value) {
        return LabelInternals.parser.parse(value);
    }

    static @Nullable Label parseOrNull(@Nullable String value) {
        try {
            return value == null ? null : parse(value);
        }
        catch (Exception e) {
            return null;
        }
    }

    class LabelParser {
        private final Interner<String> myPartsIntern = Interner.createStringInterner();
        private final ConcurrentHashMap<String, Label> myLabelIntern = new ConcurrentHashMap<>();

        public Label parse(String value) {
            return myLabelIntern.computeIfAbsent(value, it -> parseImpl(value));
        }

        private Label parseImpl(String value) {
            if (value.endsWith(LabelInternals.SYNTHETIC_TAG)) {
                return synthetic(value);
            }
            String normalized = trimStartAt(value.strip());
            int repoSeparatorIndex = normalized.indexOf("//");
            String repoName = repoSeparatorIndex < 0 ? "" : normalized.substring(0, repoSeparatorIndex);
            String pathAndName = repoSeparatorIndex < 0 ? normalized : normalized.substring(repoSeparatorIndex + 2);
            int colonIndex = pathAndName.indexOf(':');
            String packagePath = colonIndex < 0 ? pathAndName : pathAndName.substring(0, colonIndex);

            // Package names are allowed to have spaces, but target names are not: https://bazel.build/concepts/labels
            String targetName;
            if (colonIndex < 0) {
                int lastSeparatorIndex = packagePath.lastIndexOf(LabelInternals.PATH_SEGMENT_SEPARATOR);
                targetName = lastSeparatorIndex < 0 ? packagePath : packagePath.substring(lastSeparatorIndex + 1);
            }
            else {
                targetName = pathAndName.substring(colonIndex + 1);
            }
            if (targetName.contains(" ")) {
                throw new IllegalArgumentException("Label \"" + normalized + "\" is not allowed to have whitespaces in target name");
            }

            List<String> packageSegments = new ArrayList<>();
            for (String it : packagePath.split(LabelInternals.PATH_SEGMENT_SEPARATOR, -1)) {
                if (!it.isEmpty()) {
                    packageSegments.add(intern(it));
                }
            }
            PackageType packageType;
            if (!packageSegments.isEmpty() && packageSegments.get(packageSegments.size() - 1).equals(LabelInternals.ALL_PACKAGES_BENEATH)) {
                packageType = new AllPackagesBeneath(List.copyOf(packageSegments.subList(0, packageSegments.size() - 1)));
            }
            else {
                packageType = new Package(List.copyOf(packageSegments));
            }

            TargetType target;
            if (targetName.equals(LabelInternals.WILDCARD)) {
                target = AllRuleTargetsAndFiles.INSTANCE;
            }
            else if (targetName.equals(LabelInternals.ALL_TARGETS)) {
                target = AllRuleTargetsAndFiles.INSTANCE;
            }
            else if (targetName.equals(LabelInternals.ALL)) {
                target = AllRuleTargets.INSTANCE;
            }
            else if (targetName.equals(LabelInternals.ALL_PACKAGES_BENEATH)) {
                target = AllRuleTargets.INSTANCE; // Special case for //...:...
            }
            else if (pathAndName.contains(":")) {
                target = new SingleTarget(targetName);
            }
            else {
                target = AmbiguousEmptyTarget.INSTANCE;
            }

            if (packageType instanceof AllPackagesBeneath && target instanceof SingleTarget) {
                throw new IllegalArgumentException("Cannot have a single target in a wildcard package");
            }

            if (!value.contains("//")) {
                return new RelativeLabel(packageType, target);
            }

            RepoType repo;
            if (repoName.isEmpty()) {
                repo = Main.INSTANCE;
            }
            else if (value.startsWith("@@")) {
                repo = new Canonical(intern(repoName));
            }
            else {
                repo = new Apparent(intern(repoName));
            }

            return new ResolvedLabel(repo, packageType, target);
        }

        private String intern(String receiver) {
            synchronized (myPartsIntern) {
                return myPartsIntern.intern(receiver);
            }
        }

        // consulo: Kotlin String.trimStart('@')
        private static String trimStartAt(String value) {
            int index = 0;
            while (index < value.length() && value.charAt(index) == '@') {
                index++;
            }
            return value.substring(index);
        }
    }

    static @Nullable RelativeLabel asRelative(Label receiver) {
        return receiver instanceof RelativeLabel relativeLabel ? relativeLabel : null;
    }

    static ResolvedLabel assumeResolved(Label receiver) {
        return switch (receiver) {
            case ResolvedLabel resolvedLabel -> resolvedLabel;
            case SyntheticLabel ignored -> throw new IllegalStateException("Cannot resolve synthetic label " + receiver);
            case RelativeLabel relativeLabel ->
                relativeLabel.resolve(new ResolvedLabel(Main.INSTANCE, new Package(List.of()), new SingleTarget("")));
        };
    }

    private static String joinPackagePathAndTarget(PackageType packagePath, TargetType target) {
        if (target instanceof AmbiguousEmptyTarget) {
            return packagePath.toString();
        }
        else {
            return packagePath + ":" + target;
        }
    }

    static Label label(IntellijIdeInfo.TargetIdeInfo receiver) {
        return Label.parse(receiver.getKey().getLabel());
    }

    static Label label(IntellijIdeInfo.Dependency receiver) {
        return Label.parse(receiver.getTarget().getLabel());
    }
}
