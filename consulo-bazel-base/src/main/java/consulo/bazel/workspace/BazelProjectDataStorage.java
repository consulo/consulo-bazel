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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Saves the project-level and library-level Bazel data (the upstream custom workspace entities without a module parent,
 * e.g. {@code BazelProjectDirectoriesEntity}, {@code GoPackageEntities}, {@code BazelLibraryExtensionEntity}) in
 * {@code .consulo/bazelProjectData.xml} and restores it when the project opens. Upstream keeps them in the IntelliJ
 * workspace model, which is cached on disk. Module-level data is saved by {@link BazelModuleExtension}.
 * <p>
 * Library data is kept here by library name: the Bazel libraries are named project libraries owned by the sync, and
 * Consulo library properties would need a custom library kind.
 * <p>
 * The state is restored when the service is created, i.e. before any reader (exclude policies, indexable set
 * contributors, the startup sync check) gets the data, so a restore needs no roots change.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
@State(name = "BazelProjectData", storages = @Storage(value = "bazelProjectData.xml", roamingType = RoamingType.DISABLED))
public class BazelProjectDataStorage implements PersistentStateComponent<Element> {
    private static final String PROJECT = "project";
    private static final String LIBRARIES = "libraries";
    private static final String LIBRARY = "library";
    private static final String NAME = "name";

    public static BazelProjectDataStorage getInstance(Project project) {
        return project.getInstance(BazelProjectDataStorage.class);
    }

    private volatile BazelDataXml.DataSet myProjectData = BazelDataXml.DataSet.EMPTY;
    private volatile Map<String, BazelDataXml.DataSet> myLibraryData = Map.of();
    private volatile boolean myLoadedFromCache;
    // the serialized state, dropped on every change: the data can be large and the project is saved often
    private @Nullable Element myCachedState;

    public <T> @Nullable T getProjectData(Class<T> type) {
        return type.cast(myProjectData.values().get(type));
    }

    public <T> @Nullable T getLibraryData(String libraryName, Class<T> type) {
        BazelDataXml.DataSet data = myLibraryData.get(libraryName);
        return data == null ? null : type.cast(data.values().get(type));
    }

    /**
     * Whether a complete state of the current format and plugin version was restored from disk (upstream
     * {@code WorkspaceModelImpl.loadedFromCache}).
     */
    public boolean isLoadedFromCache() {
        return myLoadedFromCache;
    }

    synchronized void update(Map<Class<?>, Object> projectData, Map<String, Map<Class<?>, Object>> libraryData) {
        myProjectData = new BazelDataXml.DataSet(projectData, List.of(), true);
        Map<String, BazelDataXml.DataSet> libraries = new HashMap<>();
        libraryData.forEach((name, data) -> libraries.put(name, new BazelDataXml.DataSet(data, List.of(), true)));
        myLibraryData = Map.copyOf(libraries);
        myCachedState = null;
    }

    synchronized <T> void putProjectData(Class<T> type, T data) {
        Map<Class<?>, Object> values = new HashMap<>(myProjectData.values());
        values.put(type, data);
        myProjectData = new BazelDataXml.DataSet(values, myProjectData.unknown(), myProjectData.complete());
        myCachedState = null;
    }

    @Override
    public synchronized @Nullable Element getState() {
        BazelDataXml.DataSet projectData = myProjectData;
        Map<String, BazelDataXml.DataSet> libraryData = myLibraryData;
        if (projectData.values().isEmpty() && projectData.unknown().isEmpty() && libraryData.isEmpty()) {
            // never synced (or the restored state was dropped): nothing to save
            return null;
        }

        Element cached = myCachedState;
        if (cached == null) {
            cached = new Element("state");
            Element project = new Element(PROJECT);
            boolean complete = BazelDataXml.writeData(projectData, project);
            cached.addContent(project);

            Element libraries = new Element(LIBRARIES);
            for (Map.Entry<String, BazelDataXml.DataSet> entry : new TreeMap<>(libraryData).entrySet()) {
                Element library = new Element(LIBRARY);
                library.setAttribute(NAME, entry.getKey());
                complete &= BazelDataXml.writeData(entry.getValue(), library);
                libraries.addContent(library);
            }
            cached.addContent(libraries);

            BazelDataXml.writeHeader(cached, complete);
            myCachedState = cached;
        }
        // the storage owns the returned element
        return cached.clone();
    }

    @Override
    public synchronized void loadState(Element state) {
        myCachedState = null;
        if (!BazelDataXml.isCurrentVersion(state)) {
            // another format or plugin version: discard it, the startup activity runs a full sync
            myProjectData = BazelDataXml.DataSet.EMPTY;
            myLibraryData = Map.of();
            myLoadedFromCache = false;
            return;
        }

        boolean complete = BazelDataXml.isMarkedComplete(state);

        Element project = state.getChild(PROJECT);
        BazelDataXml.DataSet projectData = project == null ? BazelDataXml.DataSet.EMPTY : BazelDataXml.readData(project);
        complete &= projectData.complete();

        Map<String, BazelDataXml.DataSet> libraries = new HashMap<>();
        Element librariesElement = state.getChild(LIBRARIES);
        if (librariesElement != null) {
            for (Element library : librariesElement.getChildren(LIBRARY)) {
                String name = library.getAttributeValue(NAME);
                if (name == null) {
                    complete = false;
                    continue;
                }
                BazelDataXml.DataSet data = BazelDataXml.readData(library);
                complete &= data.complete();
                libraries.put(name, data);
            }
        }

        myProjectData = projectData;
        myLibraryData = Map.copyOf(libraries);
        myLoadedFromCache = complete;
    }
}
