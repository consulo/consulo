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

import consulo.externalSystem.impl.internal.service.RemoteExternalSystemService;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import jakarta.annotation.Nonnull;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 3/14/13 5:55 PM
 */
public abstract class AbstractRemoteExternalSystemServiceWrapper<S extends ExternalSystemExecutionSettings, T extends RemoteExternalSystemService<S>>
  implements RemoteExternalSystemService<S>
{

  @Nonnull
  private final T myDelegate;

  public AbstractRemoteExternalSystemServiceWrapper(@Nonnull T delegate) {
    myDelegate = delegate;
  }

  @Override
  public void setSettings(@Nonnull S settings) throws RemoteException {
    myDelegate.setSettings(settings);
  }

  @Override
  public void setNotificationListener(@Nonnull ExternalSystemTaskNotificationListener notificationListener) throws RemoteException {
    myDelegate.setNotificationListener(notificationListener);
  }

  @Override
  public boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    return myDelegate.isTaskInProgress(id);
  }

  @Override
  @Nonnull
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
    return myDelegate.getTasksInProgress();
  }

  @Nonnull
  public T getDelegate() {
    return myDelegate;
  }
}
