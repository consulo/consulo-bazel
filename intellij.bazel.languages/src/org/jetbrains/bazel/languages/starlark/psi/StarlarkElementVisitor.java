package org.jetbrains.bazel.languages.starlark.psi;

import consulo.language.psi.PsiElementVisitor;
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
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkPassStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;

public class StarlarkElementVisitor extends PsiElementVisitor {
    public void visitArgumentExpression(StarlarkArgumentExpression node) {
        visitElement(node);
    }

    public void visitArgumentList(StarlarkArgumentList node) {
        visitElement(node);
    }

    public void visitAssignmentStatement(StarlarkAssignmentStatement node) {
        visitElement(node);
    }

    public void visitAugAssignmentStatement(StarlarkAugAssignmentStatement node) {
        visitElement(node);
    }

    public void visitBinaryExpression(StarlarkBinaryExpression node) {
        visitElement(node);
    }

    public void visitBreakStatement(StarlarkBreakStatement node) {
        visitElement(node);
    }

    public void visitCallExpression(StarlarkCallExpression node) {
        visitElement(node);
    }

    public void visitConditionalExpression(StarlarkConditionalExpression node) {
        visitElement(node);
    }

    public void visitContinueStatement(StarlarkContinueStatement node) {
        visitElement(node);
    }

    public void visitDictCompExpression(StarlarkDictCompExpression node) {
        visitElement(node);
    }

    public void visitDictLiteralExpression(StarlarkDictLiteralExpression node) {
        visitElement(node);
    }

    public void visitDoubleStarExpression(StarlarkDoubleStarExpression node) {
        visitElement(node);
    }

    public void visitEmptyExpression(StarlarkEmptyExpression node) {
        visitElement(node);
    }

    public void visitExpressionStatement(StarlarkExpressionStatement node) {
        visitElement(node);
    }

    public void visitFalseLiteralExpression(StarlarkFalseLiteralExpression node) {
        visitElement(node);
    }

    public void visitFloatLiteralExpression(StarlarkFloatLiteralExpression node) {
        visitElement(node);
    }

    public void visitForStatement(StarlarkForStatement node) {
        visitElement(node);
    }

    public void visitFunctionDeclaration(StarlarkFunctionDeclaration node) {
        visitElement(node);
    }

    public void visitGeneratorExpression(StarlarkGeneratorExpression node) {
        visitElement(node);
    }

    public void visitIfStatement(StarlarkIfStatement node) {
        visitElement(node);
    }

    public void visitIntegerLiteralExpression(StarlarkIntegerLiteralExpression node) {
        visitElement(node);
    }

    public void visitKeyValueExpression(StarlarkKeyValueExpression node) {
        visitElement(node);
    }

    public void visitLambdaExpression(StarlarkLambdaExpression node) {
        visitElement(node);
    }

    public void visitListCompExpression(StarlarkListCompExpression node) {
        visitElement(node);
    }

    public void visitListLiteralExpression(StarlarkListLiteralExpression node) {
        visitElement(node);
    }

    public void visitLoadStatement(StarlarkLoadStatement node) {
        visitElement(node);
    }

    public void visitFilenameLoadValue(StarlarkFilenameLoadValue node) {
        visitElement(node);
    }

    public void visitNamedArgumentExpression(StarlarkNamedArgumentExpression node) {
        visitElement(node);
    }

    public void visitNamedLoadValue(StarlarkNamedLoadValue node) {
        visitElement(node);
    }

    public void visitNoneLiteralExpression(StarlarkNoneLiteralExpression node) {
        visitElement(node);
    }

    public void visitStringLoadValue(StarlarkStringLoadValue node) {
        visitElement(node);
    }

    public void visitMandatoryParameter(StarlarkMandatoryParameter node) {
        visitElement(node);
    }

    public void visitOptionalParameter(StarlarkOptionalParameter node) {
        visitElement(node);
    }

    public void visitVariadicParameter(StarlarkVariadicParameter node) {
        visitElement(node);
    }

    public void visitKeywordVariadicParameter(StarlarkKeywordVariadicParameter node) {
        visitElement(node);
    }

    public void visitParameterList(StarlarkParameterList node) {
        visitElement(node);
    }

    public void visitParenthesizedExpression(StarlarkParenthesizedExpression node) {
        visitElement(node);
    }

    public void visitPassStatement(StarlarkPassStatement node) {
        visitElement(node);
    }

    public void visitPrefixExpression(StarlarkPrefixExpression node) {
        visitElement(node);
    }

    public void visitReferenceExpression(StarlarkReferenceExpression node) {
        visitElement(node);
    }

    public void visitReturnStatement(StarlarkReturnStatement node) {
        visitElement(node);
    }

    public void visitSliceExpression(StarlarkSliceExpression node) {
        visitElement(node);
    }

    public void visitSliceItem(StarlarkSliceItem node) {
        visitElement(node);
    }

    public void visitStarArgumentExpression(StarlarkStarArgumentExpression node) {
        visitElement(node);
    }

    public void visitStarExpression(StarlarkStarExpression node) {
        visitElement(node);
    }

    public void visitStatementListImpl(StarlarkStatementList node) {
        visitElement(node);
    }

    public void visitStringLiteralExpression(StarlarkStringLiteralExpression node) {
        visitElement(node);
    }

    public void visitSubscriptionExpression(StarlarkSubscriptionExpression node) {
        visitElement(node);
    }

    public void visitTargetExpression(StarlarkTargetExpression node) {
        visitElement(node);
    }

    public void visitTrueLiteralExpression(StarlarkTrueLiteralExpression node) {
        visitElement(node);
    }

    public void visitTupleExpression(StarlarkTupleExpression node) {
        visitElement(node);
    }

    public void visitGlobExpression(StarlarkGlobExpression node) {
        visitElement(node);
    }
}
