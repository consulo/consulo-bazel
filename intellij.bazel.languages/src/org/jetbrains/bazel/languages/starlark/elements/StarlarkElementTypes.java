package org.jetbrains.bazel.languages.starlark.elements;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkConditionalExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkEmptyExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceItem;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkStarArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkBreakStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkContinueStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;

public final class StarlarkElementTypes {
    private StarlarkElementTypes() {
    }

    public static final StarlarkElementType ASSIGNMENT_STATEMENT = new StarlarkElementType("ASSIGNMENT_STATEMENT");
    public static final StarlarkElementType AUG_ASSIGNMENT_STATEMENT = new StarlarkElementType("AUG_ASSIGNMENT_STATEMENT");
    public static final StarlarkElementType BREAK_STATEMENT = new StarlarkElementType("BREAK_STATEMENT");
    public static final StarlarkElementType CONTINUE_STATEMENT = new StarlarkElementType("CONTINUE_STATEMENT");
    public static final StarlarkElementType EXPRESSION_STATEMENT = new StarlarkElementType("EXPRESSION_STATEMENT");
    public static final StarlarkElementType FOR_STATEMENT = new StarlarkElementType("FOR_STATEMENT");
    public static final StarlarkElementType IF_STATEMENT = new StarlarkElementType("IF_STATEMENT");
    public static final StarlarkElementType LOAD_STATEMENT = new StarlarkElementType("LOAD_STATEMENT");
    public static final StarlarkElementType FILENAME_LOAD_VALUE = new StarlarkElementType("FILENAME_LOAD_VALUE");
    public static final StarlarkElementType NAMED_LOAD_VALUE = new StarlarkElementType("NAMED_LOAD_VALUE");
    public static final StarlarkElementType STRING_LOAD_VALUE = new StarlarkElementType("STRING_LOAD_VALUE");
    public static final StarlarkElementType PASS_STATEMENT = new StarlarkElementType("PASS_STATEMENT");
    public static final StarlarkElementType RETURN_STATEMENT = new StarlarkElementType("RETURN_STATEMENT");
    public static final StarlarkElementType STATEMENT_LIST = new StarlarkElementType("STATEMENT_LIST");

    public static final StarlarkElementType FUNCTION_DECLARATION = new StarlarkElementType("FUNCTION_DECLARATION");
    public static final StarlarkElementType MANDATORY_PARAMETER = new StarlarkElementType("MANDATORY_PARAMETER");
    public static final StarlarkElementType OPTIONAL_PARAMETER = new StarlarkElementType("OPTIONAL_PARAMETER");
    public static final StarlarkElementType VARIADIC_PARAMETER = new StarlarkElementType("VARIADIC_PARAMETER");
    public static final StarlarkElementType KEYWORD_VARIADIC_PARAMETER = new StarlarkElementType("KEYWORD_VARIADIC_PARAMETER");
    public static final StarlarkElementType KEYWORD_ONLY_BOUNDARY = new StarlarkElementType("KEYWORD_ONLY_BOUNDARY");
    public static final StarlarkElementType PARAMETER_LIST = new StarlarkElementType("PARAMETER_LIST");

