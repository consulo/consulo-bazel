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

import consulo.annotation.access.RequiredReadAction;
import consulo.csharp.base.module.extension.BaseCSharpModuleExtension;
import consulo.module.content.layer.ModuleRootLayer;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The C# part of a module of a rules_dotnet C# target ({@code csharp_library}, {@code csharp_binary}, {@code csharp_test},
 * {@code csharp_nunit_test}): language version, nullable context and unsafe code of the target.
 * <p>
 * rules_dotnet passes the {@code internals_visible_to} attribute as a generated source with {@code InternalsVisibleTo}
 * attributes, which is not part of the module, so the assemblies of the attribute are kept here and added to the ones of the
 * sources.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelCSharpModuleExtension extends BaseCSharpModuleExtension<BazelCSharpModuleExtension> {
    protected List<String> myInternalsVisibleTo = new ArrayList<>();

    public BazelCSharpModuleExtension(String id, ModuleRootLayer layer) {
        super(id, layer);
    }

    public List<String> getInternalsVisibleTo() {
        return myInternalsVisibleTo;
    }

    /**
     * rules_dotnet generates no {@code AssemblyTitle} attribute, so without one in the sources the name of the module as an
     * assembly (checked against {@code InternalsVisibleTo}) is the assembly name of the target, the output file name of its
     * .NET extension, not the module name.
     */
    @RequiredReadAction
    @Override
    public @Nullable String getAssemblyTitle() {
        String title = super.getAssemblyTitle();
        if (title != null) {
            return title;
        }
        BazelDotNetCoreModuleExtension dotNetExtension = getModuleRootLayer().getExtension(BazelDotNetCoreModuleExtension.class);
        if (dotNetExtension == null) {
            return null;
        }
        String fileName = dotNetExtension.getFileName();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : null;
    }

    @RequiredReadAction
    @Override
    public Set<String> getInternalsVisibleToAssemblies() {
        if (myInternalsVisibleTo.isEmpty()) {
            return super.getInternalsVisibleToAssemblies();
        }
        Set<String> result = new LinkedHashSet<>(super.getInternalsVisibleToAssemblies());
        result.addAll(myInternalsVisibleTo);
        return result;
    }

    @Override
    public boolean isModifiedImpl(BazelCSharpModuleExtension mutableModuleExtension) {
        return super.isModifiedImpl(mutableModuleExtension) || !myInternalsVisibleTo.equals(mutableModuleExtension.myInternalsVisibleTo);
    }

    @RequiredReadAction
    @Override
    public void commit(BazelCSharpModuleExtension mutableModuleExtension) {
        super.commit(mutableModuleExtension);
        myInternalsVisibleTo = new ArrayList<>(mutableModuleExtension.myInternalsVisibleTo);
    }

    @RequiredReadAction
    @Override
    protected void loadStateImpl(Element element) {
        super.loadStateImpl(element);
        myInternalsVisibleTo.clear();
        for (Element child : element.getChildren("internals-visible-to")) {
            myInternalsVisibleTo.add(child.getText());
        }
    }

    @Override
    protected void getStateImpl(Element element) {
        super.getStateImpl(element);
        for (String assembly : myInternalsVisibleTo) {
            element.addContent(new Element("internals-visible-to").setText(assembly));
        }
    }
}
