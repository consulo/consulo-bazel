package org.jetbrains.bazel.languages.starlark.parser;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;

class StarlarkParser implements PsiParser {
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder, LanguageVersion languageVersion) {
        PsiBuilder.Marker rootMarker = builder.mark();
        ParsingContext context = new ParsingContext(builder);
        boolean lastAfterSemicolon = false;

        while (!builder.eof()) {
            context.pushScope(context.emptyParsingScope());
            if (lastAfterSemicolon) {
                context.getStatementParser().parseSimpleStatement();
            }
            else {
                context.getStatementParser().parseStatement();
            }
            lastAfterSemicolon = context.getScope().isAfterSemicolon();
            context.popScope();
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
