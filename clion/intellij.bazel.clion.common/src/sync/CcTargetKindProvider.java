package org.jetbrains.bazel.clion.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ExtensionImpl
public class CcTargetKindProvider implements TargetKindProvider {
    // consulo: upstream private top-level val CC_RULE_TYPES
    private static final Map<String, RuleType> CC_RULE_TYPES = new LinkedHashMap<>();

    static {
        CC_RULE_TYPES.put("cc_binary", RuleType.BINARY);
        CC_RULE_TYPES.put("cc_library", RuleType.LIBRARY);
        CC_RULE_TYPES.put("cc_test", RuleType.TEST);
    }

    public CcTargetKindProvider() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        Set<TargetKind> result = new LinkedHashSet<>();
        for (Map.Entry<String, RuleType> it : CC_RULE_TYPES.entrySet()) {
            result.add(new TargetKind(it.getKey(), it.getValue()));
        }
        return result;
    }
}
