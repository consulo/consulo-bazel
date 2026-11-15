package org.jetbrains.bazel.sync.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;

/**
 * consulo: the registered variant of upstream {@code ResyncFilesAction$XmlRegistered}; the annotation processor cannot handle nested
 * {@code @ActionImpl} classes.
 */
@ActionImpl(
    id = "Bazel.ResyncFilesAction",
    parents = {
        @ActionParentRef(value = @ActionRef(id = "ProjectViewPopupMenu"), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = "EditorTabPopupMenu"), anchor = ActionRefAnchor.LAST)
    }
)
public class ResyncFilesActionXmlRegistered extends ResyncFilesAction.XmlRegistered {
    public ResyncFilesActionXmlRegistered() {
    }
}
