package org.jetbrains.bazel.clion.workspace.copts;

import consulo.bazel.cpp.workspace.CcCompilerSwitches;
import org.jetbrains.bazel.clion.workspace.CcImportContext;
import org.jetbrains.bazel.clion.workspace.utils;

import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * A compile option that carries a path.
 * <p>
 * consulo: the CIDR {@code CompilerSpecificSwitchBuilder} is {@link CcCompilerSwitches.Builder}; upstream {@code apply} is
 * a suspend function that does not suspend, it is synchronous here
 */
interface CoptsProcessor {
    String getFlag();

    void apply(CcImportContext ctx, CcCompilerSwitches.Builder builder, String value);
}

class GnuCoptsProcessor implements CoptsProcessor {
    private final String myFlag;
    private final String mySeparator;
    private final BiConsumer<CcCompilerSwitches.Builder, String> myBody;

    GnuCoptsProcessor(String flag, String separator, BiConsumer<CcCompilerSwitches.Builder, String> body) {
        myFlag = flag;
        mySeparator = separator;
        myBody = body;
    }

    // consulo: overload for the upstream default value of separator
    GnuCoptsProcessor(String flag, BiConsumer<CcCompilerSwitches.Builder, String> body) {
        this(flag, "", body);
    }

    @Override
    public String getFlag() {
        return myFlag;
    }

    private String resolvePath(CcImportContext ctx, String value) {
        // GCC and Clang replace a `=` prefix of an include path with the sysroot. Keep such a path as it is.
        if (value.startsWith("=")) {
            return value;
        }

        Path resolved = utils.resolveExecutionRootPath(ctx, value);
        return resolved != null ? resolved.toString() : value;
    }

    @Override
    public void apply(CcImportContext ctx, CcCompilerSwitches.Builder builder, String value) {
        myBody.accept(builder, resolvePath(ctx, value.startsWith(mySeparator) ? value.substring(mySeparator.length()) : value));
    }
}
