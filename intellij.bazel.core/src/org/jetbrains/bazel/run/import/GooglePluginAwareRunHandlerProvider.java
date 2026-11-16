// consulo: `import` is a Java keyword, the package gets a trailing underscore (the file keeps the upstream path)
package org.jetbrains.bazel.run.import_;

import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jspecify.annotations.Nullable;

public interface GooglePluginAwareRunHandlerProvider extends RunHandlerProvider {
    /**
     * Respective handler name in the Google Bazel plugin that should be used when importing run configurations.
     */
    String getGoogleHandlerId();

    static @Nullable RunHandlerProvider getRunHandlerProvider(String googleHandlerId, String bazelCommand) {
        boolean isTestCommand = isTestCommand(bazelCommand);
        for (RunHandlerProvider it : RunHandlerProvider.getEp()) {
            if (it instanceof GooglePluginAwareRunHandlerProvider provider
                && provider.getGoogleHandlerId().equals(googleHandlerId)
                && provider.isTestHandler() == isTestCommand) {
                return it;
            }
        }
        return null;
    }

    static boolean isTestCommand(String bazelCommand) {
        boolean isTestCommand = bazelCommand.equals("test") || bazelCommand.equals("coverage");
        return isTestCommand;
    }
}
