package org.jetbrains.bazel.run.synthetic;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: upstream registers the generators through a LanguageExtension (LanguageExtensionPoint with a `language` attribute);
// Consulo language extensions return their language from getLanguage()
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SyntheticRunTargetTemplateGenerator extends LanguageExtension {
    ExtensionPointCacheKey<SyntheticRunTargetTemplateGenerator, ByLanguageValue<List<SyntheticRunTargetTemplateGenerator>>> KEY =
        ExtensionPointCacheKey.create("SyntheticRunTargetTemplateGenerator", LanguageOneToMany.build(false));

    boolean isSupported(BuildTarget target);

    Label getSyntheticTargetLabel(BuildTarget original, PsiElement element);

    Params getSyntheticParams(BuildTarget target, PsiElement element);

    SyntheticRunTargetTemplate createSyntheticTemplate(BuildTarget target, Params params);

    // consulo: upstream `ep.allForLanguage(language)`
    static List<SyntheticRunTargetTemplateGenerator> allForLanguage(Language language) {
        return Application.get().getExtensionPoint(SyntheticRunTargetTemplateGenerator.class).getOrBuildCache(KEY).requiredGet(language);
    }

    /**
     * Just one generator, because showing several gutters for the same target doesn't make sense
     */
    static @Nullable SyntheticRunTargetTemplateGenerator getTemplateGenerator(BuildTarget target, Language language) {
        for (SyntheticRunTargetTemplateGenerator it : allForLanguage(language)) {
            if (it.isSupported(target)) {
                return it;
            }
        }
        return null;
    }

    record Params(@Nullable String data) {
    }
}
