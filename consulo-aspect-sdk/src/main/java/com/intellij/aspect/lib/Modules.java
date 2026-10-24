// consulo: upstream generates this enum (as Kotlin) at build time with //private/rules:kt_aspect_enum.bzl
// from sdk/lib/BUILD: kt_aspect_enum(modules = MODULES + [rules_proto_info]), MODULES from config/aspect.bzl.
// This is a hand-written Java equivalent: when the upstream config changes, update it as described in
// src/main/aspect/README.md.

package com.intellij.aspect.lib;

import java.util.List;

public enum Modules {
    RUN_INFO("run_info", List.of()),
    TEST_INFO("test_info", List.of()),
    XCODE_INFO("xcode_info", List.of(Rules.CC)),
    CC_INFO("cc_info", List.of(Rules.CC)),
    CC_TOOLCHAIN_INFO("cc_toolchain_info", List.of(Rules.CC)),
    JAVA_INFO("java_info", List.of(Rules.JAVA)),
    JAVA_TOOLCHAIN_INFO("java_toolchain_info", List.of(Rules.JAVA)),
    JAVA_COMMON_INFO("java_common_info", List.of(Rules.JAVA, Rules.KOTLIN, Rules.SCALA)),
    JVM_INFO("jvm_info", List.of(Rules.JAVA, Rules.KOTLIN, Rules.SCALA)),
    KOTLIN_INFO("kotlin_info", List.of(Rules.KOTLIN)),
    SCALA_INFO("scala_info", List.of(Rules.SCALA)),
    GO_INFO("go_info", List.of(Rules.GO)),
    PY_INFO("py_info", List.of(Rules.PYTHON)),
    PYTHON_INFO("python_info", List.of(Rules.PYTHON)),
    PROTOBUF_INFO("protobuf_info", List.of(Rules.PROTO)),
    PROTO_INFO("proto_info", List.of(Rules.PROTO, Rules.LEGACY_RULES_PROTO)),
    RULES_PROTO_INFO("rules_proto_info", List.of(Rules.LEGACY_RULES_PROTO));

    private final String myFile;
    private final List<Rules> myRulesets;

    Modules(String file, List<Rules> rulesets) {
        myFile = file;
        myRulesets = rulesets;
    }

    public String getFile() {
        return myFile;
    }

    public List<Rules> getRulesets() {
        return myRulesets;
    }
}
