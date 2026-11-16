package org.jetbrains.bazel.bazelrunner;

import consulo.http.HttpProxyManager;
import consulo.util.io.URLUtil;
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettingsService;
import org.jspecify.annotations.Nullable;

import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

// consulo: IntelliJ ProxyConfiguration/ProxySettings/ProxyAuthentication are replaced by consulo.http.HttpProxyManager,
// Credentials by java.net.PasswordAuthentication
public final class BazelProxyForwarding {
    private BazelProxyForwarding() {
    }

    /**
     * Only static HTTP proxy is supported.
     */
    public static boolean isSupported() {
        return isSupported(HttpProxyManager.getInstance());
    }

    // consulo: upstream default argument configuration = ProxySettings.getInstance().getProxyConfiguration()
    public static boolean isSupported(HttpProxyManager configuration) {
        // consulo: the static proxy is HttpProxyManager.isHttpProxyEnabled(); PAC/auto-detect and direct are not supported
        // TODO consulo: HttpProxyManager does not expose the proxy protocol, so a static SOCKS proxy is not rejected
        return configuration.isHttpProxyEnabled();
    }

    /**
     * Returns the proxy variables for a Bazel process, or an empty map when Bazel cannot use {@code configuration} or if proxy forwarding is disabled.
     */
    public static Map<String, String> environment() {
        HttpProxyManager configuration = HttpProxyManager.getInstance();
        return environment(configuration, (host, port) -> getKnownAuthentication(configuration));
    }

    public static Map<String, String> environment(
        HttpProxyManager configuration,
        BiFunction<String, Integer, @Nullable PasswordAuthentication> credentials
    ) {
        if (!isEnabled() || !isSupported(configuration)) {
            return Map.of();
        }
        else {
            return httpProxyEnvs(configuration, credentials.apply(configuration.getProxyHost(), configuration.getProxyPort()));
        }
    }

    // consulo: replaces ProxyAuthentication.getKnownAuthentication(host, port): the credentials of the proxy settings
    private static @Nullable PasswordAuthentication getKnownAuthentication(HttpProxyManager configuration) {
        if (!configuration.isProxyAuthenticationEnabled()) {
            return null;
        }
        @Nullable String login = configuration.getProxyLogin();
        @Nullable String password = configuration.getPlainProxyPassword();
        if (login == null) {
            return null;
        }
        return new PasswordAuthentication(login, password == null ? new char[0] : password.toCharArray());
    }

    private static Map<String, String> httpProxyEnvs(
        HttpProxyManager configuration,
        @Nullable PasswordAuthentication credentials
    ) {
        @Nullable String userName = credentials == null || credentials.getUserName().isEmpty() ? null : credentials.getUserName();
        @Nullable String password =
            credentials == null || credentials.getPassword().length == 0 ? null : String.valueOf(credentials.getPassword());
        // Bazel rejects a user without a password, and the IDE also treats such credentials as unknown
        String userInfo = userName != null && password != null
            ? URLUtil.encodeURIComponent(userName) + ":" + URLUtil.encodeURIComponent(password) + "@"
            : "";
        String proxyUrl = userInfo + configuration.getProxyHost() + ":" + configuration.getProxyPort();
        // consulo: HttpProxyManager.getProxyExceptions() returns the exceptions already split by ','
        List<String> exceptions = new ArrayList<>();
        for (String it : configuration.getProxyExceptions()) {
            @Nullable String entry = toNoProxyEntry(it.trim());
            if (entry != null) {
                exceptions.add(entry);
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        // Bazel can read both, so to avoid mix-up we put both cases
        putEnvUpperAndLowerCase(result, "HTTP_PROXY", proxyUrl);
        putEnvUpperAndLowerCase(result, "HTTPS_PROXY", proxyUrl);
        // Always set, so that NO_PROXY of the parent environment does not apply to the IDE proxy
        putEnvUpperAndLowerCase(result, "NO_PROXY", String.join(",", exceptions));
        return result;
    }

    private static boolean isEnabled() {
        return BazelApplicationSettingsService
            .getInstance()
            .getSettings()
            .getForwardProxySettings();
    }

    /**
     * Converts an IDE proxy exception to a {@code NO_PROXY} entry, or returns {@code null} when Bazel has no equivalent.
     * <p>
     * Bazel does not support wildcards in {@code NO_PROXY}. An entry with a leading dot matches only the subdomains, as {@code *.} does in the IDE.
     * So {@code *.corp.com} becomes {@code .corp.com}. An entry without the dot, such as {@code corp.com}, also matches the host itself.
     * Bazel cannot express other wildcards, so the function drops them.
     * <p>
     * See {@code ProxyHelper.createProxyIfNeeded} in the Bazel sources:
     * https://github.com/bazelbuild/bazel/blob/9.3.1/src/main/java/com/google/devtools/build/lib/bazel/repository/downloader/ProxyHelper.java
     */
    private static @Nullable String toNoProxyEntry(String receiver) {
        String entry = receiver.startsWith("*.") ? receiver.substring(1) : receiver;
        return !entry.isEmpty() && entry.indexOf('*') < 0 ? entry : null;
    }

    private static void putEnvUpperAndLowerCase(Map<String, String> receiver, String name, String value) {
        receiver.put(name.toUpperCase(Locale.ROOT), value);
        receiver.put(name.toLowerCase(Locale.ROOT), value);
    }
}
