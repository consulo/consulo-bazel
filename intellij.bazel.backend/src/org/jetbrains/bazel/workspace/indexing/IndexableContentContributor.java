package org.jetbrains.bazel.workspace.indexing;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface IndexableContentContributor {

    IndexableContent getIndexableContent(Project project);

    static ExtensionPoint<IndexableContentContributor> getEp() {
        return Application.get().getExtensionPoint(IndexableContentContributor.class);
    }
}
