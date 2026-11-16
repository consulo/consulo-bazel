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
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remembers which modules and libraries were created by Bazel sync (workspace file) and gives access to the data of the
 * upstream custom workspace entities published by the last {@link BazelModelBuilder#commit()}.
 * <p>
 * The data is persisted, as the IntelliJ workspace model caches the upstream entities: module data in the
 * {@link BazelModuleExtension} of the module, project and library data in {@link BazelProjectDataStorage}.
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

    private final Project myProject;
    private final BazelProjectDataStorage myDataStorage;

    private State myState = new State();

    @Inject
    public BazelProjectModelService(Project project, BazelProjectDataStorage dataStorage) {
        myProject = project;
        myDataStorage = dataStorage;
    }

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
        return myDataStorage.getProjectData(type);
    }

    public <T> @Nullable T getModuleData(Module module, Class<T> type) {
        BazelModuleExtension extension = ModuleRootManager.getInstance(module).getExtension(BazelModuleExtensionProvider.ID);
        return extension == null ? null : extension.getData(type);
    }

    public <T> @Nullable T getLibraryData(Library library, Class<T> type) {
        String name = library.getName();
        return name == null ? null : myDataStorage.getLibraryData(name, type);
    }

    /**
     * Whether the Bazel data of the project was restored from disk, complete and written by the current plugin version
     * (upstream: the workspace model was loaded from its cache). False before the first sync, after a failed restore and
     * when a module of the last sync lost its data.
     */
    public boolean isLoadedFromCache() {
        if (!myDataStorage.isLoadedFromCache()) {
            return false;
        }
        ModuleManager moduleManager = ModuleManager.getInstance(myProject);
        for (String name : myState.modules) {
            Module module = moduleManager.findModuleByName(name);
            if (module == null) {
                continue;
            }
            BazelModuleExtension extension = ModuleRootManager.getInstance(module).getExtension(BazelModuleExtensionProvider.ID);
            if (extension != null && !extension.isComplete()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replaces one project-level data value outside a sync (upstream modifies the custom workspace entity with
     * {@code WorkspaceModel.update}).
     * <p>
     * TODO consulo: Consulo has no public API to fire a roots change, so file index contributors and exclude policies
     * reading this data see the new value on the next roots change
     */
    public <T> void putProjectData(Class<T> type, T data) {
        myDataStorage.putProjectData(type, data);
    }

    /**
     * Replaces one data value of a module outside a sync (upstream modifies the module entity with
     * {@code WorkspaceModel.update}): the {@link BazelModuleExtension} of the module is changed with a root model commit,
     * right away in a write action, otherwise in a later write action.
     */
    public <T> void putModuleData(Module module, Class<T> type, T data) {
        Runnable update = () -> {
            if (module.isDisposed()) {
                return;
            }
            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
            BazelMutableModuleExtension extension = model.getExtensionWithoutCheck(BazelModuleExtensionProvider.ID);
            if (extension == null) {
                model.dispose();
                return;
            }
            extension.setEnabled(true);
            extension.putData(type, data);
            model.commit();
        };
        if (Application.get().isWriteAccessAllowed()) {
            update.run();
        }
        else {
            WriteAction.runLater(update);
        }
    }

    void update(
        Collection<String> modules,
        Collection<String> libraries,
        Map<Class<?>, Object> projectData,
        Map<String, Map<Class<?>, Object>> libraryData
    ) {
        State state = new State();
        state.modules = new ArrayList<>(modules);
        state.libraries = new ArrayList<>(libraries);
        myState = state;
        myDataStorage.update(projectData, libraryData);
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
