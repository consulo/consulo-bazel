package org.jetbrains.bazel.settings.bazel;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

// consulo: upstream data class of BazelApplicationSettings.kt; a mutable bean for the XML serializer
public class BazelApplicationSettings {
    private UpdateChannel myUpdateChannel;
    private boolean myEnablePhasedSync;
    private boolean myForwardProxySettings;

    public BazelApplicationSettings(UpdateChannel updateChannel, boolean enablePhasedSync, boolean forwardProxySettings) {
        myUpdateChannel = updateChannel;
        myEnablePhasedSync = enablePhasedSync;
        myForwardProxySettings = forwardProxySettings;
    }

    // consulo: upstream default arguments (updateChannel = RELEASE, enablePhasedSync = false, forwardProxySettings = false),
    // also used by the serializer
    public BazelApplicationSettings() {
        this(UpdateChannel.RELEASE, false, false);
    }

    public UpdateChannel getUpdateChannel() {
        return myUpdateChannel;
    }

    public void setUpdateChannel(UpdateChannel updateChannel) {
        myUpdateChannel = updateChannel;
    }

    public boolean getEnablePhasedSync() {
        return myEnablePhasedSync;
    }

    public void setEnablePhasedSync(boolean enablePhasedSync) {
        myEnablePhasedSync = enablePhasedSync;
    }

    public boolean getForwardProxySettings() {
        return myForwardProxySettings;
    }

    public void setForwardProxySettings(boolean forwardProxySettings) {
        myForwardProxySettings = forwardProxySettings;
    }

    // consulo: Kotlin data class copy()
    public BazelApplicationSettings copy() {
        return new BazelApplicationSettings(myUpdateChannel, myEnablePhasedSync, myForwardProxySettings);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BazelApplicationSettings that)) {
            return false;
        }
        return myEnablePhasedSync == that.myEnablePhasedSync
            && myForwardProxySettings == that.myForwardProxySettings
            && myUpdateChannel == that.myUpdateChannel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myUpdateChannel, myEnablePhasedSync, myForwardProxySettings);
    }

    @Override
    public String toString() {
        return "BazelApplicationSettings(updateChannel=" + myUpdateChannel + ", enablePhasedSync=" + myEnablePhasedSync
            + ", forwardProxySettings=" + myForwardProxySettings + ")";
    }
}
