/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.task.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@State(name = "TaskProjectConfiguration", storages = @Storage("tasks"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TaskProjectConfiguration implements PersistentStateComponent<TaskProjectConfigurationState> {
    private volatile TaskProjectConfigurationState myState = new TaskProjectConfigurationState();

    @Override
    public TaskProjectConfigurationState getState() {
        return myState;
    }

    @Override
    public void loadState(TaskProjectConfigurationState state) {
        myState = state;
    }
}
