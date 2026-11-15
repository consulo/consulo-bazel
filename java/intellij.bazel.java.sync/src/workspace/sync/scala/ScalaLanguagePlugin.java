package org.jetbrains.bazel.sync.workspace.languages.scala;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.scala.sdk.ScalaSdkResolver;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.StringsCollection;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl(id = "ScalaLanguagePlugin")
public class ScalaLanguagePlugin implements LanguagePlugin {
    public ScalaLanguagePlugin() {
    }

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(ScalaBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(JavaLanguageClass.SCALA);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        return target.hasScalaTargetInfo() ? List.of(JavaLanguageClass.SCALA) : List.of();
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        TargetIdeInfo target
    ) {
        if (!target.hasScalaTargetInfo()) {
            return CompletableFuture.completedFuture(List.of());
        }
        String scalaVersion = ScalaSdkResolver.resolveScalaVersion(target);
        if (scalaVersion == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return OutputLocationCollectionBuilder.build(target.getScalaTargetInfo().getCompilerClasspathList(), server.getOutputParser())
            .thenApply(sdkJars -> List.<BuildTargetData>of(
                new ScalaBuildTarget(
                    scalaVersion,
                    sdkJars,
                    StringsCollection.of(target.getScalaTargetInfo().getScalacOptsList()),
                    target.getScalaTargetInfo().getScalatestClasspathTargetsList().stream().map(Label::parse).toList()
                )
            ));
    }
}
