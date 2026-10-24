package org.jetbrains.bazel.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jspecify.annotations.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelTestLocationHintProvider {
    boolean isApplicable(TargetKind targetKind);

    @Nullable String getLocationHint(BazelTestDetails testDetails);

    static ExtensionPoint<BazelTestLocationHintProvider> getEp() {
        return Application.get().getExtensionPoint(BazelTestLocationHintProvider.class);
    }

    static @Nullable String getLocationHint(BazelTestDetails testDetails, @Nullable TargetKind targetKind) {
        @Nullable String locationHint = getTestLocationHintByFileLocation(testDetails);
        if (locationHint != null) {
            return locationHint;
        }
        if (targetKind == null) {
            return null;
        }
        return getEp().computeSafeIfAny(provider ->
            provider.isApplicable(targetKind) ? provider.getLocationHint(testDetails) : null
        );
    }

    private static @Nullable String getTestLocationHintByFileLocation(BazelTestDetails testDetails) {
        if (testDetails.oneBasedLine() != null) {
            @Nullable String file = testDetails.file();
            if (file == null) {
                return null;
            }
            String forwardSlashFilePath = file.replace('\\', '/');
            return "file://" + forwardSlashFilePath + ":" + testDetails.oneBasedLine();
        }
        else {
            return null;
        }
    }
}
