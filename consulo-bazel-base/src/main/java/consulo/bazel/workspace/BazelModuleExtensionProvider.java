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
package consulo.bazel.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import org.jetbrains.bazel.assets.BazelPluginIcons;

/**
 * Registers {@link BazelModuleExtension}. It is system only: the sync enables it, the user cannot toggle it in the module
 * settings (like the external system module extensions).
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class BazelModuleExtensionProvider implements ModuleExtensionProvider<BazelModuleExtension> {
    public static final String ID = "bazel";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Override
    public LocalizeValue getName() {
        return BazelPluginLocalize.moduleExtensionBazelName();
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    @Override
    public ModuleExtension<BazelModuleExtension> createImmutableExtension(ModuleRootLayer layer) {
        return new BazelModuleExtension(getId(), layer);
    }

    @Override
    public MutableModuleExtension<BazelModuleExtension> createMutableExtension(ModuleRootLayer layer) {
        return new BazelMutableModuleExtension(getId(), layer);
    }
}
