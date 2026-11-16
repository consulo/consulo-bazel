package org.jetbrains.bazel.languages.bazelversion.psi;

import consulo.util.lang.SemVer;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface BazelVersionLiteral {
    record Forked(String fork, BazelVersionLiteral version) implements BazelVersionLiteral {
    }

    record Specific(SemVer version) implements BazelVersionLiteral {
    }

    record Other(String version) implements BazelVersionLiteral {
    }

    record Latest(int offset) implements BazelVersionLiteral {
    }

    enum Special implements BazelVersionLiteral {
        LAST_GREEN,
        LAST_RC,
        ROLLING,
    }

    static @Nullable BazelVersionLiteral fromString(String s) {
        return toBazelVersionLiteral(s);
    }

    static String toBazelVersionStringLiteral(BazelVersionLiteral receiver) {
        return switch (receiver) {
            case Forked forked -> forked.fork() + "/" + toBazelVersionStringLiteral(forked.version());
            case Latest latest -> latest.offset() == 0 ? "latest" : "latest-" + latest.offset();
            case Other other -> other.version();
            case Special.LAST_GREEN -> "last_green";
            case Special.LAST_RC -> "last_rc";
            case Special.ROLLING -> "rolling";
            case Specific specific -> String.valueOf(specific.version());
        };
    }

    static @Nullable SemVer toSemVer(BazelVersionLiteral receiver) {
        return switch (receiver) {
            case Forked forked -> toSemVer(forked.version());
            case Other other -> SemVer.parseFromText(other.version());
            case Specific specific -> specific.version();
            default -> null;
        };
    }

    static BazelVersionLiteral withNewVersionWhenPossible(BazelVersionLiteral receiver, String newVersion) {
        return switch (receiver) {
            case Forked forked -> new Forked(forked.fork(), withNewVersionWhenPossible(forked.version(), newVersion));
            case Other other -> Objects.requireNonNullElse(toBazelVersionLiteral(newVersion), receiver);
            case Specific specific -> Objects.requireNonNullElse(toBazelVersionLiteral(newVersion), receiver);
            default -> receiver;
        };
    }

    // consulo: upstream private top-level vals; interface fields cannot be private
    Pattern BAZEL_FORK_VERSION_REGEX = Pattern.compile("(?:([^/]+)/)?(.+)");
    Pattern BAZEL_VERSION_LATEST_REGEX = Pattern.compile("latest(?:-(\\d+))?");

    private static @Nullable BazelVersionLiteral toBazelVersionLiteral(String receiver) {
        Matcher forkMatch = BAZEL_FORK_VERSION_REGEX.matcher(receiver.trim());
        if (!forkMatch.matches()) {
            return null;
        }
        String fork = Objects.requireNonNullElse(forkMatch.group(1), "");
        String version = Objects.requireNonNullElse(forkMatch.group(2), "");
        if (!fork.isBlank()) {
            BazelVersionLiteral forkVersion = toBazelVersionLiteral(version);
            if (forkVersion == null) {
                return null;
            }
            return new Forked(fork, forkVersion);
        }

        Matcher latestMatch = BAZEL_VERSION_LATEST_REGEX.matcher(version);
        if (latestMatch.matches()) {
            String offset = Objects.requireNonNullElse(latestMatch.group(1), "");
            int offsetValue;
            try {
                offsetValue = Integer.parseInt(offset);
            }
            catch (NumberFormatException e) {
                offsetValue = 0;
            }
            return new Latest(offsetValue);
        }

        switch (version) {
            case "last_green":
                return Special.LAST_GREEN;
            case "last_rc":
                return Special.LAST_RC;
            case "rolling":
                return Special.ROLLING;
            default:
                break;
        }

        SemVer semver =
            SemVer.parseFromText(version);
        if (semver == null) {
            return new Other(version);
        }
        return new Specific(semver);
    }
}
