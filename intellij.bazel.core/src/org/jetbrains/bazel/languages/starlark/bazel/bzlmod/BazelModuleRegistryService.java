package org.jetbrains.bazel.languages.starlark.bazel.bzlmod;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelModuleRegistryService implements Disposable {
    private final Project myProject;
    private final MVStore myStore;
    private final MVMap<String, String> myMap;

    @Inject
    public BazelModuleRegistryService(Project project) {
        myProject = project;
        myStore = openStore(getProjectDataPath(project, "bzlmod/module-registry.mv"));
        myMap = myStore.openMap("registry");
    }

    private @Nullable String getResolverId() {
        return myMap.get(RESOLVER_ID_KEY);
    }

    private void setResolverId(@Nullable String value) {
        if (StringUtil.isEmptyOrSpaces(value)) {
            myMap.remove(RESOLVER_ID_KEY);
        }
        else {
            myMap.put(RESOLVER_ID_KEY, value);
        }
        myStore.commit();
    }

    private @Nullable BazelModuleResolver myActiveResolver = null;

    private @Nullable BazelModuleResolver findResolver() {
        BazelModuleResolver activeResolver = myActiveResolver;
        if (activeResolver != null && Objects.equals(activeResolver.getId(), getResolverId())) {
            return activeResolver;
        }
        // consulo: upstream BazelModuleResolver.EP_NAME.extensionList
        List<BazelModuleResolver> resolvers = Application.get().getExtensionList(BazelModuleResolver.class);
        if (StringUtil.isEmptyOrSpaces(getResolverId())) {
            setResolverId(resolvers.isEmpty() ? null : resolvers.get(0).getId());
        }
        String resolverId = getResolverId();
        myActiveResolver = resolvers.stream().filter(it -> Objects.equals(it.getId(), resolverId)).findFirst().orElse(null);
        return myActiveResolver;
    }

    public CompletableFuture<List<String>> getModuleNames() {
        BazelModuleResolver resolver = findResolver();
        if (resolver == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return resolver.getModuleNames(myProject).thenApply(it -> it != null ? it : List.of());
    }

    public CompletableFuture<List<String>> getModuleVersions(String moduleName) {
        BazelModuleResolver resolver = findResolver();
        if (resolver == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return resolver.getModuleVersions(myProject, moduleName).thenApply(it -> it != null ? it : List.of());
    }

    public CompletableFuture<?> refreshModuleNames() {
        BazelModuleResolver resolver = findResolver();
        if (resolver == null) {
            return CompletableFuture.completedFuture(null);
        }
        return resolver.refreshModuleNames(myProject);
    }

    @Override
    public void dispose() {
        try {
            myStore.close();
        }
        catch (Throwable e) {
            Logger
                .getInstance(BazelModuleRegistryService.class)
                .warn("Failed to close MVStore for Bazel module registry service", e);
        }
    }

    private static final String RESOLVER_ID_KEY = "resolverId";

    public static BazelModuleRegistryService getInstance(Project project) {
        return project.getInstance(BazelModuleRegistryService.class);
    }

    /**
     * Opens an MVStore instance for the given file path.
     * Creates the parent directories if needed.
     */
    private static MVStore openStore(Path path) {
        // consulo: Kotlin Path.createParentDirectories()
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        MVStore store = new MVStore.Builder()
            .fileName(path.toAbsolutePath().toString())
            .autoCommitDisabled()
            .open();
        store.setVersionsToKeep(0);
        return store;
    }

    // consulo: IntelliJ Project.getProjectDataPath(name)
    private static Path getProjectDataPath(Project project, String name) {
        return ProjectUtil.getProjectCachePath(project, "projects").resolve(name);
    }
}
