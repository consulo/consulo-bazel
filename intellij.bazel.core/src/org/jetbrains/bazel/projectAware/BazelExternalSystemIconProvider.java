package org.jetbrains.bazel.projectAware;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.ui.ExternalSystemIconProvider;
import consulo.ui.image.Image;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.config.BazelPluginConstants;

@ExtensionImpl
public class BazelExternalSystemIconProvider implements ExternalSystemIconProvider {
    // consulo: upstream <externalIconProvider key="bazel">
    @Override
    public ProjectSystemId getSystemId() {
        return BazelPluginConstants.SYSTEM_ID;
    }

    @Override
    public Image getReloadIcon() {
        return BazelPluginIcons.bazelReload();
    }
}
