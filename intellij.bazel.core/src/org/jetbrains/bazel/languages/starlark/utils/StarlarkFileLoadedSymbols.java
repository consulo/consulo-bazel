package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkQualifiableExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStaticValueUtils.StarlarkStaticValueAnalyzer;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStaticValueUtils.StarlarkStaticValueKind;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StarlarkFileLoadedSymbols {
    // consulo: upstream top-level class StarlarkLoadedSymbol of the same Kotlin file
    public static class StarlarkLoadedSymbol {
        private final PsiElement myElement;
        private final String myLocalName;
        private final StarlarkLoadValue myLoadValue;
        private final LazyValue<@Nullable StarlarkStaticValueKind> myStaticValueKind;

        public StarlarkLoadedSymbol(
            PsiElement element,
            String localName,
            StarlarkLoadValue loadValue
        ) {
            myElement = element;
            myLocalName = localName;
            myLoadValue = loadValue;
            // consulo: upstream `by lazy` property, initialized here because it reads the load value
            myStaticValueKind = LazyValue.nullable(() -> {
                StarlarkStringLiteralExpression loadValueExpression = loadValue.getLoadValueExpression();
                PsiReference reference = loadValueExpression == null ? null : loadValueExpression.getReference();
                PsiElement resolved = reference == null ? null : reference.resolve();
                if (!(resolved instanceof StarlarkTargetExpression declaration)) {
                    return null;
                }
                PsiElement assignedValue = StarlarkAssignmentUtils.assignedValueForTarget(declaration);
                if (assignedValue == null) {
                    return null;
                }
                return StarlarkStaticValueAnalyzer.valueKind(assignedValue);
            });
        }

        public PsiElement getElement() {
            return myElement;
        }

        public String getLocalName() {
            return myLocalName;
        }


        public @Nullable StarlarkStaticValueKind getStaticValueKind() {
            return myStaticValueKind.get();
        }
    }

    private final boolean myIsEmpty;

    private final Map<String, StarlarkLoadedSymbol> myByName;

    private final Map<PsiElement, StarlarkLoadedSymbol> myByElement;

    private StarlarkFileLoadedSymbols(List<StarlarkLoadedSymbol> symbols) {
        myIsEmpty = symbols.isEmpty();

        Map<String, StarlarkLoadedSymbol> byName = new LinkedHashMap<>();
        for (StarlarkLoadedSymbol symbol : symbols) {
            byName.put(symbol.getLocalName(), symbol);
        }
        myByName = byName;

        Map<PsiElement, StarlarkLoadedSymbol> byElement = new HashMap<>();
        for (StarlarkLoadedSymbol symbol : symbols) {
            byElement.put(symbol.getElement(), symbol);
            PsiElement parent = symbol.getElement().getParent();
            if (parent != null) {
                byElement.put(parent, symbol);
            }
        }
        myByElement = byElement;
    }

    public boolean isEmpty() {
        return myIsEmpty;
    }

    public @Nullable StarlarkLoadedSymbol findByReference(PsiElement element) {
        String name = StarlarkQualifiableExpression.getSimpleNameOrNull(element);
        if (name == null) {
            return null;
        }
        StarlarkLoadedSymbol candidate = myByName.get(name);
        if (candidate == null) {
            return null;
        }
        PsiReference reference = element.getReference();
        PsiElement resolved = reference == null ? null : reference.resolve();
        if (resolved != null) {
            StarlarkLoadedSymbol symbol = myByElement.get(resolved);
            if (symbol != null) {
                return symbol;
            }
            if (Objects.equals(resolved.getContainingFile(), element.getContainingFile())) {
                return null;
            }
        }
        return candidate;
    }

    public static StarlarkFileLoadedSymbols create(StarlarkFile file) {
        List<StarlarkLoadedSymbol> symbols = new ArrayList<>();
        for (PsiElement child : file.getChildren()) {
            if (!(child instanceof StarlarkLoadStatement loadStatement)) {
                continue;
            }
            for (PsiElement loaded : loadStatement.getLoadedSymbolsPsi()) {
                if (!(loaded instanceof StarlarkLoadValue loadValue)) {
                    continue;
                }
                StarlarkLoadedSymbol symbol = loadedSymbol(loadValue);
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }
        return new StarlarkFileLoadedSymbols(symbols);
    }

    private static @Nullable StarlarkLoadedSymbol loadedSymbol(StarlarkLoadValue value) {
        if (value instanceof StarlarkNamedLoadValue namedLoadValue) {
            String name = namedLoadValue.getName();
            if (name == null) {
                return null;
            }
            PsiElement nameIdentifier = namedLoadValue.getNameIdentifier();
            return new StarlarkLoadedSymbol(nameIdentifier != null ? nameIdentifier : namedLoadValue, name, namedLoadValue);
        }
        else if (value instanceof StarlarkStringLoadValue stringLoadValue) {
            String name = stringLoadValue.getLoadValueExpressionContent();
            if (name == null) {
                return null;
            }
            PsiElement loadValueExpression = stringLoadValue.getLoadValueExpression();
            return new StarlarkLoadedSymbol(loadValueExpression != null ? loadValueExpression : stringLoadValue, name, stringLoadValue);
        }
        else {
            return null;
        }
    }
}
