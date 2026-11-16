package org.jetbrains.bazel.python.lang;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;

import java.util.List;
import java.util.Set;

public final class PythonLanguageClass {
    private PythonLanguageClass() {
    }

    public static final LanguageClass PYTHON = new LanguageClass("python", Set.of("py", "pyw", "pyi"));
}

// consulo: upstream internal class of PythonLanguageClass.kt, a package-private top-level class (a nested @ExtensionImpl class is
// not supported by the annotation processor)
@ExtensionImpl
class PythonLanguageClassProvider implements LanguageClassProvider {
    public PythonLanguageClassProvider() {
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(PythonLanguageClass.PYTHON);
    }
}
