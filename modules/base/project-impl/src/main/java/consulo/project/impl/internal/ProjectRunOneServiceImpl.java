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
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.json.JsonService;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectRunOnceExtension;
import consulo.project.ProjectRunOneService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-07-15
 */
@Singleton
@ServiceImpl
@State(name = "ProjectOnceRunService", storages = @Storage(StoragePathMacros.DEFAULT_FILE))
public class ProjectRunOneServiceImpl implements ProjectRunOneService, PersistentStateComponent<ProjectRunOneServiceImpl.State> {
    public record Data(String id, String json) {
    }

    public static class State {
        public List<Data> values = new ArrayList<>();
    }

    private static final Logger LOG = Logger.getInstance(ProjectRunOneService.class);

    private final JsonService myJsonService;

    private State myState = new State();

    private final Project myProject;

    @Inject
    public ProjectRunOneServiceImpl(JsonService jsonService, Project project) {
        myJsonService = jsonService;
        myProject = project;
    }

    @Override
    public <T extends Record> void register(String id, T inputValue) {
        try {
            String jsonText = myJsonService.toJson(inputValue);
            myState.values.add(new Data(id, jsonText));
        }
        catch (Exception e) {
            LOG.error("Failed to deserialize: " + id, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void runDelayed() {
        if (myState.values.isEmpty()) {
            return;
        }

        Map<String, ProjectRunOnceExtension> map =
            myProject.getExtensionPoint(ProjectRunOnceExtension.class).getOrBuildCache(ProjectRunOnceExtension.CACHE_KEY);

        ArrayList<Data> oldData = new ArrayList<>(myState.values);

        myState.values.clear();

        for (Data data : oldData) {
            ProjectRunOnceExtension<Record> extension = map.get(data.id());
            if (extension == null) {
                LOG.warn("Failed to find ProjectOnceRunExtension by id: " + data.id());
                continue;
            }

            try {
                Record record = myJsonService.fromJson(data.json(), extension.getInputClass());

                extension.run(record);
            }
            catch (Exception e) {
                LOG.warn("Failed to deserialize input by id: " + data.id(), e);
            }
        }
    }

    @Override
    public ProjectRunOneServiceImpl.State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }
}
