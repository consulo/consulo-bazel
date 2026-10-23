package org.jetbrains.bazel.languages.bazelquery.parser;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes;

// consulo: Kotlin `PsiBuilder by builder` delegation is replaced by explicit calls on myBuilder
class ParsingFlags {
    private final IElementType myRoot;
    private final PsiBuilder myBuilder;

    private final ParsingUtils myUtils;

    ParsingFlags(IElementType root, PsiBuilder builder) {
        myRoot = root;
        myBuilder = builder;
        myUtils = new ParsingUtils(builder);
    }

    public PsiBuilder getBuilder() {
        return myBuilder;
    }

    public ASTNode parseFile() {
        PsiBuilder.Marker flagsList = myBuilder.mark();
        while (!myBuilder.eof()) {
            if (myUtils.atAnyToken(BazelQueryTokenSets.FLAGS)) {
                parseFlag();
            }
            else {
                myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedToken());
            }
        }
        flagsList.done(myRoot);
        return myBuilder.getTreeBuilt();
    }

    private void parseFlag() {
        PsiBuilder.Marker flag = myBuilder.mark();

        if (myUtils.atToken(BazelQueryTokenTypes.UNFINISHED_FLAG)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnfinishedFlag());
            myUtils.expectToken(BazelQueryTokenTypes.WHITE_SPACE);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.FLAG)) {
            myBuilder.advanceLexer();

            if (!myUtils.matchToken(BazelQueryTokenTypes.EQUALS)) {
                myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingFlag());
            }
            else {
                if (myUtils.matchToken(BazelQueryTokenTypes.UNFINISHED_VAL)) {
                    myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingQuote());
                }
                else if (!myUtils.matchAnyToken(BazelQueryTokenSets.FLAG_VALS)) {
                    myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingFlag());
                }
                else if (!myBuilder.eof()) {
                    myUtils.expectToken(BazelQueryTokenTypes.WHITE_SPACE);
                }
            }
        }
        else {
            myBuilder.advanceLexer();
            while (myUtils.matchToken(BazelQueryTokenTypes.MISSING_SPACE)) {
                myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingSpace());
            }
            myUtils.matchToken(BazelQueryTokenTypes.WHITE_SPACE);
        }

        flag.done(BazelQueryElementTypes.FLAG);
    }
}
