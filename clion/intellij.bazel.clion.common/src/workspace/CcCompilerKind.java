package org.jetbrains.bazel.clion.workspace;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An {@code OCCompilerKind} wrapper that disables response file usage.
 * <p>
 * Bazel's compiler wrappers cannot read the response files, CLion writes. This kind delegates
 * all behavior to the underlying {@code delegate} kind, but overrides {@code getCompilerInstance} to return
 * a {@code CcGccCompiler}, which reports a no-op command line shortener.
 * <p>
 * TODO consulo: consulo-cpp has no compiler kinds (CIDR {@code OCCompilerKind}, {@code OCCompilerId}): this class only
 * keeps the identity of the kind (the CIDR compiler id, e.g. {@code GCC}). The CIDR behavior it delegates and overrides
 * (switch builders, {@code skipLanguageNotRelatedSwitches}, {@code fixPchSwitches}, the command line shortener and the
 * {@code GCCCompiler} instance used to collect the compiler info) is not ported.
 */
public final class CcCompilerKind {
    public static final CcCompilerKind GCC = new CcCompilerKind("GCC");

    public static final CcCompilerKind CLANG = new CcCompilerKind("CLANG");

    public static final CcCompilerKind APPLE_CLANG = new CcCompilerKind("APPLE_CLANG");

    /**
     * consulo: stands for CIDR's {@code UnknownCompilerKind}, the kind of a compiler that was not identified.
     */
    public static final CcCompilerKind UNKNOWN = new CcCompilerKind(null);

    // consulo: upstream delegates to the CIDR kind, here its id (OCCompilerId.toString(), null when unknown)
    private final @Nullable String myDelegate;

    private CcCompilerKind(@Nullable String delegate) {
        myDelegate = delegate;
    }

    /**
     * The CIDR compiler id ({@code OCCompilerId.toString()}): {@code GCC}, {@code CLANG}, {@code APPLE_CLANG};
     * {@code null} for {@link #UNKNOWN}.
     */
    public @Nullable String getId() {
        return myDelegate;
    }

    /**
     * consulo: CIDR's {@code OCCompilerKind.isUnknown()}
     */
    public boolean isUnknown() {
        return myDelegate == null;
    }

    @Override
    public String toString() {
        return "Cc(" + (myDelegate == null ? "Unknown" : myDelegate) + ")";
    }

    // TODO consulo: CIDR OCCompilerKind overrides, no consulo-cpp API
    //  override fun skipLanguageNotRelatedSwitches(switches: List<String?>): List<String?> = delegate.skipLanguageNotRelatedSwitches(switches)
    //  override fun fixPchSwitches(switches: List<String?>): List<String?> = delegate.fixPchSwitches(switches)
    //  override fun getCommandLineShortener(): OCCompilerCommandLineShortener = BasicCompilerCommandLineShortener()
    //  override fun getCompilerInstance(project, compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool): OCCompiler =
    //    CcGccCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool)
    //  override fun getCompilerInstance(project, compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool, cache): OCCompiler =
    //    CcGccCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool)
}

// TODO consulo: CIDR GCCCompiler does not exist in consulo-cpp
//  private class CcGccCompiler(compilerExecutable: File, compilerWorkingDirectory: File, environment: CidrToolEnvironment,
//    tempFilesPool: TempFilesPool) : GCCCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool) {
//    // TODO: can we remove this duplicated entry point on the GCCCompiler?
//    override fun getCommandLineShortener(): OCCompilerCommandLineShortener = BasicCompilerCommandLineShortener()
//  }

// TODO consulo: CIDR extension cidr.lang.compilerKindProvider (OCCompilerKindProvider) does not exist in consulo-cpp,
//  the class is not registered
class CcCompilerKindProvider {
    public CcCompilerKindProvider() {
    }

    public List<CcCompilerKind> getCompilerKinds() {
        return List.of(CcCompilerKind.GCC, CcCompilerKind.CLANG, CcCompilerKind.APPLE_CLANG);
    }
}
