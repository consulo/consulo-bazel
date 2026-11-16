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
package consulo.bazel.dotnet.nunit.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.dotnet.module.extension.BazelDotNetCoreModuleExtensionProvider;
import consulo.bazel.dotnet.nunit.localize.BazelDotNetNUnitLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.nunit.icon.NUnitIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Registers {@link BazelNUnitModuleExtension}, a child of {@link BazelDotNetCoreModuleExtensionProvider#ID}. System only: the
 * Bazel sync enables it for targets that reference NUnit.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class BazelNUnitModuleExtensionProvider implements ModuleExtensionProvider<BazelNUnitModuleExtension> {
    public static final String ID = "bazel-nunit";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return BazelDotNetCoreModuleExtensionProvider.ID;
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Override
    public LocalizeValue getName() {
        return BazelDotNetNUnitLocalize.moduleExtensionNunitName();
    }

    @Override
    public Image getIcon() {
        return NUnitIconGroup.nunit();
    }

    @Override
    public ModuleExtension<BazelNUnitModuleExtension> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelNUnitModuleExtension(getId(), moduleRootLayer);
    }

    @Override
    public MutableModuleExtension<BazelNUnitModuleExtension> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelNUnitMutableModuleExtension(getId(), moduleRootLayer);
    }
}
