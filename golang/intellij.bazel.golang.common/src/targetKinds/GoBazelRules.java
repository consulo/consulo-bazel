package org.jetbrains.bazel.golang.targetKinds;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.golang.GoLanguageClassProvider.GoLanguageClass;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ExtensionImpl
public class GoBazelRules implements TargetKindProvider {
    @Override
    public Set<TargetKind> getTargetKinds() {
        return Arrays.stream(GoRuleTypes.values())
            .map(it -> new TargetKind(
                it.getKindString(),
                Set.of(GoLanguageClass.GO),
                it.getRuleType()
            ))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // consulo: upstream top-level extension `fun TargetKind.includesGo()`
    public static boolean includesGo(TargetKind receiver) {
        return receiver.languageClasses().contains(GoLanguageClass.GO);
    }
}

enum GoRuleTypes {
    GO_GAZELLE_BINARY("gazelle_binary", RuleType.BINARY),
    GO_TEST("go_test", RuleType.TEST),
    GO_APPENGINE_TEST("go_appengine_test", RuleType.TEST),
    GO_BINARY("go_binary", RuleType.BINARY),
    GO_APPENGINE_BINARY("go_appengine_binary", RuleType.BINARY),
    GO_LIBRARY("go_library", RuleType.LIBRARY),
    GO_SOURCE("go_source", RuleType.LIBRARY),
    GO_APPENGINE_LIBRARY("go_appengine_library", RuleType.LIBRARY),
    GO_PROTO_LIBRARY("go_proto_library", RuleType.LIBRARY),
    GO_WRAP_CC("go_wrap_cc", RuleType.LIBRARY),
    GO_WEB_TEST("go_web_test", RuleType.TEST),
    ;

    private final String myKindString;
    private final RuleType myRuleType;

    GoRuleTypes(String kindString, RuleType ruleType) {
        myKindString = kindString;
        myRuleType = ruleType;
    }

    public String getKindString() {
        return myKindString;
    }

    public RuleType getRuleType() {
        return myRuleType;
    }
}
