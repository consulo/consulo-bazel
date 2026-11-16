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
package consulo.bazel.cpp.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelRecordDataSerializer;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity.CcToolchainCompilerInfoEntities;

/**
 * Project data of the C/C++ sync: records only (strings, maps, {@code OutputLocation}s).
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class CcToolchainCompilerInfoEntitiesSerializer extends BazelRecordDataSerializer<CcToolchainCompilerInfoEntities> {
    public CcToolchainCompilerInfoEntitiesSerializer() {
        super("ccToolchainCompilerInfoEntities", CcToolchainCompilerInfoEntities.class);
    }
}
