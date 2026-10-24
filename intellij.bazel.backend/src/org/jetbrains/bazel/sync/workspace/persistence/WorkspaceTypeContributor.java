package org.jetbrains.bazel.sync.workspace.persistence;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface WorkspaceTypeContributor {
    List<WorkspaceTypeEntry> contribute(Project project);

    // consulo: upstream EP_NAME, served by the Consulo extension point
    static ExtensionPoint<WorkspaceTypeContributor> getEpName() {
        return Application.get().getExtensionPoint(WorkspaceTypeContributor.class);
    }

    record WorkspaceTypeEntry(String fqn, Class<?> type) {
    }

    // consulo: upstream `fun <T : Any> MutableList<WorkspaceTypeEntry>.type(type: KClass<T>)` and
    // `inline fun <reified T : Any> MutableList<WorkspaceTypeEntry>.type()`, the type is passed explicitly
    static <T> void type(List<WorkspaceTypeEntry> receiver, Class<T> type) {
        // consulo: KClass.qualifiedName is the canonical name of the class
        String fqn = type.getCanonicalName();
        if (fqn == null) {
            throw new IllegalStateException("`" + type + "` have no valid qualified name");
        }
        receiver.add(new WorkspaceTypeEntry(fqn, type));
    }

    static <T> void sealed(List<WorkspaceTypeEntry> receiver, Class<T> type) {
        if (!type.isSealed()) {
            throw new IllegalArgumentException("type " + type + " must be sealed");
        }
        type(receiver, type);
        for (Class<?> it : type.getPermittedSubclasses()) {
            type(receiver, it);
        }
    }
}
