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
package consulo.bazel.go.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelDataSerializer;
import consulo.bazel.workspace.BazelRecordXml;
import org.jdom.Element;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.BazelGoTargetEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists the Go project data. {@link GoPackageEntities} is not a record: only its package and target entities (records)
 * are written, the lookup maps are rebuilt by its constructor on read.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class GoPackageEntitiesSerializer implements BazelDataSerializer<GoPackageEntities> {
    private static final String PACKAGE = "package";
    private static final String TARGET = "target";

    @Override
    public String getId() {
        return "goPackageEntities";
    }

    @Override
    public Class<GoPackageEntities> getDataClass() {
        return GoPackageEntities.class;
    }

    @Override
    public void write(GoPackageEntities data, Element element) {
        for (BazelGoPackageEntity it : data.getPackages()) {
            Element packageElement = new Element(PACKAGE);
            BazelRecordXml.writeRecord(it, packageElement);
            element.addContent(packageElement);
        }
        for (BazelGoTargetEntity it : data.getTargets()) {
            Element targetElement = new Element(TARGET);
            BazelRecordXml.writeRecord(it, targetElement);
            element.addContent(targetElement);
        }
    }

    @Override
    public GoPackageEntities read(Element element) {
        List<BazelGoPackageEntity> packages = new ArrayList<>();
        for (Element it : element.getChildren(PACKAGE)) {
            packages.add(BazelRecordXml.readRecord(BazelGoPackageEntity.class, it));
        }
        List<BazelGoTargetEntity> targets = new ArrayList<>();
        for (Element it : element.getChildren(TARGET)) {
            targets.add(BazelRecordXml.readRecord(BazelGoTargetEntity.class, it));
        }
        return new GoPackageEntities(packages, targets);
    }
}
