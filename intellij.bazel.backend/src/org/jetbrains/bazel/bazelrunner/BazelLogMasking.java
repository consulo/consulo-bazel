package org.jetbrains.bazel.bazelrunner;

import consulo.util.io.URLUtil;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Masks secrets in the environment and in the arguments of a Bazel command, so that a log does not show them.
 * <p>
 * A variable with a sensitive name, such as {@code GITHUB_TOKEN}, gets its whole value masked.
 * In all other values, the user info of a URL is masked, such as {@code user:password} in {@code https://user:password@host}.
 */
public final class BazelLogMasking {
    private BazelLogMasking() {
    }

    private static final String MASK = "******";

    private static final Pattern COMMON_SENSITIVE_PATTERNS = Pattern.compile(
        "SECRET|PASSWORD|PASSWD|PASSPHRASE|TOKEN|CREDENTIAL|API_?KEY|_KEY$|_AUTH$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Proxy variables. Their value is one URL, which can have no scheme, such as {@code user:password@host:port}.
     */
    private static final Set<String> KNOWN_SENSITIVE_NAMES = Set.of("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY");

    /**
     * RFC 3986, section 3.1
     */
    private static final String SCHEME = "[a-zA-Z][a-zA-Z0-9+.-]*";

    /**
     * RFC 3986, section 3.2
     */
    private static final String USER_INFO = "[^\\s/?#\\[\\]@]+";

    /**
     * Finds the user info of each URL in a text, such as {@code user:password} in {@code https://user:password@host}.
     */
    private static final Pattern URL_USER_INFO = Pattern.compile("(" + SCHEME + "://)" + USER_INFO + "@");

    /**
     * The flags that set a variable as {@code NAME=value}.
     */
    private static final Set<String> ENVIRONMENT_FLAGS = Set.of("--action_env", "--host_action_env", "--test_env", "--repo_env");

    /**
     * The flags that Bazel tags {@code fully_redacted_in_logs}: the option "may contain sensitive credentials and should be redacted in logs".
     * Their whole value is masked.
     * <p>
     * See the flags and the option metadata tags in https://bazel.build/reference/command-line-reference.
     */
    private static final Set<String> FULLY_REDACTED_FLAGS = Set.of(
        "--bes_header",
        "--remote_header",
        "--remote_cache_header",
        "--remote_exec_header",
        "--remote_downloader_header",
        "--google_credentials",
        "--tls_client_key"
    );

    /**
     * Masks the value of a variable. Returns the value as it is when {@link BazelFeatureFlags#getMaskSecretsInLog()} is off.
     */
    public static String maskEnvValue(String name, String value) {
        if (BazelFeatureFlags.getMaskSecretsInLog()) {
            return maskVariableValue(name, value);
        }
        else {
            return value;
        }
    }

    /**
     * Masks the arguments of a command. Returns the arguments as they are when {@link BazelFeatureFlags#getMaskSecretsInLog()} is off.
     * <p>
     * A flag from {@link #ENVIRONMENT_FLAGS} or {@link #FULLY_REDACTED_FLAGS} can have its value in the same argument, as {@code --flag=value},
     * or in the next argument, as {@code --flag value}.
     */
    public static List<String> maskArguments(List<String> arguments) {
        if (BazelFeatureFlags.getMaskSecretsInLog()) {
            return maskEachArgument(arguments);
        }
        else {
            return arguments;
        }
    }

    private static String maskVariableValue(String name, String value) {
        if (COMMON_SENSITIVE_PATTERNS.matcher(name).find()) {
            return MASK;
        }
        else if (KNOWN_SENSITIVE_NAMES.contains(name.toUpperCase(Locale.ROOT)) && !URLUtil.containsScheme(value)) {
            return withTemporaryScheme(value, BazelLogMasking::maskUrlUserInfo);
        }
        else {
            return maskUrlUserInfo(value);
        }
    }

    private static List<String> maskEachArgument(List<String> arguments) {
        // The empty first item is the previous argument of the first argument. It is not a flag.
        List<String> result = new ArrayList<>(arguments.size());
        String previous = "";
        for (String argument : arguments) {
            if (FULLY_REDACTED_FLAGS.contains(previous)) {
                result.add(MASK);
            }
            else if (ENVIRONMENT_FLAGS.contains(previous)) {
                result.add(maskVariable(argument));
            }
            else {
                result.add(maskArgument(argument));
            }
            previous = argument;
        }
        return result;
    }

    /**
     * Masks one argument, which is either {@code --flag=value} or any other text.
     */
    private static String maskArgument(String argument) {
        Map.@Nullable Entry<String, String> split = splitAtFirstEquals(argument);
        if (split == null) {
            return maskUrlUserInfo(argument);
        }
        String flag = split.getKey();
        String value = split.getValue();
        if (FULLY_REDACTED_FLAGS.contains(flag)) {
            return flag + "=" + MASK;
        }
        else if (ENVIRONMENT_FLAGS.contains(flag)) {
            return flag + "=" + maskVariable(value);
        }
        else {
            return maskUrlUserInfo(argument);
        }
    }

    /**
     * Masks the {@code NAME=value} of an environment flag. A value without {@code =}, such as an inherited variable {@code NAME}, has no secret.
     */
    private static String maskVariable(String variable) {
        Map.@Nullable Entry<String, String> split = splitAtFirstEquals(variable);
        if (split == null) {
            return variable;
        }
        String name = split.getKey();
        String value = split.getValue();
        return name + "=" + maskVariableValue(name, value);
    }

    /**
     * Splits {@code name=value} at the first {@code =}, or returns {@code null} when there is no {@code =}.
     */
    private static Map.@Nullable Entry<String, String> splitAtFirstEquals(String receiver) {
        int index = receiver.indexOf('=');
        if (index < 0) {
            return null;
        }
        else {
            return Map.entry(receiver.substring(0, index), receiver.substring(index + 1));
        }
    }

    private static String maskUrlUserInfo(String receiver) {
        return URL_USER_INFO.matcher(receiver).replaceAll("$1" + MASK + "@");
    }

    private static String withTemporaryScheme(String receiver, UnaryOperator<String> transform) {
        String transformed = transform.apply(URLUtil.addSchemaIfMissing(receiver));
        String prefix = URLUtil.HTTPS_PROTOCOL + URLUtil.SCHEME_SEPARATOR;
        return transformed.startsWith(prefix) ? transformed.substring(prefix.length()) : transformed;
    }
}
