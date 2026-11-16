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

import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelMutableModuleExtension extends BazelModuleExtension implements MutableModuleExtension<BazelModuleExtension> {
    public BazelMutableModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    /**
     * Replaces all data values of the module (a sync rebuilds the module, upstream {@code replaceBySource}).
     */
    void setData(Map<Class<?>, Object> data) {
        myData = new BazelDataXml.DataSet(data, List.of(), true);
    }

    /**
     * Replaces one data value, keeping the others.
     */
    <T> void putData(Class<T> type, T data) {
        Map<Class<?>, Object> values = new HashMap<>(myData.values());
        values.put(type, data);
        myData = new BazelDataXml.DataSet(values, myData.unknown(), myData.complete());
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createConfigurationComponent(Disposable uiDisposable, Runnable updateOnCheck) {
        // managed by the Bazel sync, nothing to configure
        return null;
    }

    @Override
    public void setEnabled(boolean val) {
        myIsEnabled = val;
    }

    @Override
    public boolean isModified(BazelModuleExtension originalExtension) {
        return myIsEnabled != originalExtension.isEnabled() || !myData.equals(originalExtension.myData);
    }
}
