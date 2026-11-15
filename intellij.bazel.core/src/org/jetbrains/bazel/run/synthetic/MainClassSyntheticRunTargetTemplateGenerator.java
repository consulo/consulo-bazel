package org.jetbrains.bazel.run.synthetic;

import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

public abstract class MainClassSyntheticRunTargetTemplateGenerator implements SyntheticRunTargetTemplateGenerator {
    private static final String DEFAULT_TARGET_NAME = "synthetic_binary";

    @Override
    public boolean isSupported(BuildTarget target) {
        return true;
    }

    @Override
    public Label getSyntheticTargetLabel(BuildTarget original, PsiElement element) {
        String mainClass = getMainClassInternal(element);
        if (mainClass == null) {
            throw new IllegalStateException("failed to get main class");
        }
        String[] pkg = getTargetPath(original, mainClass);
        return SyntheticRunTargetUtils.getSyntheticTargetLabel(/* packageParts = */ pkg, /* targetName = */ DEFAULT_TARGET_NAME);
    }

    @Override
    public SyntheticRunTargetTemplateGenerator.Params getSyntheticParams(BuildTarget target, PsiElement element) {
        String mainClass = getMainClassInternal(element);
        if (mainClass == null) {
            throw new IllegalStateException("failed to get main class");
        }
        return new SyntheticRunTargetTemplateGenerator.Params(mainClass);
    }

    @Override
    public SyntheticRunTargetTemplate createSyntheticTemplate(
        BuildTarget target,
        SyntheticRunTargetTemplateGenerator.Params params
    ) {
        String mainClass = params.data();
        if (mainClass == null) {
            throw new IllegalStateException("failed to get main class");
        }
        String build = getBuildContent(target, DEFAULT_TARGET_NAME, mainClass);
        String[] pkg = getTargetPath(target, mainClass);
        return new SyntheticRunTargetTemplate(/* buildFileContent = */ build, /* buildFilePath = */ String.join("/", pkg));
    }

    public String[] getTargetPath(BuildTarget original, String mainClass) {
        return new String[]{
            SyntheticRunTargetUtils.escapeTargetLabel(BuildTarget.getId(original).toString()),
            SyntheticRunTargetUtils.escapeTargetLabel(mainClass),
        };
    }

    public @Nullable String getMainClassInternal(PsiElement receiver) {
        return ReadAction.<@Nullable String, RuntimeException>compute(() -> getMainClass(receiver));
    }

    protected abstract @Nullable String getMainClass(PsiElement element);

    protected abstract String getBuildContent(BuildTarget target, String syntheticTarget, String mainClass);
}
