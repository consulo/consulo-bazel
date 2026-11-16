package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ThreeState;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tells which targets from a given set run the code at a PSI element.
 * <p>
 * The entry point of a target is the class that the target starts.
 * For a test, it is the test class. For a binary, it is the main class.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelEntryPointMatcher {
    /**
     * Returns one result for each target in {@code targets}:
     * <ul>
     * <li>{@link ThreeState#YES} when the entry point contains {@code element}, or {@code element} contains the entry point.
     *   For example, {@code element} is a method of the test class, or the file of the test class.</li>
     * <li>{@link ThreeState#NO} when the entry point is a different class.</li>
     * <li>{@link ThreeState#UNSURE} when this matcher does not know the entry point.</li>
     * </ul>
     * <p>
     * {@link ThreeState#UNSURE} from a matcher does not change the result of another matcher.
     * When one matcher gives {@link ThreeState#YES} and another gives {@link ThreeState#NO}, the result is {@link ThreeState#UNSURE}.
     * A target that no matcher knows gets {@link ThreeState#UNSURE}.
     * <p>
     * The result depends on {@code element}, not only on its file.
     * A Kotlin file can hold more than one class, and a Java class can hold nested classes.
     * Each of these classes can be the entry point of a different target.
     */
    Map<Label, ThreeState> match(PsiElement element, List<BuildTargetInfo> targets);

    /**
     * Returns one result for each target in {@code targets} from all matchers.
     * The results combine as {@link BazelEntryPointMatcher#match} describes.
     */
    static Map<Label, ThreeState> matchAll(PsiElement element, List<BuildTargetInfo> targets) {
        Map<Label, ThreeState> matches = new LinkedHashMap<>();
        for (BuildTargetInfo it : targets) {
            matches.put(it.id(), ThreeState.UNSURE);
        }
        Application.get().getExtensionPoint(BazelEntryPointMatcher.class).forEachExtensionSafe(matcher -> {
            for (Map.Entry<Label, ThreeState> entry : matcher.match(element, targets).entrySet()) {
                matches.merge(entry.getKey(), entry.getValue(), BazelEntryPointMatcher::combine);
            }
        });
        return matches;
    }

    /**
     * Combines the results of two matchers.
     * {@link ThreeState#UNSURE} changes nothing, and {@link ThreeState#YES} with {@link ThreeState#NO} gives {@link ThreeState#UNSURE}.
     */
    // consulo: upstream private top-level extension function ThreeState.combine
    private static ThreeState combine(ThreeState receiver, ThreeState other) {
        return switch (receiver) {
            case UNSURE -> other;
            case YES, NO -> other == ThreeState.UNSURE ? receiver : receiver.merge(other);
        };
    }
}