    public static final StarlarkElementType ARGUMENT_EXPRESSION = new StarlarkElementType("ARGUMENT_EXPRESSION");
    public static final StarlarkElementType ARGUMENT_LIST = new StarlarkElementType("ARGUMENT_LIST");
    public static final StarlarkElementType BINARY_EXPRESSION = new StarlarkElementType("BINARY_EXPRESSION");
    public static final StarlarkElementType CALL_EXPRESSION = new StarlarkElementType("CALL_EXPRESSION");
    public static final StarlarkElementType CONDITIONAL_EXPRESSION = new StarlarkElementType("CONDITIONAL_EXPRESSION");
    public static final StarlarkElementType DICT_COMP_EXPRESSION = new StarlarkElementType("DICT_COMP_EXPRESSION");
    public static final StarlarkElementType DICT_LITERAL_EXPRESSION = new StarlarkElementType("DICT_LITERAL_EXPRESSION");
    public static final StarlarkElementType DOUBLE_STAR_EXPRESSION = new StarlarkElementType("DOUBLE_STAR_EXPRESSION");
    public static final StarlarkElementType EMPTY_EXPRESSION = new StarlarkElementType("EMPTY_EXPRESSION");
    public static final StarlarkElementType FALSE_LITERAL_EXPRESSION = new StarlarkElementType("FALSE_LITERAL_EXPRESSION");
    public static final StarlarkElementType FLOAT_LITERAL_EXPRESSION = new StarlarkElementType("FLOAT_LITERAL_EXPRESSION");
    public static final StarlarkElementType GENERATOR_EXPRESSION = new StarlarkElementType("GENERATOR_EXPRESSION");
    public static final StarlarkElementType GLOB_EXPRESSION = new StarlarkElementType("GLOB_EXPRESSION");
    public static final StarlarkElementType TARGET_EXPRESSION = new StarlarkElementType("TARGET_EXPRESSION");
    public static final StarlarkElementType INTEGER_LITERAL_EXPRESSION = new StarlarkElementType("INTEGER_LITERAL_EXPRESSION");
    public static final StarlarkElementType KEY_VALUE_EXPRESSION = new StarlarkElementType("KEY_VALUE_EXPRESSION");
    public static final StarlarkElementType LAMBDA_EXPRESSION = new StarlarkElementType("LAMBDA_EXPRESSION");
    public static final StarlarkElementType LIST_COMP_EXPRESSION = new StarlarkElementType("LIST_COMP_EXPRESSION");
    public static final StarlarkElementType LIST_LITERAL_EXPRESSION = new StarlarkElementType("LIST_LITERAL_EXPRESSION");
    public static final StarlarkElementType NAMED_ARGUMENT_EXPRESSION = new StarlarkElementType("NAMED_ARGUMENT_EXPRESSION");
    public static final StarlarkElementType NONE_LITERAL_EXPRESSION = new StarlarkElementType("NONE_LITERAL_EXPRESSION");
    public static final StarlarkElementType PARENTHESIZED_EXPRESSION = new StarlarkElementType("PARENTHESIZED_EXPRESSION");
    public static final StarlarkElementType PREFIX_EXPRESSION = new StarlarkElementType("PREFIX_EXPRESSION");
    public static final StarlarkElementType REFERENCE_EXPRESSION = new StarlarkElementType("REFERENCE_EXPRESSION");
    public static final StarlarkElementType SLICE_EXPRESSION = new StarlarkElementType("SLICE_EXPRESSION");
    public static final StarlarkElementType SLICE_ITEM = new StarlarkElementType("SLICE_ITEM");
    public static final StarlarkElementType STAR_ARGUMENT_EXPRESSION = new StarlarkElementType("STAR_ARGUMENT_EXPRESSION");
    public static final StarlarkElementType STAR_EXPRESSION = new StarlarkElementType("STAR_EXPRESSION");
    public static final StarlarkElementType STRING_LITERAL_EXPRESSION = new StarlarkElementType("STRING_LITERAL_EXPRESSION");
    public static final StarlarkElementType SUBSCRIPTION_EXPRESSION = new StarlarkElementType("SUBSCRIPTION_EXPRESSION");
    public static final StarlarkElementType TRUE_LITERAL_EXPRESSION = new StarlarkElementType("TRUE_LITERAL_EXPRESSION");
    public static final StarlarkElementType TUPLE_EXPRESSION = new StarlarkElementType("TUPLE_EXPRESSION");


    public static final TokenSet EXPRESSIONS =
        TokenSet.create(
            ARGUMENT_EXPRESSION,
            ARGUMENT_LIST,
            BINARY_EXPRESSION,
            CALL_EXPRESSION,
            CONDITIONAL_EXPRESSION,
            DICT_COMP_EXPRESSION,
            DICT_LITERAL_EXPRESSION,
            DOUBLE_STAR_EXPRESSION,
            EMPTY_EXPRESSION,
            FALSE_LITERAL_EXPRESSION,
            FLOAT_LITERAL_EXPRESSION,
            GENERATOR_EXPRESSION,
            GLOB_EXPRESSION,
            TARGET_EXPRESSION,
            INTEGER_LITERAL_EXPRESSION,
            KEY_VALUE_EXPRESSION,
            LAMBDA_EXPRESSION,
            LIST_COMP_EXPRESSION,
            LIST_LITERAL_EXPRESSION,
            NAMED_ARGUMENT_EXPRESSION,
            NONE_LITERAL_EXPRESSION,
            PARENTHESIZED_EXPRESSION,
            PREFIX_EXPRESSION,
            REFERENCE_EXPRESSION,
            SLICE_EXPRESSION,
            SLICE_ITEM,
            STAR_ARGUMENT_EXPRESSION,
            STAR_EXPRESSION,
            STRING_LITERAL_EXPRESSION,
            SUBSCRIPTION_EXPRESSION,
            TRUE_LITERAL_EXPRESSION,
            TUPLE_EXPRESSION
        );

