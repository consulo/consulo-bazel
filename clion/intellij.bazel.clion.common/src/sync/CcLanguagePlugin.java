package org.jetbrains.bazel.clion.sync;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CIdeInfo;
import com.intellij.aspect.lib.ReadTarget;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.clion.sync.CcBuildTarget.CompilationContext;
import org.jetbrains.bazel.clion.sync.CcBuildTarget.RuleContext;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class CcLanguagePlugin implements LanguagePlugin {
    public CcLanguagePlugin() {
    }

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(CcBuildTarget.class, CcToolchainBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(CcLanguageClass.CC_LANGUAGE_CLASS);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(IntellijIdeInfo.TargetIdeInfo target) {
        if (target.hasCIdeInfo() || target.hasCToolchainIdeInfo() || target.hasXcodeIdeInfo()) {
            return List.of(CcLanguageClass.CC_LANGUAGE_CLASS);
        }
        else {
            return List.of();
        }
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        IntellijIdeInfo.TargetIdeInfo target
    ) {
        if (target.hasCIdeInfo()) {
            return mapIdeInfo(target.getCIdeInfo(), server.getOutputParser()).thenApply(it -> List.<BuildTargetData>of(it));
        }
        else if (target.hasCToolchainIdeInfo()) {
            return mapToolchainIdeInfo(target.getCToolchainIdeInfo(), server.getOutputParser(), server.getOutputResolver())
                .thenApply(it -> List.<BuildTargetData>of(it));
        }
        else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    // consulo: upstream private top-level suspend functions of CcLanguagePlugin.kt; the suspend calls run sequentially,
    // in the same order

    private static CompletableFuture<CcBuildTarget> mapIdeInfo(CIdeInfo info, OutputLocationParser outputParser) {
        CompletableFuture<@Nullable RuleContext> ruleContext = info.hasRuleContext()
            ? mapRuleContext(info.getRuleContext(), outputParser).thenApply(it -> it)
            : CompletableFuture.completedFuture(null);

        return ruleContext.thenCompose(mappedRuleContext ->
            mapCompilationContext(info.getCompilationContext(), outputParser).thenApply(compilationContext ->
                new CcBuildTarget(mappedRuleContext, compilationContext)
            )
        );
    }

    private static CompletableFuture<RuleContext> mapRuleContext(CIdeInfo.RuleContext ctx, OutputLocationParser outputParser) {
        return OutputLocationCollectionBuilder.build(ctx.getHeadersList(), outputParser).thenCompose(headers ->
            OutputLocationCollectionBuilder.build(ctx.getTextualHeadersList(), outputParser).thenApply(textualHeaders ->
                new RuleContext(
                    headers,
                    textualHeaders,
                    StringsCollection.of(ctx.getCoptsList()),
                    StringsCollection.of(ctx.getConlyoptsList()),
                    StringsCollection.of(ctx.getCxxoptsList()),
                    StringsCollection.of(ctx.getArgsList()),
                    ctx.getIncludePrefix(),
                    ctx.getStripIncludePrefix()
                )
            )
        );
    }

    private static CompletableFuture<CompilationContext> mapCompilationContext(
        CIdeInfo.CompilationContext ctx,
        OutputLocationParser outputParser
    ) {
        return OutputLocationCollectionBuilder.build(ctx.getHeadersList(), outputParser).thenCompose(headers ->
            OutputLocationCollectionBuilder.build(ctx.getDirectHeadersList(), outputParser).thenCompose(directHeaders ->
                OutputLocationCollectionBuilder.buildExecroot(ctx.getIncludesList(), outputParser).thenCompose(includes ->
                    OutputLocationCollectionBuilder.buildExecroot(ctx.getQuoteIncludesList(), outputParser).thenCompose(quoteIncludes ->
                        OutputLocationCollectionBuilder.buildExecroot(ctx.getSystemIncludesList(), outputParser).thenApply(systemIncludes ->
                            new CompilationContext(
                                headers,
                                directHeaders,
                                StringsCollection.of(ctx.getDefinesList()),
                                includes,
                                quoteIncludes,
                                systemIncludes
                            )
                        )
                    )
                )
            )
        );
    }

    private static CompletableFuture<CcToolchainBuildTarget> mapToolchainIdeInfo(
        IntellijIdeInfo.CToolchainIdeInfo info,
        OutputLocationParser parser,
        OutputLocationResolver resolver
    ) {
        // we don't need to store them, they just need to be hardlinked
        return parser.parse(info.getAllFilesList()).thenCompose(ignored ->
            OutputLocationCollectionBuilder.buildExecroot(info.getBuiltInIncludeDirectoryList(), parser).thenCompose(builtInIncludeDirectories ->
                mapXcodeIdeInfo(info, parser, resolver).thenApply(xcodeInfo -> {
                    @Nullable OutputLocation sysroot =
                        info.getSysroot().isEmpty() ? null : parser.parseExecrootPath(info.getSysroot());

                    return new CcToolchainBuildTarget(
                        info.getTargetName(),
                        info.getCompilerName(),
                        StringsCollection.of(info.getCppOptionList()),
                        StringsCollection.of(info.getCOptionList()),
                        parser.parseExecrootPath(info.getCCompiler()),
                        parser.parseExecrootPath(info.getCppCompiler()),
                        builtInIncludeDirectories,
                        sysroot,
                        new LinkedHashMap<>(info.getCEnvironmentMap()),
                        new LinkedHashMap<>(info.getCppEnvironmentMap()),
                        xcodeInfo
                    );
                })
            )
        );
    }

    private static CompletableFuture<CcToolchainBuildTarget.@Nullable XcodeInfo> mapXcodeIdeInfo(
        IntellijIdeInfo.CToolchainIdeInfo info,
        OutputLocationParser parser,
        OutputLocationResolver resolver
    ) {
        if (!info.hasXcodeInfo()) {
            return CompletableFuture.completedFuture(null);
        }

        return parser.parse(info.getXcodeInfo()).thenApply(location -> {
            Path path = resolver.resolve(location);
            if (path == null) {
                return null;
            }
            Map<String, String> environment = ReadTarget.readEnvFromFile(path);
            if (environment == null) {
                return null;
            }

            return new CcToolchainBuildTarget.XcodeInfo(
                environment.getOrDefault("DEVELOPER_DIR", ""),
                environment.getOrDefault("SDKROOT", ""),
                environment.getOrDefault("XCODE_VERSION_OVERRIDE", "")
            );
        });
    }
}
