package org.jetbrains.bazel.languages.projectview.lexer;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;

/**
 * A token type for the project view language.
 * <p>
 * See the Project Views documentation for more information.
 */
public class ProjectViewTokenType extends IElementType {
    /**
     * @param debugName the name of the element type, used for debugging purposes.
     */
    private ProjectViewTokenType(String debugName) {
        super(debugName, ProjectViewLanguage.INSTANCE);
    }

    /**
     * The character ':'
     */
    public static final ProjectViewTokenType COLON = new ProjectViewTokenType("colon");

    /**
     * A comment starting with '#'.
     * <p>
     * No non-white characters are present before the comment.
     */
    public static final ProjectViewTokenType COMMENT = new ProjectViewTokenType("comment");

    /**
     * A sequence of characters that are neither whitespace nor colon nor comment.
     */
    public static final ProjectViewTokenType IDENTIFIER = new ProjectViewTokenType("identifier");

    /**
     * A series of whitespaces at the beginning of a line NOT followed by a comment or '\n'.
     */
    public static final ProjectViewTokenType INDENT = new ProjectViewTokenType("indent");

    /**
     * An identifier that is a section keyword.
     */
    public static final ProjectViewTokenType SECTION_KEYWORD = new ProjectViewTokenType("section_keyword");

    /**
     * An identifier that is an import keyword.
     */
    public static final ProjectViewTokenType IMPORT_KEYWORD = new ProjectViewTokenType("import_keyword");

    /**
     * An identifier that is an try_import keyword.
     */
    public static final ProjectViewTokenType TRY_IMPORT_KEYWORD = new ProjectViewTokenType("try_import_keyword");

    /**
     * The character '\n'.
     */
    public static final ProjectViewTokenType NEWLINE = new ProjectViewTokenType("newline");

    /**
     * A consecutive sequence of characters from the set { ' ', '\r', '\t' }.
     */
    public static final ProjectViewTokenType WHITESPACE = new ProjectViewTokenType("whitespace");

    @Override
    public String toString() {
        return "ProjectView:" + super.toString();
    }
}
