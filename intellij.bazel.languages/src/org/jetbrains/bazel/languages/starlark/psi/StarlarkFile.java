package org.jetbrains.bazel.languages.starlark.psi;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.function.Processor;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarlarkFile extends PsiFileBase implements StarlarkElement {
    public StarlarkFile(FileViewProvider viewProvider) {
        super(viewProvider, StarlarkLanguage.INSTANCE);
    }

    @Override
    public GlobalSearchScope getUseScope() {
        return StarlarkSearchScope.starlarkProjectScope(getProject());
    }

    @Override
    public FileType getFileType() {
        return StarlarkFileType.INSTANCE;
    }

    public boolean isBuildFile() {
        return this.getBazelFileType() == BazelFileType.BUILD;
    }

    public BazelFileType getBazelFileType() {
        return BazelFileType.ofFileName(getName());
    }

    public List<StarlarkCallExpression> getTargetRules() {
        return Arrays.stream(findChildrenByClass(StarlarkExpressionStatement.class))
            .map(StarlarkExpressionStatement::callExpressionOrNull)
            .filter(Objects::nonNull)
            .filter(it -> it.isRuleTarget())
            .toList();
    }

    public @Nullable StarlarkCallExpression findTargetRule(String targetName) {
        StarlarkFile file = this;
        Map<String, StarlarkCallExpression> rulesByName = LanguageCachedValueUtil.getCachedValue(file, () -> {
            Map<String, StarlarkCallExpression> result = new HashMap<>();
            for (StarlarkCallExpression rule : file.getTargetRules()) {
                String name = rule.getNameAttributeValue();
                if (name == null) {
                    continue;
                }
                result.putIfAbsent(name, rule);
            }
            return CachedValueProvider.Result.create(result, file);
        });
        return rulesByName.get(targetName);
    }

    public boolean searchInLoads(Processor<StarlarkElement> processor) {
        return Arrays.stream(findChildrenByClass(StarlarkLoadStatement.class))
            .flatMap(it -> it.getLoadedSymbolsPsi().stream())
            .allMatch(processor::process);
    }

    public boolean searchInFunctionCalls(Processor<StarlarkElement> processor) {
        return Arrays.stream(findChildrenByClass(StarlarkExpressionStatement.class))
            .map(StarlarkExpressionStatement::callExpressionOrNull)
            .filter(Objects::nonNull)
            .allMatch(processor::process);
    }
}
