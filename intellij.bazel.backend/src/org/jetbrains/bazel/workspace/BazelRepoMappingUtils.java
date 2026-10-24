package org.jetbrains.bazel.languages.starlark.repomapping;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.RepoMappingDisabled;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AllPackagesBeneath;
import org.jetbrains.bazel.label.Label.AllRuleTargets;
import org.jetbrains.bazel.label.Label.AllRuleTargetsAndFiles;
import org.jetbrains.bazel.label.Label.AmbiguousEmptyTarget;
import org.jetbrains.bazel.label.Label.Apparent;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.Main;
import org.jetbrains.bazel.label.Label.Package;
import org.jetbrains.bazel.label.Label.RepoType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bazel.label.Label.TargetType;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewTargets;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class BazelRepoMappingUtils {
    private BazelRepoMappingUtils() {
    }

    public static @Nullable VirtualFile findContainingBazelRepo(VirtualFile path) {
        for (VirtualFile repoDir = path; repoDir != null; repoDir = repoDir.getParent()) {
            for (String workspaceFileName : Constants.WORKSPACE_FILE_NAMES) {
                // consulo: Kotlin VirtualFile.isFile
                VirtualFile child = repoDir.findChild(workspaceFileName);
                if (child != null && child.isValid() && !child.isDirectory()) {
                    return repoDir;
                }
            }
        }
        return null;
    }

    // consulo: overload for the upstream default argument (targetName = null)
    public static @Nullable ResolvedLabel calculateLabel(
        Project project,
        VirtualFile buildFile
    ) {
        return calculateLabel(project, buildFile, null);
    }

    public static @Nullable ResolvedLabel calculateLabel(
        Project project,
        VirtualFile buildFile,
        @Nullable String targetName
    ) {
        VirtualFile packageDirectory = buildFile.getParent();
        if (packageDirectory == null) {
            return null;
        }
        ResolvedLabel packageLabel = calculatePackageLabel(project, packageDirectory);
        if (packageLabel == null) {
            return null;
        }
        if (targetName != null) {
            return new ResolvedLabel(packageLabel.repo(), packageLabel.packagePath(), new SingleTarget(targetName));
        }
        else {
            return packageLabel;
        }
    }

    public static @Nullable ResolvedLabel calculateWildcardLabel(
        Project project,
        VirtualFile packageDirectory
    ) {
        ResolvedLabel packageLabel = calculatePackageLabel(project, packageDirectory);
        if (packageLabel == null) {
            return null;
        }
        return new ResolvedLabel(
            packageLabel.repo(),
            new AllPackagesBeneath(packageLabel.packagePath().pathSegments()),
            AllRuleTargets.INSTANCE
        );
    }

    private static @Nullable ResolvedLabel calculatePackageLabel(
        Project project,
        VirtualFile packageDirectory
    ) {
        VirtualFile containingRepoDirectory = findContainingBazelRepo(packageDirectory);
        if (containingRepoDirectory == null) {
            return null;
        }
        Path containingRepoPath = Path.of(containingRepoDirectory.getPath());
        String containingCanonicalRepoName = null;
        for (Map.Entry<String, Path> entry : BazelRepoMappingProvider.getCanonicalRepoNameToPath(project).entrySet()) {
            if (entry.getValue().equals(containingRepoPath)) {
                containingCanonicalRepoName = entry.getKey();
                break;
            }
        }
        if (containingCanonicalRepoName == null) {
            return null;
        }

        String relativeTargetBaseDirectory = VirtualFileUtil.getRelativePath(packageDirectory, containingRepoDirectory, '/');
        if (relativeTargetBaseDirectory == null) {
            return null;
        }
        return new ResolvedLabel(
            Canonical.createCanonicalOrMain(containingCanonicalRepoName),
            new Package(Arrays.stream(relativeTargetBaseDirectory.split("/")).filter(it -> !it.isEmpty()).toList()),
            AmbiguousEmptyTarget.INSTANCE
        );
    }

    /**
     * We should show apparent labels in the UI, where possible, to avoid confusing the user with labels containing symbols
     * such as `~` or `+`.
     * If conversion to an apparent label fails, fall back to the original label.
     */
    public static Label toApparentLabelOrThis(Label receiver, Project project) {
        ResolvedLabel apparentLabel = toApparentLabel(receiver, project);
        return apparentLabel != null ? apparentLabel : receiver;
    }

    public static Label toApparentLabelOrThis(Label receiver, RepoMapping repoMapping) {
        ResolvedLabel apparentLabel = toApparentLabel(receiver, repoMapping);
        return apparentLabel != null ? apparentLabel : receiver;
    }

    /**
     * Converts this {@link Label}'s repository either to {@link Apparent} or to {@link Main}.
     */
    public static @Nullable ResolvedLabel toApparentLabel(Label receiver, Project project) {
        if (!(receiver instanceof ResolvedLabel resolvedLabel)) {
            return null;
        }
        if (!(resolvedLabel.repo() instanceof Canonical canonical)) {
            return resolvedLabel;
        }
        String apparentRepoName = RepoMapping.getCanonicalRepoNameToApparentName(BazelRepoMappingProvider.getRepoMapping(project))
            .get(canonical.repoName());
        if (apparentRepoName == null) {
            return null;
        }
        return new ResolvedLabel(new Apparent(apparentRepoName), resolvedLabel.packagePath(), resolvedLabel.target());
    }

    public static @Nullable ResolvedLabel toApparentLabel(Label receiver, RepoMapping repoMapping) {
        if (!(receiver instanceof ResolvedLabel resolvedLabel)) {
            return null;
        }
        if (!(resolvedLabel.repo() instanceof Canonical canonical)) {
            return resolvedLabel;
        }
        String apparentRepoName = switch (repoMapping) {
            case BzlmodRepoMapping bzlmodRepoMapping -> bzlmodRepoMapping.getCanonicalRepoNameToApparentName().get(canonical.repoName());
            case RepoMappingDisabled ignored -> null;
        };
        if (apparentRepoName == null) {
            return null;
        }
        return new ResolvedLabel(new Apparent(apparentRepoName), resolvedLabel.packagePath(), resolvedLabel.target());
    }

    /**
     * Converts this {@link Label}'s repository either to {@link Canonical} or to {@link Main}.
     */
    public static @Nullable ResolvedLabel toCanonicalLabel(Label receiver, RepoMapping repoMapping) {
        if (!(receiver instanceof ResolvedLabel resolvedLabel)) {
            return null;
        }
        RepoType repo;
        if (!(resolvedLabel.repo() instanceof Apparent apparent)) {
            repo = resolvedLabel.repo();
        }
        else if (repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping) {
            String canonicalRepoName = bzlmodRepoMapping.getApparentRepoNameToCanonicalName().get(apparent.repoName());
            if (canonicalRepoName == null) {
                return null;
            }
            repo = Canonical.createCanonicalOrMain(canonicalRepoName);
        }
        else {
            return null;
        }
        TargetType target;
        if (resolvedLabel.target() instanceof AllRuleTargets) {
            target = resolvedLabel.target();
        }
        else if (resolvedLabel.target() instanceof AllRuleTargetsAndFiles) {
            target = resolvedLabel.target();
        }
        else {
            target = singleTarget(resolvedLabel);
            if (target == null) {
                return null;
            }
        }
        return new ResolvedLabel(repo, resolvedLabel.packagePath(), target);
    }

    /**
     * Converts this {@link Label}'s repository either to {@link Canonical} or to {@link Main}.
     */
    public static @Nullable ResolvedLabel toCanonicalLabel(Label receiver, Project project) {
        if (!(receiver instanceof ResolvedLabel resolvedLabel)) {
            return null;
        }
        RepoType repo;
        if (!(resolvedLabel.repo() instanceof Apparent apparent)) {
            repo = resolvedLabel.repo();
        }
        else {
            String canonicalRepoName = RepoMapping.getApparentRepoNameToCanonicalName(BazelRepoMappingProvider.getRepoMapping(project))
                .get(apparent.repoName());
            if (canonicalRepoName == null) {
                return null;
            }
            repo = Canonical.createCanonicalOrMain(canonicalRepoName);
        }
        TargetType target;
        if (resolvedLabel.target() instanceof AllRuleTargets) {
            target = resolvedLabel.target();
        }
        else if (resolvedLabel.target() instanceof AllRuleTargetsAndFiles) {
            target = resolvedLabel.target();
        }
        else {
            target = singleTarget(resolvedLabel);
            if (target == null) {
                return null;
            }
        }
        return new ResolvedLabel(repo, resolvedLabel.packagePath(), target);
    }

    public static @Nullable ResolvedLabel toCanonicalLabelOrThis(Label receiver, Project project) {
        ResolvedLabel canonicalLabel = toCanonicalLabel(receiver, project);
        if (canonicalLabel != null) {
            return canonicalLabel;
        }
        return receiver instanceof ResolvedLabel resolvedLabel ? resolvedLabel : null;
    }

    public static @Nullable ResolvedLabel toCanonicalLabelOrThis(Label receiver, RepoMapping repoMapping) {
        ResolvedLabel canonicalLabel = toCanonicalLabel(receiver, repoMapping);
        if (canonicalLabel != null) {
            return canonicalLabel;
        }
        return receiver instanceof ResolvedLabel resolvedLabel ? resolvedLabel : null;
    }

    public static @Nullable SingleTarget singleTarget(Label receiver) {
        TargetType oldTarget = receiver.target();
        if (oldTarget instanceof AmbiguousEmptyTarget) {
            List<String> pathSegments = receiver.packagePath().pathSegments();
            return pathSegments.isEmpty() ? null : new SingleTarget(pathSegments.get(pathSegments.size() - 1));
        }
        else if (oldTarget instanceof SingleTarget singleTarget) {
            return singleTarget;
        }
        else {
            return null;
        }
    }

    public static String toShortString(Label receiver, Project project) {
        return toShortString(toApparentLabelOrThis(receiver, project));
    }

    public static String toShortString(Label receiver, RepoMapping repoMapping) {
        return toShortString(toApparentLabelOrThis(receiver, repoMapping));
    }


    private static String toShortString(Label receiver) {
        Label label = receiver;
        if (!(label instanceof ResolvedLabel resolvedLabel)) {
            return label.toString();
        }

        String repoPart = !(resolvedLabel.repo() instanceof Main) ? resolvedLabel.repo().toString() : "";
        String packagePart = resolvedLabel.packagePath().toString();
        String targetPart;
        if (resolvedLabel.target() instanceof AmbiguousEmptyTarget) {
            targetPart = "";
        }
        else if (resolvedLabel.target() instanceof SingleTarget singleTarget &&
            resolvedLabel.packagePath() instanceof Package pkg &&
            singleTarget.targetName().equals(pkg.name())) {
            targetPart = "";
        }
        else {
            targetPart = ":" + resolvedLabel.target();
        }
        return repoPart + "//" + packagePart + targetPart;
    }

    /**
     * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
     * belong to them.
     */
    public static List<String> getExternalRepositoriesTreatedAsInternal(ProjectView receiver) {
        return ProjectViewTargets.getTargets(receiver)
            .stream()
            .filter(ExcludableValue::isIncluded)
            .map(excludableValue -> Label.assumeResolved(excludableValue.value()).repo().repoName())
            .filter(repoName -> !repoName.isEmpty())
            .distinct()
            .toList();
    }
}
