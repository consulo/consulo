/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.remote;

import consulo.externalSystem.impl.internal.service.RemoteExternalSystemService;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.project.ProjectData;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 4/9/13 6:56 PM
 */
public interface RemoteExternalSystemProjectResolver<S extends ExternalSystemExecutionSettings> extends RemoteExternalSystemService<S> {

  RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings> NULL_OBJECT
    = new RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings>() {
    @Nullable
    @Override
    public DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                                    String projectPath,
                                                    boolean isPreviewMode,
                                                    @Nullable ExternalSystemExecutionSettings settings)
      throws ExternalSystemException, IllegalArgumentException, IllegalStateException
    {
      return null;
    }

    @Override
    public void setSettings(ExternalSystemExecutionSettings settings) {
    }

    @Override
    public void setNotificationListener(ExternalSystemTaskNotificationListener notificationListener) {
    }

    @Override
    public boolean isTaskInProgress(ExternalSystemTaskId id) {
      return false;
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId id) {
      return false;
    }


    @Override
    public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
      return Collections.emptyMap();
    }
  };

  @Nullable
  DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                           String projectPath,
                                           boolean isPreviewMode,
                                           @Nullable S settings)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException;
}
