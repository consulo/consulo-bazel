package org.jetbrains.bazel.clion;

import consulo.application.util.registry.Registry;

public final class BazelCLionFeatureFlags {
    private BazelCLionFeatureFlags() {
    }

    public static final String CLION_ENABLED = "bazel.clion.enable";

    public static boolean isCLionEnabled() {
        return isEnabled(CLION_ENABLED);
    }

    @SuppressWarnings("deprecation")
    private static boolean isEnabled(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            // consulo: Kotlin String.toBooleanStrict()
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("The string doesn't represent a boolean value: " + value);
        }

        // consulo: the key is not one of consulo.bazel.commons.BazelRegistryKeys (registered by upstream
        // intellij.bazel.clion.common.xml with defaultValue="false", which the CLion product turns on). The C/C++ library is
        // loaded only together with consulo.cpp, so it is on by default here
        return Registry.is(key, true);
    }
}
