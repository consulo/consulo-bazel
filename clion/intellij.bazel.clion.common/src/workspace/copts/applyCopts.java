package org.jetbrains.bazel.clion.workspace.copts;

import consulo.bazel.cpp.workspace.CcCompilerSwitches;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jetbrains.bazel.clion.workspace.CcImportContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// consulo: upstream Kotlin top-level declarations of applyCopts.kt. The CIDR CompilerSpecificSwitchBuilder is
// CcCompilerSwitches.Builder, OCCompilerKind is CcCompilerKind; upstream suspend functions that do not suspend are
// synchronous here
public final class applyCopts {
    private applyCopts() {
    }

    // consulo: the CIDR OCCompilerId values, as CcCompilerKind.getId()
    private static final Set<String> GNU_COMPILERS = Set.of("GCC", "CLANG", "APPLE_CLANG");

    /**
     * The GNU spelling of the path options.
     */
    private static final List<CoptsProcessor> GNU_PROCESSORS = List.of(
        new GnuCoptsProcessor("-I", CcCompilerSwitches.Builder::withIncludePath),
        new GnuCoptsProcessor("-isystem", CcCompilerSwitches.Builder::withSystemIncludePath),
        new GnuCoptsProcessor("-iquote", CcCompilerSwitches.Builder::withQuoteIncludePath),
        new GnuCoptsProcessor("--sysroot", "=", CcCompilerSwitches.Builder::withSysroot)
    );

    private static final List<CoptsProcessor> MSVC_PROCESSORS = List.of(
        new MsvcCoptsProcessor("/I", CcCompilerSwitches.Builder::withIncludePath),
        new MsvcCoptsProcessor("/external:I", CcCompilerSwitches.Builder::withSystemIncludePath)
    );

    private static final List<CoptsProcessor> CLANG_CL_PROCESSORS = concat(MSVC_PROCESSORS, List.of(
        new GnuCoptsProcessor("/clang:-I", "/clang:", CcCompilerSwitches.Builder::withIncludePath),
        new GnuCoptsProcessor("/clang:-iquote", "/clang:", CcCompilerSwitches.Builder::withQuoteIncludePath),
        new GnuCoptsProcessor("/clang:-isystem", "/clang:", CcCompilerSwitches.Builder::withSystemIncludePath)
    ));

    // consulo: Kotlin `list + list`
    private static List<CoptsProcessor> concat(List<CoptsProcessor> first, List<CoptsProcessor> second) {
        List<CoptsProcessor> result = new ArrayList<>(first);
        result.addAll(second);
        return List.copyOf(result);
    }

    /**
     * The GNU and Clang spelling of the forward options.
     */
    private static final List<CoptsForwarder> GNU_FORWARDERS = List.of(
        new CoptsForwardNext("-Xclang"),
        new CoptsForwardNext("-Xclangas"),
        new CoptsForwardNext("-Xanalyzer"),
        new CoptsForwardNext("-Xpreprocessor"),
        new CoptsForwardNext("-Xassembler"),
        new CoptsForwardNext("-Xlinker"),
        new CoptsForwardNext("-mllvm"),
        new CoptsForwardNext("-Xarch_", true), // the arch follows the flag, e.g. -Xarch_x86_64
        new CoptsForwardNext("-Xopenmp-target", true), // the target triple can follow the flag, e.g. -Xopenmp-target=nvptx64
        new CoptsForwardCommaSeparated("-Wp"),
        new CoptsForwardCommaSeparated("-Wa"),
        new CoptsForwardCommaSeparated("-Wl")
    );

    private record CoptsForwardGroup(CoptsForwarder forwarder, CoptsProcessorStream stream) {
    }

    private static final class CoptsProcessorStream {
        private final CcCompilerSwitches.Builder mySink;
        private final List<CoptsProcessor> myProcessors;

        CoptsProcessorStream(CcCompilerSwitches.Builder sink, List<CoptsProcessor> processors) {
            mySink = sink;
            myProcessors = processors;
        }

        // consulo: upstream `kind.getSwitchBuilder(CidrSwitchBuilder())`
        CoptsProcessorStream(CcCompilerKind kind, List<CoptsProcessor> processors) {
            this(CcCompilerSwitches.getBuilder(kind), processors);
        }

        CcCompilerSwitches.Builder getSink() {
            return mySink;
        }

        private @Nullable CoptsProcessor myPending;

        void consume(CcImportContext ctx, String option) {
            // Bazel writes an empty option when a make variable expands to nothing. A blank value drops its flag as well.
            if (option.isBlank()) {
                myPending = null;
                return;
            }

            CoptsProcessor pending = myPending;
            if (pending != null) {
                pending.apply(ctx, mySink, option);
                myPending = null;
                return;
            }

            @Nullable CoptsProcessor processor = null;
            for (CoptsProcessor it : myProcessors) {
                if (option.startsWith(it.getFlag())) {
                    processor = it;
                    break;
                }
            }
            if (processor == null) {
                mySink.withSwitch(option);
                return;
            }

            if (option.equals(processor.getFlag())) {
                // the next option is the value; for example, -I include/foo
                myPending = processor;
            }
            else {
                // the value follows the flag; for example, -Iinclude/foo or --sysroot=path
                processor.apply(ctx, mySink, option.substring(processor.getFlag().length()));
            }
        }
    }

    // consulo: upstream `context(ctx: CcImportContext) suspend fun CompilerSpecificSwitchBuilder.applyCopts(kind, options)`
    public static void applyCopts(CcImportContext ctx, CcCompilerSwitches.Builder receiver, CcCompilerKind kind, List<String> options) {
        List<CoptsProcessor> processors;
        List<CoptsForwarder> forwarders;
        // consulo: Set.of rejects a null lookup, the id of an unknown kind is null; the ids are the CIDR OCCompilerId values
        @Nullable String id = kind.getId();
        if (id != null && GNU_COMPILERS.contains(id)) {
            processors = GNU_PROCESSORS;
            forwarders = GNU_FORWARDERS;
        }
        else if ("MSVC".equals(id)) {
            processors = MSVC_PROCESSORS;
            forwarders = List.of();
        }
        else if ("CLANG_CL".equals(id)) {
            processors = CLANG_CL_PROCESSORS;
            forwarders = List.of();
        }
        else {
            processors = List.of();
            forwarders = List.of();
        }

        CoptsProcessorStream stream = new CoptsProcessorStream(receiver, processors);
        Map<String, CoptsForwardGroup> forwardStreams = new LinkedHashMap<>();

        Iterator<String> iterator = options.iterator();
        options:
        while (iterator.hasNext()) {
            String option = iterator.next();

            for (CoptsForwarder forwarder : forwarders) {
                String match = forwarder.match(option);
                if (match == null) {
                    continue;
                }
                CoptsForwardGroup group = forwardStreams.computeIfAbsent(
                    match,
                    it -> new CoptsForwardGroup(forwarder, new CoptsProcessorStream(kind, processors))
                );

                for (String it : forwarder.expand(option, iterator)) {
                    group.stream().consume(ctx, it);
                }
                continue options;
            }

            stream.consume(ctx, option);
        }

        forwardStreams.forEach((flag, group) -> group.forwarder().apply(receiver, flag, group.stream().getSink().buildRaw()));
    }
}
