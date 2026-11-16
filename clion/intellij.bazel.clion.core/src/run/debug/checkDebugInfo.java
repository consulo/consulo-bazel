package org.jetbrains.bazel.clion.run.debug;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo;
import org.jetbrains.bazel.clion.workspace.CcCompilerInfoService;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// consulo: top-level declarations of upstream checkDebugInfo.kt
public final class checkDebugInfo {
    private static final Set<String> MSVC_DEBUG_INFO_FLAGS = Set.of("/Z7", "/Zi", "/ZI");

    // consulo: CIDR OCCompilerId (com.jetbrains.cidr.lang.workspace.compiler) does not exist; CcCompilerKind keeps the id as
    //   OCCompilerId.toString(), so the ids are compared as strings
    private static final String GCC_ID = "GCC";
    private static final String CLANG_ID = "CLANG";
    private static final String APPLE_CLANG_ID = "APPLE_CLANG";
    private static final String MSVC_ID = "MSVC";
    private static final String CLANG_CL_ID = "CLANG_CL";

    private static final Set<String> GNU_COMPILERS = Set.of(GCC_ID, CLANG_ID, APPLE_CLANG_ID);

    // the GCC and Clang flags that set the debug level, a debugger tuning like -glldb also enables the debug info
    private static final Pattern GCC_DEBUG_LEVEL_FLAG =
        Pattern.compile("-g(\\d|gdb\\d?|dwarf(-\\d)?|lldb|sce|dbx|line-tables-only|line-directives-only)?");

    private checkDebugInfo() {
    }

    /**
     * consulo: stands for {@code com.intellij.util.system.OS}, which has no Consulo counterpart (consulo.platform describes only the
     * machine the IDE runs on)
     */
    public enum OS {
        Linux,
        macOS,
        Windows,
        Other;

        static OS current() {
            PlatformOperatingSystem os = Platform.current().os();
            if (os.isLinux()) {
                return Linux;
            }
            else if (os.isMac()) {
                return macOS;
            }
            else if (os.isWindows()) {
                return Windows;
            }
            else {
                return Other;
            }
        }
    }

    // consulo: upstream public top-level class of checkDebugInfo.kt
    public static class CcDebugInfoCheck {
        private final CcDebugContext myCtx;
        private final List<CcCompilerInfo> myCompilers;

        public CcDebugInfoCheck(CcDebugContext ctx, List<CcCompilerInfo> compilers) {
            myCtx = ctx;
            myCompilers = compilers;
        }

        public record Issue(Label label, CcDebugInfo.Action action, CcDebugInfo.CompilationMode compilationMode) {

            @Override
            public String toString() {
                return label + " (" + action.getMnemonic() + ", " + compilationMode.name().toLowerCase(Locale.ROOT) + "): " +
                    String.join(" ", action.getArgumentsList());
            }
        }

        public record Result(List<Issue> issues) {

            /**
             * Returns true if {@code --compilation_mode=dbg} might add the missing debug info.
             */
            public boolean isFixableByCompilationMode() {
                return issues.stream().allMatch(it -> it.compilationMode() != CcDebugInfo.CompilationMode.DBG);
            }
        }

        /**
         * Returns null if no toolchain of the last sync uses the executable of the action.
         */
        private @Nullable CcCompilerKind resolveCompilerKind(CcDebugInfo.Action action) {
            if (action.getArgumentsList().isEmpty()) {
                return null;
            }
            String executable = action.getArgumentsList().get(0);

            for (CcCompilerInfo it : myCompilers) {
                if (it.cCompiler().endsWith(executable)) {
                    return it.cCompilerKind();
                }
                else if (it.cppCompiler().endsWith(executable)) {
                    return it.cppCompilerKind();
                }
            }
            return null;
        }

