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
import consulo.annotation.access.RequiredWriteAction;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classic-model replacement for the IntelliJ workspace model {@code MutableEntityStorage} that upstream sync fills.
 * <p>
 * Sync hooks and workspace importers create modules and libraries through this builder; every module and library
 * created here is owned by Bazel. {@link #commit()} applies everything in one write action and removes the Bazel-owned
 * modules and libraries of the previous sync that were not created again, which is what upstream's
 * {@code replaceBySource(BazelEntitySource)} does. Data of the upstream custom workspace entities (which have no
 * classic counterpart) is collected with {@link #putProjectData} / {@link #putModuleData} and published to
 * {@link BazelProjectModelService} on commit.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class BazelModelBuilder {
    private final Project myProject;
    private final ModifiableModuleModel myModuleModel;
    private final LibraryTable.ModifiableModel myLibraryModel;

    private final Map<Module, ModifiableRootModel> myRootModels = new LinkedHashMap<>();
    private final Map<Library, Library.ModifiableModel> myLibraryModels = new LinkedHashMap<>();
    private final Set<String> myModuleNames = new LinkedHashSet<>();
    private final Set<String> myLibraryNames = new LinkedHashSet<>();

    private final Map<Class<?>, Object> myProjectData = new HashMap<>();
    private final Map<String, Map<Class<?>, Object>> myModuleData = new HashMap<>();
    private final Map<String, Map<Class<?>, Object>> myLibraryData = new HashMap<>();

    private boolean myFinished;

    @RequiredReadAction
    public BazelModelBuilder(Project project) {
        myProject = project;
        myModuleModel = ModuleManager.getInstance(project).getModifiableModel();
        myLibraryModel = ProjectLibraryTable.getInstance(project).getModifiableModel();
    }

    public Project getProject() {
        return myProject;
    }

    public ModifiableModuleModel getModuleModel() {
        return myModuleModel;
    }

    public LibraryTable.ModifiableModel getLibraryModel() {
        return myLibraryModel;
    }

    /**
     * Returns the Bazel-owned module with the given name, creating it if needed. Its root model is cleared on first
     * access through {@link #getRootModel}, so the module is rebuilt from scratch by this sync.
     */
    public Module getOrCreateModule(String name, @Nullable String moduleDirPath) {
        checkNotFinished();
        myModuleNames.add(name);
        Module module = myModuleModel.findModuleByName(name);
        if (module == null) {
            module = myModuleModel.newModule(name, moduleDirPath);
        }
        return module;
    }

    public @Nullable Module findModule(String name) {
        return myModuleNames.contains(name) ? myModuleModel.findModuleByName(name) : null;
    }

    public ModifiableRootModel getRootModel(Module module) {
        checkNotFinished();
        return myRootModels.computeIfAbsent(module, it -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(it).getModifiableModel();
            model.clear();
            return model;
        });
    }

    /**
     * Root models of the modules filled by this sync (the classic counterpart of the module entities in upstream's diff storage).
     */
    public Collection<ModifiableRootModel> getRootModels() {
        return Collections.unmodifiableCollection(myRootModels.values());
    }

    /**
     * Returns the Bazel-owned project library with the given name, creating it if needed. Its roots are cleared on
     * first access through {@link #getLibraryModifiableModel}.
     */
    public Library getOrCreateLibrary(String name) {
        checkNotFinished();
        myLibraryNames.add(name);
        Library library = myLibraryModel.getLibraryByName(name);
        if (library == null) {
            library = myLibraryModel.createLibrary(name);
        }
        return library;
    }

    public Library.ModifiableModel getLibraryModifiableModel(Library library) {
        checkNotFinished();
        return myLibraryModels.computeIfAbsent(library, it -> {
            Library.ModifiableModel model = it.getModifiableModel();
            for (OrderRootType type : OrderRootType.getAllTypes()) {
                for (String url : model.getUrls(type.getId())) {
                    model.removeRoot(url, type.getId());
                }
            }
            return model;
        });
    }

    /**
     * Project-level data of an upstream custom workspace entity (e.g. {@code BazelProjectDirectoriesEntity}).
     */
    public <T> void putProjectData(Class<T> type, T data) {
        checkNotFinished();
        myProjectData.put(type, data);
    }

    public <T> @Nullable T getProjectData(Class<T> type) {
        return type.cast(myProjectData.get(type));
    }

    /**
     * Module-level data of an upstream custom workspace entity whose parent is a {@code ModuleEntity}
     * (e.g. {@code BazelModuleExtensionEntity}, {@code JvmBinaryJarsEntity}).
     */
    public <T> void putModuleData(Module module, Class<T> type, T data) {
        checkNotFinished();
        myModuleData.computeIfAbsent(module.getName(), it -> new HashMap<>()).put(type, data);
    }

    public <T> @Nullable T getModuleData(Module module, Class<T> type) {
        Map<Class<?>, Object> data = myModuleData.get(module.getName());
        return data == null ? null : type.cast(data.get(type));
    }

    /**
     * Library-level data of an upstream custom workspace entity whose parent is a {@code LibraryEntity}
     * (e.g. {@code BazelLibraryExtensionEntity}).
     */
    public <T> void putLibraryData(Library library, Class<T> type, T data) {
        checkNotFinished();
        myLibraryData.computeIfAbsent(libraryName(library), it -> new HashMap<>()).put(type, data);
    }

    public <T> @Nullable T getLibraryData(Library library, Class<T> type) {
        Map<Class<?>, Object> data = myLibraryData.get(libraryName(library));
        return data == null ? null : type.cast(data.get(type));
    }

    static String libraryName(Library library) {
        String name = library.getName();
        if (name == null) {
            throw new IllegalArgumentException("Bazel libraries are named project libraries");
        }
        return name;
    }

    /**
     * Applies the model. Bazel-owned modules and libraries of the previous sync that were not created by this builder
     * are removed.
     */
    @RequiredWriteAction
    public void commit() {
        checkNotFinished();
        myFinished = true;

        BazelProjectModelService modelService = BazelProjectModelService.getInstance(myProject);

        for (String name : modelService.getOwnedModuleNames()) {
            if (!myModuleNames.contains(name)) {
                Module stale = myModuleModel.findModuleByName(name);
                if (stale != null) {
                    myModuleModel.disposeModule(stale);
                }
            }
        }

        for (String name : modelService.getOwnedLibraryNames()) {
            if (!myLibraryNames.contains(name)) {
                Library stale = myLibraryModel.getLibraryByName(name);
                if (stale != null) {
                    myLibraryModel.removeLibrary(stale);
                }
            }
        }

        for (Library.ModifiableModel model : myLibraryModels.values()) {
            model.commit();
        }
        myLibraryModel.commit();

        ModifiableModelCommitter.getInstance(myProject).multiCommit(myRootModels.values().toArray(ModifiableRootModel[]::new), myModuleModel);

        modelService.update(myModuleNames, myLibraryNames, myProjectData, myModuleData, myLibraryData);
    }

    /**
     * Discards all changes (e.g. when the sync failed or was cancelled).
     */
    public void dispose() {
        if (myFinished) {
            return;
        }
        myFinished = true;
        for (Library.ModifiableModel model : myLibraryModels.values()) {
            model.dispose();
        }
        for (ModifiableRootModel model : myRootModels.values()) {
            model.dispose();
        }
        myModuleModel.dispose();
    }

    private void checkNotFinished() {
        if (myFinished) {
            throw new IllegalStateException("BazelModelBuilder is already committed or disposed");
        }
    }
}
