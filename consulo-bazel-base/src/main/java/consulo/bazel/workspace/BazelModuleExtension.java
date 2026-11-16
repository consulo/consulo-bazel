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

import consulo.annotation.access.RequiredReadAction;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * The Bazel data of a Bazel-owned module: the classic counterpart of the upstream custom workspace entities whose parent is
 * a {@code ModuleEntity} ({@code BazelModuleExtensionEntity}, {@code JvmBinaryJarsEntity}, {@code ScalaAddendumEntity}).
 * The sync enables it and fills it through {@link BazelModelBuilder#putModuleData}; it is saved with the module and restored
 * when the project opens, as the IntelliJ workspace model restores its entities from its cache.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelModuleExtension extends ModuleExtensionBase<BazelModuleExtension> {
    BazelDataXml.DataSet myData = BazelDataXml.DataSet.EMPTY;

    public BazelModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    public <T> @Nullable T getData(Class<T> type) {
        return type.cast(myData.values().get(type));
    }

    /**
     * False when the stored data was written by another format or plugin version, or misses a value; the project then
     * resyncs on startup.
     */
    public boolean isComplete() {
        return myData.complete();
    }

    @RequiredReadAction
    @Override
    public void commit(BazelModuleExtension mutableModuleExtension) {
        super.commit(mutableModuleExtension);
        myData = mutableModuleExtension.myData;
    }

    @Override
    protected void getStateImpl(Element element) {
        boolean complete = BazelDataXml.writeData(myData, element);
        BazelDataXml.writeHeader(element, complete);
    }

    @RequiredReadAction
    @Override
    protected void loadStateImpl(Element element) {
        if (!BazelDataXml.isCurrentVersion(element)) {
            // the data of another format or plugin version is dropped, the project resyncs
            myData = BazelDataXml.DataSet.INCOMPLETE;
            return;
        }
        BazelDataXml.DataSet data = BazelDataXml.readData(element);
        myData = new BazelDataXml.DataSet(data.values(), data.unknown(), data.complete() && BazelDataXml.isMarkedComplete(element));
    }
}
