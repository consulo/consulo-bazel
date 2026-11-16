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

import consulo.content.bundle.Sdk;
import consulo.disposer.Disposable;
import consulo.dotnet.module.extension.DotNetConfigurationPanel;
import consulo.dotnet.module.extension.DotNetMutableModuleExtension;
import consulo.dotnet.psi.impl.module.extension.DotNetQualifiedElementQualifierProducer;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelDotNetCoreMutableModuleExtension extends BazelDotNetCoreModuleExtension
    implements DotNetMutableModuleExtension<BazelDotNetCoreModuleExtension> {
    public BazelDotNetCoreMutableModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
        return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createConfigurationComponent(Disposable uiDisposable, Runnable updateOnCheck) {
        // read only in practice: the Bazel sync writes the values again
        return DotNetConfigurationPanel.create(this, DotNetQualifiedElementQualifierProducer.INSTANCE, getVariables(), uiDisposable, updateOnCheck);
    }

    @Override
    public void setEnabled(boolean val) {
        myIsEnabled = val;
    }

    @Override
    public boolean isModified(BazelDotNetCoreModuleExtension originalExtension) {
        return isModifiedImpl(originalExtension);
    }
}
