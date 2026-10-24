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
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remembers which modules and libraries were created by Bazel sync (persisted) and holds the data of upstream custom
 * workspace entities published by the last {@link BazelModelBuilder#commit()} (in memory).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
@State(name = "BazelProjectModel", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class BazelProjectModelService implements PersistentStateComponent<BazelProjectModelService.State> {
    public static class State {
        public List<String> modules = new ArrayList<>();
        public List<String> libraries = new ArrayList<>();
    }

    public static BazelProjectModelService getInstance(Project project) {
        return project.getInstance(BazelProjectModelService.class);
    }

    private State myState = new State();

    private volatile Map<Class<?>, Object> myProjectData = Map.of();
    private volatile Map<String, Map<Class<?>, Object>> myModuleData = Map.of();
    private volatile Map<String, Map<Class<?>, Object>> myLibraryData = Map.of();

    public List<String> getOwnedModuleNames() {
        return List.copyOf(myState.modules);
    }

    public List<String> getOwnedLibraryNames() {
        return List.copyOf(myState.libraries);
    }

    public boolean isBazelModule(Module module) {
        return myState.modules.contains(module.getName());
    }

    public <T> @Nullable T getProjectData(Class<T> type) {
        return type.cast(myProjectData.get(type));
    }

    public <T> @Nullable T getModuleData(Module module, Class<T> type) {
        Map<Class<?>, Object> data = myModuleData.get(module.getName());
        return data == null ? null : type.cast(data.get(type));
    }

    public <T> @Nullable T getLibraryData(Library library, Class<T> type) {
        String name = library.getName();
        Map<Class<?>, Object> data = name == null ? null : myLibraryData.get(name);
        return data == null ? null : type.cast(data.get(type));
    }

    void update(
        Collection<String> modules,
        Collection<String> libraries,
        Map<Class<?>, Object> projectData,
        Map<String, Map<Class<?>, Object>> moduleData,
        Map<String, Map<Class<?>, Object>> libraryData
    ) {
        State state = new State();
        state.modules = new ArrayList<>(modules);
        state.libraries = new ArrayList<>(libraries);
        myState = state;
        myProjectData = Map.copyOf(projectData);
        Map<String, Map<Class<?>, Object>> copy = new HashMap<>();
        moduleData.forEach((name, data) -> copy.put(name, Map.copyOf(data)));
        myModuleData = Map.copyOf(copy);
        Map<String, Map<Class<?>, Object>> libraryCopy = new HashMap<>();
        libraryData.forEach((name, data) -> libraryCopy.put(name, Map.copyOf(data)));
        myLibraryData = Map.copyOf(libraryCopy);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    // set of names for faster lookups by callers that check many modules
    public Set<String> getOwnedModuleNameSet() {
        return Set.copyOf(myState.modules);
    }
}
