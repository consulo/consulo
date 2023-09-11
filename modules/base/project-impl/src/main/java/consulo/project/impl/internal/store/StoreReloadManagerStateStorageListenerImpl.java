/*
 * Copyright 2013-2023 consulo.io
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
package consulo.project.impl.internal.store;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.component.store.impl.internal.storage.StateStorage;
import consulo.component.store.impl.internal.storage.StateStorageListener;
import consulo.project.Project;
import consulo.project.StoreReloadManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 11/09/2023
 */
@TopicImpl(ComponentScope.PROJECT)
public class StoreReloadManagerStateStorageListenerImpl implements StateStorageListener {
  private final StoreReloadManagerImpl myStoreReloadManager;
  private final Project myProject;

  @Inject
  public StoreReloadManagerStateStorageListenerImpl(StoreReloadManager storeReloadManager, Project project) {
    myProject = project;
    myStoreReloadManager = (StoreReloadManagerImpl)storeReloadManager;
  }

  @Override
  public void storageFileChanged(@Nonnull VirtualFileEvent event, @Nonnull StateStorage storage) {
    myStoreReloadManager.projectStorageFileChanged(event, storage, myProject);
  }
}
