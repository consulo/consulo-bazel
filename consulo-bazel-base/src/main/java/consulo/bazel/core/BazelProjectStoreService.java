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
package consulo.bazel.core;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Replaces IntelliJ's project store descriptor ({@code ProjectStorePathCustomizer} / {@code ProjectStoreDescriptor}): upstream
 * recognizes a Bazel project by its {@link BazelProjectStoreDescriptor}. Consulo keeps the project files in {@code .consulo},
 * so the descriptor is persisted in the project configuration when the project is imported as a Bazel project.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
@State(name = "BazelProjectStore", storages = @Storage("bazel"))
public class BazelProjectStoreService implements PersistentStateComponent<BazelProjectStoreService.State> {
    public static class State {
        public @Nullable String projectIdentityFile;
        public @Nullable String dotIdea;
        public @Nullable String historicalProjectBasePath;
        public @Nullable String projectViewFile;
    }

    private State myState = new State();
    private volatile @Nullable BazelProjectStoreDescriptor myDescriptor;

    public static BazelProjectStoreService getInstance(Project project) {
        return project.getInstance(BazelProjectStoreService.class);
    }

    public static @Nullable BazelProjectStoreDescriptor getStoreDescriptor(Project project) {
        if (project.isDefault()) {
            return null;
        }
        return getInstance(project).getDescriptor();
    }

    public @Nullable BazelProjectStoreDescriptor getDescriptor() {
        return myDescriptor;
    }

    public void setDescriptor(@Nullable BazelProjectStoreDescriptor descriptor) {
        State state = new State();
        if (descriptor != null) {
            state.projectIdentityFile = descriptor.getProjectIdentityFile().toString();
            state.dotIdea = descriptor.getDotIdea().toString();
            state.historicalProjectBasePath = descriptor.getHistoricalProjectBasePath().toString();
            state.projectViewFile = descriptor.getProjectViewFile().toString();
        }
        myState = state;
        myDescriptor = descriptor;
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
        String projectIdentityFile = state.projectIdentityFile;
        String dotIdea = state.dotIdea;
        String historicalProjectBasePath = state.historicalProjectBasePath;
        String projectViewFile = state.projectViewFile;
        if (projectIdentityFile == null || dotIdea == null || historicalProjectBasePath == null || projectViewFile == null) {
            myDescriptor = null;
            return;
        }
        myDescriptor = new BazelProjectStoreDescriptor(
            Path.of(projectIdentityFile),
            Path.of(dotIdea),
            Path.of(historicalProjectBasePath),
            Path.of(projectViewFile)
        );
    }
}
