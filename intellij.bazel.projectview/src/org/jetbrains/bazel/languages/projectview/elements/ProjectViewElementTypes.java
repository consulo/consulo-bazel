package org.jetbrains.bazel.languages.projectview.elements;

import consulo.language.ast.IFileElementType;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiTryImport;

public final class ProjectViewElementTypes {
    private ProjectViewElementTypes() {
    }

    public static final IFileElementType FILE = new IFileElementType(ProjectViewLanguage.INSTANCE);

    public static final ProjectViewElementType IMPORT = new ProjectViewElementType("import", ProjectViewPsiImport.class);
    public static final ProjectViewElementType TRY_IMPORT = new ProjectViewElementType("try_import", ProjectViewPsiTryImport.class);
    public static final ProjectViewElementType IMPORT_ITEM = new ProjectViewElementType("import_item", ProjectViewPsiImportItem.class);

    public static final ProjectViewElementType SECTION = new ProjectViewElementType("section", ProjectViewPsiSection.class);
    public static final ProjectViewElementType SECTION_NAME = new ProjectViewElementType("section_name", ProjectViewPsiSectionName.class);
    public static final ProjectViewElementType SECTION_ITEM = new ProjectViewElementType("section_item", ProjectViewPsiSectionItem.class);
}
