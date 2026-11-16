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
import consulo.csharp.lang.psi.icon.CSharpPsiIconGroup;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Registers {@link BazelCSharpModuleExtension}, a child of {@link BazelDotNetCoreModuleExtensionProvider#ID}. System only: the
 * Bazel sync enables and fills it.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class BazelCSharpModuleExtensionProvider implements ModuleExtensionProvider<BazelCSharpModuleExtension> {
    public static final String ID = "bazel-csharp";

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
        return BazelDotNetLocalize.moduleExtensionCsharpName();
    }

    @Override
    public Image getIcon() {
        return CSharpPsiIconGroup.csharp();
    }

    @Override
    public ModuleExtension<BazelCSharpModuleExtension> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelCSharpModuleExtension(getId(), moduleRootLayer);
    }

    @Override
    public MutableModuleExtension<BazelCSharpModuleExtension> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new BazelCSharpMutableModuleExtension(getId(), moduleRootLayer);
    }
}
