// consulo: package org.jetbrains.bazel.languages.starlark.bazel is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.languages.starlark.bazel.java;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;

import java.util.List;

// consulo: upstream EP_NAME (org.jetbrains.bazel.starlarkClassParametersProvider) ->
//   Application.get().getExtensionList(StarlarkClassParametersProvider.class)
@ExtensionAPI(ComponentScope.APPLICATION)
public interface StarlarkClassParametersProvider {
    List<String> getClassnameParameters();
}

@ExtensionImpl
class DefaultStarlarkClassParametersProvider implements StarlarkClassParametersProvider {
    public DefaultStarlarkClassParametersProvider() {
    }

    @Override
    public List<String> getClassnameParameters() {
        return List.of(
            "classname",
            "main_class",
            "test_class",
            "processor_class",
            "genclass",
            "tag_class"
        );
    }
}
