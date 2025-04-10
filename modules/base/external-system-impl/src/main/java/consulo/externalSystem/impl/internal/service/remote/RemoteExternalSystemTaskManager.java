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
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.rt.model.ExternalSystemException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 4/9/13 7:02 PM
 */
public interface RemoteExternalSystemTaskManager<S extends ExternalSystemExecutionSettings> extends RemoteExternalSystemService<S> {

  /** <a href="http://en.wikipedia.org/wiki/Null_Object_pattern">Null object</a> for {@link RemoteExternalSystemProjectResolverImpl}. */
  RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings> NULL_OBJECT =
    new RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings>() {

      @Override
      public void executeTasks(@Nonnull ExternalSystemTaskId id,
                               @Nonnull List<String> taskNames,
                               @Nonnull String projectPath,
                               @Nullable ExternalSystemExecutionSettings settings,
                               @Nonnull List<String> vmOptions,
                               @Nonnull List<String> scriptParameters,
                               @Nullable String debuggerSetup) throws RemoteException, ExternalSystemException
      {
      }

      @Override
      public boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException, ExternalSystemException
      {
        return false;
      }

      @Override
      public void setSettings(@Nonnull ExternalSystemExecutionSettings settings) throws RemoteException {
      }

      @Override
      public void setNotificationListener(@Nonnull ExternalSystemTaskNotificationListener notificationListener) throws RemoteException {
      }

      @Override
      public boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException {
        return false;
      }

      @Nonnull
      @Override
      public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
        return Collections.emptyMap();
      }
    };

  void executeTasks(@Nonnull ExternalSystemTaskId id,
                    @Nonnull List<String> taskNames,
                    @Nonnull String projectPath,
                    @Nullable S settings,
                    @Nonnull List<String> vmOptions,
                    @Nonnull List<String> scriptParameters,
                    @Nullable String debuggerSetup) throws RemoteException, ExternalSystemException;

  boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException, ExternalSystemException;
}
