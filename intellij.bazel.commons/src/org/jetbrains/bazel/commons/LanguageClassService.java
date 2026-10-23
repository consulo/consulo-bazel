package org.jetbrains.bazel.commons;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// consulo: upstream declares this service in LanguageClass.kt; Consulo's annotation processor needs a top-level class,
// so it lives in its own file with the upstream fully qualified name
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class LanguageClassService {
    private final List<LanguageClass> myLanguages;

    private final Map<String, LanguageClass> myRecognizedExtensions;

    @Inject
    public LanguageClassService() {
        List<LanguageClass> languages = new ArrayList<>();
        LanguageClassProvider.getEp().forEachExtensionSafe(it -> languages.addAll(it.getLanguages()));
        myLanguages = languages;

        Map<String, LanguageClass> recognizedExtensions = new HashMap<>();
        for (LanguageClass lang : myLanguages) {
            for (String ext : lang.recognizedFilenameExtensions()) {
                if (recognizedExtensions.get(ext) != null) {
                    throw new IllegalStateException("Duplicated recognized filename extension '" + ext + "'");
                }
                recognizedExtensions.put(ext, lang);
            }
        }
        myRecognizedExtensions = recognizedExtensions;
    }

    public @Nullable LanguageClass fromName(String languageName) {
        for (LanguageClass it : myLanguages) {
            if (it.languageName().equals(languageName)) {
                return it;
            }
        }
        return null;
    }

    public @Nullable LanguageClass fromExtension(String filenameExtension) {
        return myRecognizedExtensions.get(filenameExtension);
    }

    public @Nullable LanguageClass fromPath(Path path) {
        // consulo: Kotlin Path.extension
        Path fileName = path.getFileName();
        String name = fileName == null ? "" : fileName.toString();
        int dotIndex = name.lastIndexOf('.');
        return myRecognizedExtensions.get(dotIndex < 0 ? "" : name.substring(dotIndex + 1));
    }

    public @Nullable LanguageClass fromPath(String path) {
        int dotIndex = path.lastIndexOf('.');
        return myRecognizedExtensions.get(dotIndex < 0 ? "" : path.substring(dotIndex + 1));
    }

    public static LanguageClassService getInstance() {
        return Application.get().getInstance(LanguageClassService.class);
    }
}
