package org.jetbrains.bazel.python.lang;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class PythonBazelRules implements TargetKindProvider {
    @Override
    public Set<TargetKind> getTargetKinds() {
        return new LinkedHashSet<>(List.of(
            new TargetKind("py_binary", Set.of(PythonLanguageClass.PYTHON), RuleType.BINARY),
            new TargetKind("py_test", Set.of(PythonLanguageClass.PYTHON), RuleType.TEST),
            new TargetKind("py_library", Set.of(PythonLanguageClass.PYTHON), RuleType.LIBRARY)
        ));
    }
}
