package org.jetbrains.bazel.clion.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class CcTargetKindProvider implements TargetKindProvider {
    // consulo: upstream top-level val CC_TEST_KIND of CcTargetKindProvider.kt
    public static final TargetKind CC_TEST_KIND = new TargetKind("cc_test", Set.of(CcLanguageClass.CC_LANGUAGE_CLASS), RuleType.TEST);

    private final Set<TargetKind> myTargetKinds = new LinkedHashSet<>(List.of(
        new TargetKind("cc_binary", Set.of(CcLanguageClass.CC_LANGUAGE_CLASS), RuleType.BINARY),
        new TargetKind("cc_library", Set.of(CcLanguageClass.CC_LANGUAGE_CLASS), RuleType.LIBRARY),
        CC_TEST_KIND
    ));

    public CcTargetKindProvider() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        return myTargetKinds;
    }
}
