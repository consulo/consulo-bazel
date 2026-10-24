package org.jetbrains.bazel.languages.projectview.parser;

import consulo.application.Application;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementType;
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class ProjectViewParser {
    private final PsiBuilder myBuilder;

    public ProjectViewParser(PsiBuilder builder) {
        myBuilder = builder;
    }

    public void parseFile() {
        myBuilder.setDebugMode(Application.get().isUnitTestMode());
        while (!myBuilder.eof()) {
            if (matches(ProjectViewTokenType.NEWLINE)) {
                continue;
            }
            parseBlock();
        }
    }

    /**
     * Parse an import statement or a section.
     */
    public void parseBlock() {
        PsiBuilder.Marker sectionMarker = myBuilder.mark();
        IElementType currentTokenType = getCurrentTokenType();
        if (currentTokenType == ProjectViewTokenType.SECTION_KEYWORD) {
            PsiBuilder.Marker sectionNameMarker = myBuilder.mark();
            myBuilder.advanceLexer();
            sectionNameMarker.done(ProjectViewElementTypes.SECTION_NAME);
            expect(ProjectViewTokenType.COLON);
            if (!matches(ProjectViewTokenType.NEWLINE)) {
                parseItem(ProjectViewElementTypes.SECTION_ITEM);
            }
            parseListItems();
            sectionMarker.done(ProjectViewElementTypes.SECTION);
            return;
        }
        else if (currentTokenType == ProjectViewTokenType.IMPORT_KEYWORD) {
            myBuilder.advanceLexer();
            parseItem(ProjectViewElementTypes.IMPORT_ITEM);
            myBuilder.advanceLexer();
            sectionMarker.done(ProjectViewElementTypes.IMPORT);
            return;
        }
        else if (currentTokenType == ProjectViewTokenType.TRY_IMPORT_KEYWORD) {
            myBuilder.advanceLexer();
            parseItem(ProjectViewElementTypes.IMPORT_ITEM);
            myBuilder.advanceLexer();
            sectionMarker.done(ProjectViewElementTypes.TRY_IMPORT);
            return;
        }
        // Error handling
        // consulo: upstream hard-codes these messages; they live in BazelProjectViewLocalize
        if (matches(ProjectViewTokenType.INDENT)) {
            skipBlockAndError(sectionMarker, BazelProjectViewLocalize.bazelLanguageProjectParserIndentedLine());
        }
        else if (matches(ProjectViewTokenType.COLON)) {
            skipBlockAndError(sectionMarker, BazelProjectViewLocalize.bazelLanguageProjectParserLineStartsWithColon());
        }
        else {
            skipBlockAndError(sectionMarker, BazelProjectViewLocalize.bazelLanguageProjectParserUnrecognizedKeyword(String.valueOf(myBuilder.getTokenText())));
        }
    }

    public void parseListItems() {
        while (!myBuilder.eof()) {
            if (matches(ProjectViewTokenType.NEWLINE)) {
                continue;
            }
            if (!matches(ProjectViewTokenType.INDENT)) {
                return;
            }
            parseItem(ProjectViewElementTypes.SECTION_ITEM);
        }
    }

    public void parseItem(ProjectViewElementType itemType) {
        PsiBuilder.Marker item = myBuilder.mark();
        skipToEndOfLine();
        item.done(itemType);
    }

    public void skipBlockAndError(PsiBuilder.Marker marker, LocalizeValue message) {
        while (!myBuilder.eof()) {
            IElementType nextTokenType = myBuilder.lookAhead(1);
            if (myBuilder.lookAhead(0) == ProjectViewTokenType.NEWLINE &&
                nextTokenType != null && BLOCK_HEADERS.contains(nextTokenType)
            ) {
                myBuilder.advanceLexer();
                break;
            }
            myBuilder.advanceLexer();
        }
        marker.error(message);
    }

    public void skipToEndOfLine() {
        while (!myBuilder.eof()) {
            if (getCurrentTokenType() == ProjectViewTokenType.NEWLINE) {
                return;
            }
            myBuilder.advanceLexer();
        }
    }

    public void skipToNextLine() {
        while (!myBuilder.eof()) {
            if (matches(ProjectViewTokenType.NEWLINE)) {
                return;
            }
            myBuilder.advanceLexer();
        }
    }

    /**
     * Consume the current token if matches the provided token type and return true.
     * Otherwise, return false and mark an error.
     */
    public boolean expect(ProjectViewTokenType type) {
        if (matches(type)) {
            return true;
        }
        myBuilder.error(BazelProjectViewLocalize.bazelLanguageProjectParserError(type));
        return false;
    }

    /**
     * Consume the current token if matches the provided token type and return true.
     * Otherwise, return false.
     */
    public boolean matches(ProjectViewTokenType type) {
        if (getCurrentTokenType() == type) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    public @Nullable IElementType getCurrentTokenType() {
        return myBuilder.getTokenType();
    }

    private static final Set<IElementType> BLOCK_HEADERS =
        Set.of(
            ProjectViewTokenType.IDENTIFIER,
            ProjectViewTokenType.IMPORT_KEYWORD,
            ProjectViewTokenType.TRY_IMPORT_KEYWORD,
            ProjectViewTokenType.SECTION_KEYWORD
        );
}