        /**
         * Returns the compiler kind of the linker action, assumes that the linker matches the compiler or
         * the compiler is used for linking as well.
         */
        private @Nullable CcCompilerKind resolveLinkerKind(TargetIdeInfo target) {
            for (CcDebugInfo.Action action : compileActions(target)) {
                CcCompilerKind kind = resolveCompilerKind(action);
                if (kind != null) {
                    return kind;
                }
            }
            for (CcDebugInfo.Action action : linkActions(target)) {
                CcCompilerKind kind = resolveCompilerKind(action);
                if (kind != null) {
                    return kind;
                }
            }
            return null;
        }

        /**
         * Reports the issues as one warning.
         */
        public Result check(Collection<TargetIdeInfo> targets) {
            Result result = findDebugInfoIssues(targets);
            if (!result.issues().isEmpty()) {
                reportIssues(result);
            }

            return result;
        }

        private Result findDebugInfoIssues(Collection<TargetIdeInfo> targets) {
            List<Issue> compileIssues = new ArrayList<>();
            for (TargetIdeInfo target : targets) {
                for (CcDebugInfo.Action it : compileActions(target)) {
                    if (!hasCompileDebugInfo(it, resolveCompilerKind(it))) {
                        compileIssues.add(new Issue(Label.label(target), it, compilationMode(target)));
                    }
                }
            }

            List<Issue> linkIssues = new ArrayList<>();
            for (TargetIdeInfo target : targets) {
                for (CcDebugInfo.Action it : linkActions(target)) {
                    if (!hasLinkDebugInfo(it, resolveLinkerKind(target), os(target))) {
                        linkIssues.add(new Issue(Label.label(target), it, compilationMode(target)));
                    }
                }
            }

            List<Issue> issues = new ArrayList<>(compileIssues);
            issues.addAll(linkIssues);
            return new Result(List.copyOf(issues));
        }

        private void reportIssues(Result result) {
            long targetCount = result.issues().stream().map(Issue::label).distinct().count();

            myCtx.reportEvent(
                /* severity = */ MessageEvent.Kind.WARNING,
                /* message = */ BazelCLionCoreLocalize.debugWarningNoDebugInfo(targetCount),
                /* description = */ result.issues().stream().map(Issue::toString).collect(Collectors.joining("\n"))
            );
        }
    }

    // consulo: context(ctx: CcDebugContext) -> ctx parameter
    public static CcDebugInfoCheck.Result checkCcDebugInfo(CcDebugContext ctx, Collection<TargetIdeInfo> targets) {
        List<CcCompilerInfo> compilers = List.copyOf(CcCompilerInfoService.getInstance(ctx.getProject()).all().values());
        return new CcDebugInfoCheck(ctx, compilers).check(targets);
    }

    public static boolean hasCompileDebugInfo(CcDebugInfo.Action receiver, @Nullable CcCompilerKind compilerKind) {
        List<String> argumentsList = receiver.getArgumentsList();
        if (hasAnyId(compilerKind, GNU_COMPILERS)) {
            return hasGccDebugInfo(argumentsList);
        }
        else if (hasId(compilerKind, MSVC_ID)) {
            return hasMsvcDebugInfo(argumentsList);
        }
        else if (hasId(compilerKind, CLANG_CL_ID)) {
            return hasGccDebugInfo(argumentsList) || hasClangClDebugInfo(argumentsList) || hasMsvcDebugInfo(argumentsList);
        }
        else {
            return hasGccDebugInfo(argumentsList) || hasMsvcDebugInfo(argumentsList);
        }
    }

    // consulo: CIDR extension functions OCCompilerKind?.hasId / hasAnyId
    private static boolean hasId(@Nullable CcCompilerKind kind, String id) {
        return kind != null && id.equals(kind.getId());
    }

    private static boolean hasAnyId(@Nullable CcCompilerKind kind, Set<String> ids) {
        return kind != null && kind.getId() != null && ids.contains(kind.getId());
    }

