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

import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.nunit.module.extension.NUnitSimpleModuleExtension;

/**
 * NUnit tests in a C# module of the Bazel sync: {@code NUnitTestFramework} of consulo.nunit recognizes {@code [TestFixture]} and
 * {@code [Test]}. Not a {@code NUnitModuleExtension} (no NUnit bundle): the tests run with {@code bazel test}.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelNUnitModuleExtension extends ModuleExtensionBase<BazelNUnitModuleExtension>
    implements NUnitSimpleModuleExtension<BazelNUnitModuleExtension> {
    public BazelNUnitModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }
}
