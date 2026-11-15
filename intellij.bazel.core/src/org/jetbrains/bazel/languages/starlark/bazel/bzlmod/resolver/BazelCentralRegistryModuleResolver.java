package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver;

import com.google.gson.Gson;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.http.HttpRequests;
import consulo.http.HttpVersion;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.languages.starlark.bazel.bzlmod.BazelModuleResolver;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@ExtensionImpl
public class BazelCentralRegistryModuleResolver implements BazelModuleResolver {
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Bazel Central Registry";
    }

    @Override
    public CompletableFuture<@Nullable List<String>> getModuleNames(Project project) {
        BazelModuleBcrCacheService bcrCache = project.getInstance(BazelModuleBcrCacheService.class);
        return bcrCache.getOrFetch(MODULE_NAMES_KEY, this::fetchModuleNamesFromRegistry).<@Nullable List<String>>thenApply(it -> it);
    }

    @Override
    public CompletableFuture<@Nullable List<String>> getModuleVersions(Project project, String moduleName) {
        BazelModuleBcrCacheService bcrCache = project.getInstance(BazelModuleBcrCacheService.class);
        return bcrCache.getOrFetch(moduleName, () -> fetchModuleVersionsFromRegistry(moduleName)).<@Nullable List<String>>thenApply(it -> it);
    }

    @Override
    public CompletableFuture<?> refreshModuleNames(Project project) {
        BazelModuleBcrCacheService cache = project.getInstance(BazelModuleBcrCacheService.class);
        cache.invalidate(MODULE_NAMES_KEY);
        return getModuleNames(project);
    }

    private final Gson myGson = new Gson();
    private final String myGithubApiUrl = "https://api.github.com/repos/bazelbuild/bazel-central-registry/contents/modules";
    private static final Logger LOG = Logger.getInstance(BazelCentralRegistryModuleResolver.class);
    // consulo: upstream java.net.http.HttpClient (HTTP/2) is replaced by Consulo HttpRequests, see performHttpRequest()

    private record GitHubContent(String name, String type) {
    }

    private record GitHubFileContent(@Nullable String content, @Nullable String encoding) {
    }

    private record ModuleMetadata(@Nullable List<String> versions) {
    }

    private CompletableFuture<@Nullable List<String>> fetchModuleNamesFromRegistry() {
        return performHttpRequest(myGithubApiUrl).handle((json, error) -> {
            if (error != null) {
                Throwable cause = unwrap(error);
                if (cause instanceof IOException e) {
                    LOG.warn(StarlarkLocalize.bzlmodWarningFetchingModuleNames(e).get());
                    return null;
                }
                throw new CompletionException(cause);
            }
            return Arrays.stream(myGson.fromJson(json, GitHubContent[].class))
                // Note: the GitHub endpoint can return various item types (dir, file, symlink, submodule).
                // Modules are directories, so we explicitly filter only type == "dir".
                .filter(it -> "dir".equals(it.type()))
                .map(GitHubContent::name)
                .sorted()
                .toList();
        });
    }

    private CompletableFuture<@Nullable List<String>> fetchModuleVersionsFromRegistry(String moduleName) {
        String url = myGithubApiUrl + "/" + moduleName + "/metadata.json";
        return performHttpRequest(url).handle((json, error) -> {
            if (error != null) {
                Throwable cause = unwrap(error);
                if (cause instanceof IOException e) {
                    LOG.warn(StarlarkLocalize.bzlmodWarningFetchingModuleVersions(moduleName, e).get());
                    return null;
                }
                throw new CompletionException(cause);
            }
            GitHubFileContent file = myGson.fromJson(json, GitHubFileContent.class);
            String decoded;
            String content = file.content();
            if (content != null && "base64".equals(file.encoding())) {
                decoded = new String(Base64.getMimeDecoder().decode(content), StandardCharsets.UTF_8);
            }
            else {
                LOG.warn(StarlarkLocalize.bzlmodWarningEncoding(moduleName).get());
                return null;
            }
            ModuleMetadata metadata = myGson.fromJson(decoded, ModuleMetadata.class);
            List<String> versions = metadata.versions();
            if (versions == null) {
                return null;
            }
            List<String> reversed = new ArrayList<>(versions);
            Collections.reverse(reversed);
            return reversed;
        });
    }

    // consulo: upstream withContext(Dispatchers.IO) + HttpClient.sendAsync(),
    // Consulo HttpRequests is blocking and runs on a pooled thread
    private CompletableFuture<String> performHttpRequest(String url) {
        return CompletableFuture.supplyAsync(() -> {
            Pair<Integer, String> response;
            try {
                response = HttpRequests.request(url)
                    .header("accept", "application/vnd.github.v3+json")
                    .header("User-Agent", Application.get().getName().get())
                    .version(HttpVersion.HTTP_2)
                    // consulo: the status code is checked below, like upstream
                    .allowErrorCodes(true)
                    .connect(request -> {
                        int statusCode = request.statusCode();
                        // consulo: the body is only read for a successful status, it is not used otherwise
                        String body = statusCode >= 200 && statusCode <= 299 ? request.readString(null) : "";
                        return Pair.create(statusCode, body);
                    });
            }
            catch (Exception e) {
                throw new CompletionException(new IOException(StarlarkLocalize.bzlmodExceptionHttpRequest(url, e).get()));
            }

            int statusCode = response.getFirst();
            if (!(statusCode >= 200 && statusCode <= 299)) {
                throw new CompletionException(new IOException(StarlarkLocalize.bzlmodExceptionHttpStatus(statusCode, url).get()));
            }

            return response.getSecond();
        }, Application.get().getInstance(ApplicationConcurrency.class).executor());
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static final String ID = "bcr";
    private static final String MODULE_NAMES_KEY = "___MODULE_NAMES___";
}
