package org.jetbrains.bazel.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;

public final class Format {
    private Format() {
    }

    public static String duration(Duration duration) {
        if (duration.toSeconds() == 0L) {
            return duration.toMillis() + "ms";
        }
        if (duration.toMinutes() == 0L) {
            DecimalFormat df = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));
            return df.format(((double) duration.toMillis()) / 1000) + "s";
        }
        String minutes = duration.toMinutes() + "m";

        if (duration.toSecondsPart() > 0 || duration.toMillisPart() > 0) {
            double millisAsSeconds = ((double) duration.toMillisPart()) / 1000;
            double seconds = millisAsSeconds + duration.toSecondsPart();
            return minutes + " " + (Math.round(seconds) + "s");
        }

        return minutes;
    }
}
