package org.jetbrains.bazel.ui.settings;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

// consulo: upstream data class of BazelApplicationSettingsPanel.kt, own file because it is public (state of
// BazelApplicationSettingsService); a mutable bean for the XML serializer
public class BazelApplicationSettings {
    private UpdateChannel myUpdateChannel;
    private boolean myEnablePhasedSync;

    public BazelApplicationSettings(UpdateChannel updateChannel, boolean enablePhasedSync) {
        myUpdateChannel = updateChannel;
        myEnablePhasedSync = enablePhasedSync;
    }

    // consulo: upstream default arguments (updateChannel = RELEASE, enablePhasedSync = false), also used by the serializer
    public BazelApplicationSettings() {
        this(UpdateChannel.RELEASE, false);
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

    // consulo: Kotlin data class copy()
    public BazelApplicationSettings copy() {
        return new BazelApplicationSettings(myUpdateChannel, myEnablePhasedSync);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BazelApplicationSettings that)) {
            return false;
        }
        return myEnablePhasedSync == that.myEnablePhasedSync && myUpdateChannel == that.myUpdateChannel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myUpdateChannel, myEnablePhasedSync);
    }

    @Override
    public String toString() {
        return "BazelApplicationSettings(updateChannel=" + myUpdateChannel + ", enablePhasedSync=" + myEnablePhasedSync + ")";
    }
}
