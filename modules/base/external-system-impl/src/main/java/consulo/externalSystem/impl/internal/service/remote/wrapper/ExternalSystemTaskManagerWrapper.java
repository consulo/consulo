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
package consulo.externalSystem.impl.internal.service.remote.wrapper;

import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemTaskManager;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.rt.model.ExternalSystemException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 3/14/13 5:53 PM
 */
public class ExternalSystemTaskManagerWrapper<S extends ExternalSystemExecutionSettings>
  extends AbstractRemoteExternalSystemServiceWrapper<S, RemoteExternalSystemTaskManager<S>>
  implements RemoteExternalSystemTaskManager<S>
{

  @Nonnull
  private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemTaskManagerWrapper(@Nonnull RemoteExternalSystemTaskManager<S> delegate,
                                          @Nonnull RemoteExternalSystemProgressNotificationManager progressManager)
  {
    super(delegate);
    myProgressManager = progressManager;
  }

  @Override
  public void executeTasks(@Nonnull ExternalSystemTaskId id,
                           @Nonnull List<String> taskNames,
                           @Nonnull String projectPath,
                           @Nullable S settings,
                           @Nonnull List<String> vmOptions,
                           @Nonnull List<String> scriptParameters,
                           @jakarta.annotation.Nullable String debuggerSetup) throws RemoteException, ExternalSystemException
  {
    myProgressManager.onQueued(id);
    try {
      getDelegate().executeTasks(id, taskNames, projectPath, settings, vmOptions, scriptParameters, debuggerSetup);
    }
    catch (ExternalSystemException e) {
      myProgressManager.onFailure(id, e);
      throw e;
    }
    catch (Exception e) {
      myProgressManager.onFailure(id, e);
      throw new ExternalSystemException(e);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }

  @Override
  public boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException, ExternalSystemException
  {
    myProgressManager.onQueued(id);
    try {
      return getDelegate().cancelTask(id);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }
}