    /**
     * The last flag that sets the debug level wins. The other {@code -g} flags, only change the format of the debug info.
     */
    private static boolean hasGccDebugInfo(List<String> arguments) {
        String last = null;
        for (String it : arguments) {
            if (GCC_DEBUG_LEVEL_FLAG.matcher(it).matches()) {
                last = it;
            }
        }
        if (last == null) {
            return false;
        }
        return !last.equals("-g0") && !last.equals("-ggdb0");
    }

    private static boolean hasClangClDebugInfo(List<String> arguments) {
        return hasGccDebugInfo(arguments.stream()
            .map(checkDebugInfo::toMsvcOption)
            .filter(it -> it.startsWith("/clang:"))
            .map(it -> it.substring("/clang:".length()))
            .toList());
    }

    /**
     * The compiler options are case-sensitive, for example {@code /Zi} and {@code /ZI} are different options.
     */
    private static boolean hasMsvcDebugInfo(List<String> arguments) {
        return arguments.stream().anyMatch(it -> MSVC_DEBUG_INFO_FLAGS.contains(toMsvcOption(it)));
    }

    /**
     * MSVC accepts each option with a {@code /} or a {@code -} prefix.
     */
    private static String toMsvcOption(String receiver) {
        return receiver.startsWith("-") ? "/" + receiver.substring(1) : receiver;
    }

    public static boolean hasLinkDebugInfo(CcDebugInfo.Action receiver, @Nullable CcCompilerKind compilerKind, OS os) {
        List<String> argumentsList = receiver.getArgumentsList();
        if (hasAnyId(compilerKind, GNU_COMPILERS) && os == OS.macOS) {
            // on macOS the binary refers to the object files, and lldb finds them only with a relative path
            return !hasStripFlag(argumentsList) && argumentsList.stream().anyMatch(it -> it.contains("-oso_prefix,"));
        }
        else if (hasAnyId(compilerKind, GNU_COMPILERS)) {
            return !hasStripFlag(argumentsList);
        }
        else if (hasId(compilerKind, MSVC_ID)) {
            return hasMsvcDebugLinkFlag(argumentsList);
        }
        else {
            return true;
        }
    }

    /**
     * Bazel adds {@code -Wl,-S} for {@code --strip=always}, the linker then removes the debug info.
     */
    private static boolean hasStripFlag(List<String> arguments) {
        return arguments.stream()
            .anyMatch(it -> it.startsWith("-Wl,") && Arrays.asList(it.substring("-Wl,".length()).split(",", -1)).contains("-S"));
    }

    /**
     * The last /DEBUG flag wins, a plain /DEBUG is /DEBUG:FULL. The linker options are not case-sensitive.
     */
    private static boolean hasMsvcDebugLinkFlag(List<String> arguments) {
        String last = null;
        for (String argument : arguments) {
            String it = toMsvcOption(argument).toUpperCase(Locale.ROOT);
            if (it.equals("/DEBUG") || it.startsWith("/DEBUG:")) {
                last = it;
            }
        }
        return "/DEBUG".equals(last) || "/DEBUG:FULL".equals(last);
    }

    private static List<CcDebugInfo.Action> compileActions(TargetIdeInfo receiver) {
        return receiver.hasCcDebugInfo() ? receiver.getCcDebugInfo().getCompileActionsList() : List.of();
    }

    private static List<CcDebugInfo.Action> linkActions(TargetIdeInfo receiver) {
        return receiver.hasCcDebugInfo() ? receiver.getCcDebugInfo().getLinkActionsList() : List.of();
    }

    private static OS os(TargetIdeInfo receiver) {
        return switch (receiver.getPlatformInfo().getOs()) {
            case LINUX -> OS.Linux;
            case MACOS -> OS.macOS;
            case WINDOWS -> OS.Windows;
            default -> OS.current();
        };
    }

    private static CcDebugInfo.CompilationMode compilationMode(TargetIdeInfo receiver) {
        // consulo: upstream `ccDebugInfo?.compilationMode ?: UNRECOGNIZED`, the proto getter never returns null
        return receiver.getCcDebugInfo().getCompilationMode();
    }
}