    public static PsiElement createElement(ASTNode node) {
        IElementType type = node.getElementType();
        if (type == ASSIGNMENT_STATEMENT) {
            return new StarlarkAssignmentStatement(node);
        }
        else if (type == AUG_ASSIGNMENT_STATEMENT) {
            return new StarlarkAugAssignmentStatement(node);
        }
        else if (type == BREAK_STATEMENT) {
            return new StarlarkBreakStatement(node);
        }
        else if (type == CONTINUE_STATEMENT) {
            return new StarlarkContinueStatement(node);
        }
        else if (type == EXPRESSION_STATEMENT) {
            return new StarlarkExpressionStatement(node);
        }
        else if (type == FOR_STATEMENT) {
            return new StarlarkForStatement(node);
        }
        else if (type == IF_STATEMENT) {
            return new StarlarkIfStatement(node);
        }
        else if (type == LOAD_STATEMENT) {
            return new StarlarkLoadStatement(node);
        }
        else if (type == FILENAME_LOAD_VALUE) {
            return new StarlarkFilenameLoadValue(node);
        }
        else if (type == NAMED_LOAD_VALUE) {
            return new StarlarkNamedLoadValue(node);
        }
        else if (type == STRING_LOAD_VALUE) {
            return new StarlarkStringLoadValue(node);
        }
        else if (type == PASS_STATEMENT) {
            return new StarlarkAugAssignmentStatement(node);
        }
        else if (type == RETURN_STATEMENT) {
            return new StarlarkReturnStatement(node);
        }
        else if (type == STATEMENT_LIST) {
            return new StarlarkStatementList(node);
        }
        else if (type == FUNCTION_DECLARATION) {
            return new StarlarkFunctionDeclaration(node);
        }
        else if (type == MANDATORY_PARAMETER) {
            return new StarlarkMandatoryParameter(node);
        }
        else if (type == OPTIONAL_PARAMETER) {
            return new StarlarkOptionalParameter(node);
        }
        else if (type == VARIADIC_PARAMETER) {
            return new StarlarkVariadicParameter(node);
        }
        else if (type == KEYWORD_VARIADIC_PARAMETER) {
            return new StarlarkKeywordVariadicParameter(node);
        }
        else if (type == PARAMETER_LIST) {
            return new StarlarkParameterList(node);
        }
        else if (type == KEYWORD_ONLY_BOUNDARY) {
            return new StarlarkKeywordOnlyBoundary(node);
        }
        else if (type == ARGUMENT_EXPRESSION) {
            return new StarlarkArgumentExpression(node);
        }
        else if (type == ARGUMENT_LIST) {
            return new StarlarkArgumentList(node);
        }
        else if (type == BINARY_EXPRESSION) {
            return new StarlarkBinaryExpression(node);
        }
        else if (type == CALL_EXPRESSION) {
            return new StarlarkCallExpression(node);
        }
        else if (type == CONDITIONAL_EXPRESSION) {
            return new StarlarkConditionalExpression(node);
        }
        else if (type == DICT_COMP_EXPRESSION) {
            return new StarlarkDictCompExpression(node);
        }
        else if (type == DICT_LITERAL_EXPRESSION) {
            return new StarlarkDictLiteralExpression(node);
        }
        else if (type == DOUBLE_STAR_EXPRESSION) {
            return new StarlarkDoubleStarExpression(node);
        }
        else if (type == EMPTY_EXPRESSION) {
            return new StarlarkEmptyExpression(node);
        }
        else if (type == FALSE_LITERAL_EXPRESSION) {
            return new StarlarkFalseLiteralExpression(node);
        }
        else if (type == FLOAT_LITERAL_EXPRESSION) {
            return new StarlarkFloatLiteralExpression(node);
        }
        else if (type == GENERATOR_EXPRESSION) {
            return new StarlarkGeneratorExpression(node);
        }
        else if (type == GLOB_EXPRESSION) {
            return new StarlarkGlobExpression(node);
        }
        else if (type == INTEGER_LITERAL_EXPRESSION) {
            return new StarlarkIntegerLiteralExpression(node);
        }
        else if (type == KEY_VALUE_EXPRESSION) {
            return new StarlarkKeyValueExpression(node);
        }
        else if (type == LAMBDA_EXPRESSION) {
            return new StarlarkLambdaExpression(node);
        }
        else if (type == LIST_COMP_EXPRESSION) {
            return new StarlarkListCompExpression(node);
        }
        else if (type == LIST_LITERAL_EXPRESSION) {
            return new StarlarkListLiteralExpression(node);
        }
        else if (type == NAMED_ARGUMENT_EXPRESSION) {
            return new StarlarkNamedArgumentExpression(node);
        }
        else if (type == NONE_LITERAL_EXPRESSION) {
            return new StarlarkNoneLiteralExpression(node);
        }
        else if (type == PARENTHESIZED_EXPRESSION) {
            return new StarlarkParenthesizedExpression(node);
        }
        else if (type == PREFIX_EXPRESSION) {
            return new StarlarkPrefixExpression(node);
        }
        else if (type == REFERENCE_EXPRESSION) {
            return new StarlarkReferenceExpression(node);
        }
        else if (type == SLICE_EXPRESSION) {
            return new StarlarkSliceExpression(node);
        }
        else if (type == SLICE_ITEM) {
            return new StarlarkSliceItem(node);
        }
        else if (type == STAR_ARGUMENT_EXPRESSION) {
            return new StarlarkStarArgumentExpression(node);
        }
        else if (type == STAR_EXPRESSION) {
            return new StarlarkStarExpression(node);
        }
        else if (type == STRING_LITERAL_EXPRESSION) {
            return new StarlarkStringLiteralExpression(node);
        }
        else if (type == SUBSCRIPTION_EXPRESSION) {
            return new StarlarkSubscriptionExpression(node);
        }
        else if (type == TARGET_EXPRESSION) {
            return new StarlarkTargetExpression(node);
        }
        else if (type == TRUE_LITERAL_EXPRESSION) {
            return new StarlarkTrueLiteralExpression(node);
        }
        else if (type == TUPLE_EXPRESSION) {
            return new StarlarkTupleExpression(node);
        }
        else {
            throw new IllegalStateException("Unknown element type: " + type);
        }
    }
}
