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
package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import com.intellij.util.Producer;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Denis Zhdanov
 * @since 4/9/13 5:30 PM
 */
public abstract class AbstractRemoteExternalSystemService<S extends ExternalSystemExecutionSettings>
  implements RemoteExternalSystemService<S>
{

  private final ConcurrentMap<ExternalSystemTaskType, Set<ExternalSystemTaskId>> myTasksInProgress = new ConcurrentHashMap<>();

  private final AtomicReference<S> mySettings = new AtomicReference<S>();
  
  private final AtomicReference<ExternalSystemTaskNotificationListener> myListener
    = new AtomicReference<ExternalSystemTaskNotificationListener>();

  protected <T> T execute(@Nonnull ExternalSystemTaskId id, @Nonnull Producer<T> task) {
    Set<ExternalSystemTaskId> tasks = myTasksInProgress.get(id.getType());
    if (tasks == null) {
      myTasksInProgress.putIfAbsent(id.getType(), new HashSet<ExternalSystemTaskId>());
      tasks = myTasksInProgress.get(id.getType());
    }
    tasks.add(id);
    try {
      return task.produce();
    }
    finally {
      tasks.remove(id);
    }
  }

  @Override
  public void setSettings(@Nonnull S settings) throws RemoteException {
    mySettings.set(settings);
  }

  @Nullable
  public S getSettings() {
    return mySettings.get();
  }
  
  @Override
  public void setNotificationListener(@Nonnull ExternalSystemTaskNotificationListener listener) throws RemoteException {
    myListener.set(listener);
  }

  @Nonnull
  public ExternalSystemTaskNotificationListener getNotificationListener() {
    return myListener.get();
  }

  @Override
  public boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    Set<ExternalSystemTaskId> tasks = myTasksInProgress.get(id.getType());
    return tasks != null && tasks.contains(id);
  }

  @Nonnull
  @Override
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
    return myTasksInProgress;
  }
}
