/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.dotnet.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.dotnet.localize.BazelDotNetLocalize;
import consulo.dotnet.icon.DotNetIconGroup;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;

/**
 * Registers {@link BazelDotNetCoreModuleExtension}. System only: the Bazel sync enables and fills it.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class BazelDotNetCoreModuleExtensionProvider implements ModuleExtensionProvider<BazelDotNetCoreModuleExtension> {
    public static final String ID = "bazel-dotnet-core";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return BazelDotNetLocalize.moduleExtensionDotnetCoreName();
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Override
    public Image getIcon() {
        return DotNetIconGroup.netfoundation();
    }

    @Override
    public ModuleExtension<BazelDotNetCoreModuleExtension> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelDotNetCoreModuleExtension(getId(), moduleRootLayer);
    }

    @Override
    public MutableModuleExtension<BazelDotNetCoreModuleExtension> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelDotNetCoreMutableModuleExtension(getId(), moduleRootLayer);
    }
}
