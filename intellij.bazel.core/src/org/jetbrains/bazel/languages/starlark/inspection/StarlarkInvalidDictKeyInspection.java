package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStringDecoder;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ExtensionImpl
public class StarlarkInvalidDictKeyInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new InvalidDictKeyVisitor(holder);
    }

    private static class InvalidDictKeyVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        InvalidDictKeyVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitKeyValueExpression(StarlarkKeyValueExpression node) {
            PsiElement key = firstExpressionChild(node);
            if (key == null) {
                return;
            }
            if (isUnhashableLiteral(key)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionDictKeyNotHashable())
                    .range(key)
                    .create();
            }
        }

        @Override
        public void visitDictLiteralExpression(StarlarkDictLiteralExpression node) {
            List<StaticDictKey> seenKeys = new ArrayList<>();
            for (PsiElement child : node.getChildren()) {
                if (!(child instanceof StarlarkKeyValueExpression keyValue)) {
                    continue;
                }
                PsiElement key = firstExpressionChild(keyValue);
                if (key == null) {
                    continue;
                }
                StaticDictKey staticKey = staticDictKey(key);
                if (staticKey == null) {
                    continue;
                }
                if (seenKeys.stream().anyMatch(it -> it.isSameKey(staticKey))) {
                    myHolder.newProblem(StarlarkLocalize.inspectionDescriptionDictKeyDuplicate())
                        .range(key)
                        .create();
                }
                else {
                    seenKeys.add(staticKey);
                }
            }
        }

        // consulo: Kotlin children.firstOrNull { isExpression(it) }
        private @Nullable PsiElement firstExpressionChild(PsiElement element) {
            for (PsiElement child : element.getChildren()) {
                if (isExpression(child)) {
                    return child;
                }
            }
            return null;
        }

        private boolean isUnhashableLiteral(PsiElement element) {
            PsiElement expression = unwrapParenthesized(element);
            if (expression instanceof StarlarkListLiteralExpression || expression instanceof StarlarkDictLiteralExpression) {
                return true;
            }
            else if (expression instanceof StarlarkTupleExpression tupleExpression) {
                return tupleElements(tupleExpression).stream().anyMatch(this::isUnhashableLiteral);
            }
            else {
                return false;
            }
        }

        private PsiElement unwrapParenthesized(PsiElement element) {
            if (element instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
                StarlarkTupleExpression tuple = parenthesizedExpression.getTuple();
                if (tuple != null) {
                    return tuple;
                }
                PsiElement child = firstExpressionChild(parenthesizedExpression);
                if (child != null) {
                    return unwrapParenthesized(child);
                }
                return element;
            }
            else {
                return element;
            }
        }

        private List<PsiElement> tupleElements(StarlarkTupleExpression tuple) {
            return Arrays.stream(tuple.getChildren()).filter(this::isExpression).toList();
        }

        private boolean isExpression(PsiElement element) {
            return StarlarkElementTypes.EXPRESSIONS.contains(PsiUtilCore.getElementType(element));
        }

        private @Nullable StaticDictKey staticDictKey(PsiElement element) {
            PsiElement expression = unwrapParenthesized(element);
            if (expression instanceof StarlarkStringLiteralExpression) {
                String decoded = StarlarkStringDecoder.decodeLiteral(expression.getText());
                return decoded == null ? null : new StaticDictKey.StringKey(decoded);
            }
            else if (expression instanceof StarlarkIntegerLiteralExpression) {
                Number number = parseIntegerNumberLiteral(expression.getText());
                return number == null ? null : new StaticDictKey.NumberKey(number);
            }
            else if (expression instanceof StarlarkFloatLiteralExpression) {
                Number number = parseFloatNumberLiteral(expression.getText());
                return number == null ? null : new StaticDictKey.NumberKey(number);
            }
            else if (expression instanceof StarlarkTrueLiteralExpression) {
                return new StaticDictKey.BooleanKey(true);
            }
            else if (expression instanceof StarlarkFalseLiteralExpression) {
                return new StaticDictKey.BooleanKey(false);
            }
            else if (expression instanceof StarlarkNoneLiteralExpression) {
                return StaticDictKey.NoneKey.INSTANCE;
            }
            else if (expression instanceof StarlarkPrefixExpression prefixExpression) {
                return staticPrefixedNumberKey(prefixExpression);
            }
            else if (expression instanceof StarlarkTupleExpression tupleExpression) {
                return staticTupleKey(tupleExpression);
            }
            else {
                return null;
            }
        }

        private @Nullable Number parseIntegerNumberLiteral(String text) {
            // consulo: Kotlin runCatching { ... }.getOrNull()
            try {
                String value = text.toLowerCase(Locale.ROOT);
                BigInteger integer;
                if (value.startsWith("0x")) {
                    integer = new BigInteger(value.substring("0x".length()), 16);
                }
                else if (value.startsWith("0o")) {
                    integer = new BigInteger(value.substring("0o".length()), 8);
                }
                else {
                    integer = new BigInteger(value, 10);
                }
                return new Number.Integer(integer);
            }
            catch (RuntimeException e) {
                return null;
            }
        }

        private @Nullable Number parseFloatNumberLiteral(String text) {
            // consulo: Kotlin runCatching { ... }.getOrNull()
            try {
                return new Number.Float(java.lang.Double.parseDouble(text.toLowerCase(Locale.ROOT)));
            }
            catch (RuntimeException e) {
                return null;
            }
        }

        private @Nullable StaticDictKey staticPrefixedNumberKey(StarlarkPrefixExpression expression) {
            PsiElement firstChild = expression.getFirstChild();
            String operatorText = firstChild == null ? null : firstChild.getText();
            String operator = operatorText != null && (operatorText.equals("+") || operatorText.equals("-")) ? operatorText : null;
            if (operator == null) {
                return null;
            }
            PsiElement operand = null;
            for (PsiElement child : expression.getChildren()) {
                if (isExpression(child) && !Objects.equals(child, expression.getFirstChild())) {
                    operand = child;
                    break;
                }
            }
            if (operand == null) {
                return null;
            }
            if (!(staticDictKey(operand) instanceof StaticDictKey.NumberKey key)) {
                return null;
            }

            Number value = operator.equals("-") ? key.value().negate() : key.value();
            return new StaticDictKey.NumberKey(value);
        }

        private @Nullable StaticDictKey staticTupleKey(StarlarkTupleExpression tuple) {
            List<StaticDictKey> keys = new ArrayList<>();
            for (PsiElement element : tupleElements(tuple)) {
                StaticDictKey key = staticDictKey(element);
                if (key == null) {
                    return null;
                }
                keys.add(key);
            }
            return new StaticDictKey.Tuple(keys);
        }
    }

    private sealed interface StaticDictKey {
        boolean isSameKey(StaticDictKey other);

        record StringKey(String value) implements StaticDictKey {
            @Override
            public boolean isSameKey(StaticDictKey other) {
                return other instanceof StringKey stringKey && value.equals(stringKey.value);
            }
        }

        record NumberKey(Number value) implements StaticDictKey {
            @Override
            public boolean isSameKey(StaticDictKey other) {
                return other instanceof NumberKey numberKey && value.isSameNumber(numberKey.value);
            }
        }

        record BooleanKey(boolean value) implements StaticDictKey {
            @Override
            public boolean isSameKey(StaticDictKey other) {
                return other instanceof BooleanKey booleanKey && value == booleanKey.value;
            }
        }

        // consulo: Kotlin data object
        record NoneKey() implements StaticDictKey {
            static final NoneKey INSTANCE = new NoneKey();

            @Override
            public boolean isSameKey(StaticDictKey other) {
                return other instanceof NoneKey;
            }
        }

        record Tuple(List<StaticDictKey> elements) implements StaticDictKey {
            @Override
            public boolean isSameKey(StaticDictKey other) {
                if (!(other instanceof Tuple tuple) || elements.size() != tuple.elements.size()) {
                    return false;
                }
                for (int i = 0; i < elements.size(); i++) {
                    if (!elements.get(i).isSameKey(tuple.elements.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    private sealed interface Number {
        Number negate();

        boolean isSameNumber(Number other);

        record Integer(BigInteger value) implements Number {
            @Override
            public Number negate() {
                return new Integer(value.negate());
            }

            @Override
            public boolean isSameNumber(Number other) {
                if (other instanceof Integer integer) {
                    return value.equals(integer.value);
                }
                else if (other instanceof Float floatNumber) {
                    return Objects.equals(toExactIntegerOrNull(floatNumber.value), value);
                }
                else {
                    throw new IllegalStateException();
                }
            }
        }

        record Float(double value) implements Number {
            @Override
            public Number negate() {
                return new Float(-value);
            }

            @Override
            public boolean isSameNumber(Number other) {
                if (other instanceof Integer integer) {
                    return Objects.equals(toExactIntegerOrNull(value), integer.value);
                }
                else if (other instanceof Float floatNumber) {
                    return value == floatNumber.value;
                }
                else {
                    throw new IllegalStateException();
                }
            }
        }

        private static @Nullable BigInteger toExactIntegerOrNull(double receiver) {
            // consulo: Kotlin runCatching { ... }.getOrNull()
            try {
                if (!java.lang.Double.isFinite(receiver)) {
                    return null;
                }
                return new BigDecimal(receiver).toBigIntegerExact();
            }
            catch (RuntimeException e) {
                return null;
            }
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkInvalidDictKey";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkInvalidDictKeyCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.starlarkInspectionGroupName();
    }

    // consulo: upstream groupPath="Bazel"
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{BazelPluginLocalize.bazelInspectionGroupName(), getGroupDisplayName()};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
