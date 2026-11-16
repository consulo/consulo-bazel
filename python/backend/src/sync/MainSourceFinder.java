package com.intellij.bazel.python.backend.sync;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.PythonTargetInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.util.io.FileUtil;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MainSourceFinder {
    private MainSourceFinder() {
    }

    public static @Nullable ArtifactLocation findMainFile(
        TargetIdeInfo target,
        PythonTargetInfo pythonTarget
    ) {
        boolean mainFileDeclared = !pythonTarget.getMain().getRelativePath().isEmpty();
        if (mainFileDeclared) {
            return pythonTarget.getMain();
        }
        else {
            return findMainFileAmongSources(target);
        }
    }

    private static @Nullable ArtifactLocation findMainFileAmongSources(TargetIdeInfo target) {
        List<ArtifactLocation> sources = target.getSrcsList();
        if (sources.size() == 1) {
            return sources.get(0);
        }
        else {
            Label label = Label.parse(target.getKey().getLabel());
            for (ArtifactLocation it : sources) {
                Path path = Path.of(it.getRelativePath());
                // when having multiple sources, target //aaa/bbb:ccc/ddd will choose aaa/bbb/ccc/ddd.py as its main file
                List<String> allLabelSegments = new ArrayList<>(label.packagePath().pathSegments());
                allLabelSegments.addAll(Arrays.asList(label.getTargetName().split("/", -1)));
                List<String> allPathSegments = new ArrayList<>(segments(path.getParent()));
                // consulo: Kotlin Path.nameWithoutExtension
                allPathSegments.add(FileUtil.getNameWithoutExtension(path.getFileName().toString()));
                if (allLabelSegments.equals(allPathSegments)) {
                    return it;
                }
            }
            return null;
        }
    }

    private static List<String> segments(@Nullable Path receiver) {
        if (receiver == null || receiver.getNameCount() == 1 && receiver.toString().isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < receiver.getNameCount(); i++) {
            result.add(receiver.getName(i).toString());
        }
        return result;
    }
}
