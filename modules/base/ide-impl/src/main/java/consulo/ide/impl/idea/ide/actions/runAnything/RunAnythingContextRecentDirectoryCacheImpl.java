/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.internal.RunAnythingContextRecentDirectoryCache;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
@Singleton
@State(name = "RunAnythingContextRecentDirectoryCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class RunAnythingContextRecentDirectoryCacheImpl implements
    RunAnythingContextRecentDirectoryCache,
    PersistentStateComponent<RunAnythingContextRecentDirectoryCacheImpl.State> {
    static class State {
        public List<String> paths = new ArrayList<>();
    }

    @Nonnull
    public static RunAnythingContextRecentDirectoryCache getInstance(@Nonnull Project project) {
        return project.getInstance(RunAnythingContextRecentDirectoryCache.class);
    }

    private State myState = new State();

    @Override
    public List<String> getPaths() {
        return myState.paths;
    }

    @Nonnull
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
}
