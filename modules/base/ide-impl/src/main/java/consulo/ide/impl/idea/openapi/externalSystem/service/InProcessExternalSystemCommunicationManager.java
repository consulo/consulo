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
package consulo.ide.impl.idea.openapi.externalSystem.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.ide.impl.idea.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.wrapper.ExternalSystemFacadeWrapper;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 8/9/13 4:00 PM
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class InProcessExternalSystemCommunicationManager implements ExternalSystemCommunicationManager {
  @Nonnull
  private final ExternalSystemProgressNotificationManagerImpl myProgressManager;

  @Inject
  public InProcessExternalSystemCommunicationManager(@Nonnull ExternalSystemProgressNotificationManager notificationManager) {
    myProgressManager = (ExternalSystemProgressNotificationManagerImpl)notificationManager;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public RemoteExternalSystemFacade acquire(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId) throws Exception {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    InProcessExternalSystemFacadeImpl result = new InProcessExternalSystemFacadeImpl(manager.getProjectResolverFactory(),
                                                                                     manager.getTaskManagerFactory());
    result.applyProgressManager(myProgressManager);
    return result;
  }

  @Override
  public void release(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId) throws Exception {
  }

  @Override
  public boolean isAlive(@Nonnull RemoteExternalSystemFacade facade) {
    RemoteExternalSystemFacade toCheck = facade;
    if (facade instanceof ExternalSystemFacadeWrapper) {
      toCheck = ((ExternalSystemFacadeWrapper)facade).getDelegate();
    }
    return toCheck instanceof InProcessExternalSystemFacadeImpl;
  }

  @Override
  public void clear() {
  }
}
