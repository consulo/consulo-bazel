package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.build.ui.event.MessageEvent;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class CcCompilerResolver {
    private final CcImportContext myCtx;

    CcCompilerResolver(CcImportContext ctx) {
        myCtx = ctx;
    }

    record Result(CcCompilerKind kind, Path path) {
    }

    private final Map<OutputLocation, @Nullable Result> myCache = new LinkedHashMap<>();

    @Nullable Result resolve(OutputLocation location, CcToolEnvironment environment) {
        // consulo: Kotlin getOrPutIfMissing, a null result is cached as well
        if (myCache.containsKey(location)) {
            return myCache.get(location);
        }
        Result result = doResolve(location, environment);
        myCache.put(location, result);
        return result;
    }

    private @Nullable Result doResolve(OutputLocation location, CcToolEnvironment environment) {
        Path path = utils.resolveOutputLocation(myCtx, location);
        if (path == null) {
            return null;
        }
        // TODO consulo: CIDR OCCompilerResolver.resolve(project, path, environment) identifies the compiler kind by
        //  running the compiler; consulo-cpp has no compiler resolver, the kind stays unknown
        //  val kind = OCCompilerResolver.resolve(ctx.project, path, environment)
        CcCompilerKind kind = CcCompilerKind.UNKNOWN;

        return new Result(mapKind(kind), path);
    }

    private CcCompilerKind mapKind(CcCompilerKind kind) {
        String id = kind.getId();
        if ("CLANG".equals(id)) {
            return CcCompilerKind.CLANG;
        }
        else if ("GCC".equals(id)) {
            return CcCompilerKind.GCC;
        }
        else if ("APPLE_CLANG".equals(id)) {
            return CcCompilerKind.APPLE_CLANG;
        }
        else {
            return kind;
        }
    }

    void reportProblems() {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<OutputLocation, @Nullable Result> entry : myCache.entrySet()) {
            String compilerPath = String.valueOf(utils.resolveOutputLocation(myCtx, entry.getKey()));
            Result result = entry.getValue();

            if (result == null) {
                problems.add(BazelCLionCommonLocalize.ccCompilerPathResolveFailed(compilerPath).get());
            }
            else if (result.kind().isUnknown()) {
                problems.add(BazelCLionCommonLocalize.ccCompilerKindResolveFailed(compilerPath).get());
            }
        }

        if (problems.isEmpty()) {
            return;
        }

        myCtx.reportEvent(
            MessageEvent.Kind.WARNING,
            BazelCLionCommonLocalize.ccCompilerResolveFailed(),
            String.join("\n", problems)
        );
    }
}
