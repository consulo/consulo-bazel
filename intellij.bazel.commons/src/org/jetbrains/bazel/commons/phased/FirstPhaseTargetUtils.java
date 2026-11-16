package org.jetbrains.bazel.commons.phased;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import org.jetbrains.bsp.protocol.BuildTargetTag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FirstPhaseTargetUtils {
    private FirstPhaseTargetUtils() {
    }

    public static String getName(Target receiver) {
        return receiver.getRule().getName();
    }

    public static String getKind(Target receiver) {
        return receiver.getRule().getRuleClass();
    }

    private static final String TAGS_NAME = "tags";

    public static List<String> getTags(Target receiver) {
        return getListAttribute(receiver, TAGS_NAME);
    }

    private static final String SRCS_NAME = "srcs";

    public static List<String> getSrcs(Target receiver) {
        return getListAttribute(receiver, SRCS_NAME);
    }

    private static final String RESOURCES_NAME = "resources";

    public static List<String> getResources(Target receiver) {
        return getListAttribute(receiver, RESOURCES_NAME);
    }

    private static final List<String> compileDeps = List.of("deps", "jars", "exports", "associates", "proc_macro_deps");
    private static final List<String> runtimeDeps = List.of("runtime_deps");

    public static List<String> getInterestingDeps(Target receiver) {
        List<String> names = new ArrayList<>(compileDeps);
        names.addAll(runtimeDeps);
        List<String> result = new ArrayList<>();
        for (String it : names) {
            result.addAll(getListAttribute(receiver, it));
        }
        return result;
    }

    private static final String GENERATOR_NAME = "generator_name";

    public static @Nullable String getGeneratorName(Target receiver) {
        return getStringAttribute(receiver, GENERATOR_NAME);
    }

    static List<String> getListAttribute(Target receiver, String name) {
        for (Build.Attribute it : receiver.getRule().getAttributeList()) {
            if (it.getName().equals(name)) {
                return List.copyOf(it.getStringListValueList());
            }
        }
        return List.of();
    }

    static @Nullable String getStringAttribute(Target receiver, String name) {
        for (Build.Attribute it : receiver.getRule().getAttributeList()) {
            if (it.getName().equals(name)) {
                return it.getStringValue();
            }
        }
        return null;
    }

    public static boolean isManual(Target receiver) {
        return getTags(receiver).contains(BuildTargetTag.MANUAL);
    }

    public static boolean isNoIde(Target receiver) {
        return getTags(receiver).contains(BuildTargetTag.NO_IDE);
    }
}
