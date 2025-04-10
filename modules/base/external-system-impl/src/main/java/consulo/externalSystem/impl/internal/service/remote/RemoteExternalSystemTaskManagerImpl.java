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

import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.rmi.RemoteException;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Denis Zhdanov
 * @since 4/9/13 7:49 PM
 */
public class RemoteExternalSystemTaskManagerImpl<S extends ExternalSystemExecutionSettings>
        extends AbstractRemoteExternalSystemService<S> implements RemoteExternalSystemTaskManager<S>
{

  @Nonnull
  private final ExternalSystemTaskManager<S> myDelegate;

  public RemoteExternalSystemTaskManagerImpl(@Nonnull ExternalSystemTaskManager<S> delegate) {
    myDelegate = delegate;
  }

  @Override
  public void executeTasks(@Nonnull final ExternalSystemTaskId id,
                           @Nonnull final List<String> taskNames,
                           @Nonnull final String projectPath,
                           @Nullable final S settings,
                           @Nonnull final List<String> vmOptions,
                           @Nonnull final List<String> scriptParameters,
                           @Nullable final String debuggerSetup) throws RemoteException, ExternalSystemException
  {
    execute(id, new Supplier<Object>() {
      @Nullable
      @Override
      public Object get() {
        myDelegate.executeTasks(
                id, taskNames, projectPath, settings, vmOptions, scriptParameters, debuggerSetup, getNotificationListener());
        return null;
      }
    });
  }

  @Override
  public boolean cancelTask(@Nonnull final ExternalSystemTaskId id) throws RemoteException, ExternalSystemException
  {
    return myDelegate.cancelTask(id, getNotificationListener());
  }
}
