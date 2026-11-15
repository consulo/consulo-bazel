package org.jetbrains.bazel.ui.widgets;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;

/**
 * consulo: the upstream {@code BazelJumpToBuildFileAction$XmlRegistered} action registration
 * ({@code @ActionImpl} on nested classes is not supported by the annotation processor).
 */
@ActionImpl(
    id = "Bazel.JumpToBuildFile",
    parents = {
        @ActionParentRef(@ActionRef(id = "EditorPopupMenu")),
        @ActionParentRef(@ActionRef(id = "EditorTabPopupMenu")),
    }
)
// TODO consulo: default shortcut control alt G, J
public class BazelJumpToBuildFileActionXmlRegistered extends BazelJumpToBuildFileAction.XmlRegistered {
    public BazelJumpToBuildFileActionXmlRegistered() {
    }
}
