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
package consulo.ide.impl.idea.openapi.vfs;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.openapi.vfs.impl.VirtualFileManagerImpl;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ManagingFS;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.FileNameCache;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
@ServiceImpl
public class PlatformVirtualFileManager extends VirtualFileManagerImpl {
  @Nonnull
  private final ManagingFS myManagingFS;

  @Inject
  public PlatformVirtualFileManager(@Nonnull Application application, @Nonnull ManagingFS managingFS) {
    super(application);
    myManagingFS = managingFS;
  }

  @Override
  protected long doRefresh(boolean asynchronous, @Nullable Runnable postAction) {
    if (!asynchronous) {
      ApplicationManager.getApplication().assertIsDispatchThread();
    }

    // todo: get an idea how to deliver changes from local FS to jar fs before they go refresh
    RefreshSession session = RefreshQueue.getInstance().createSession(asynchronous, true, postAction);
    session.addAllFiles(myManagingFS.getRoots());
    session.launch();

    super.doRefresh(asynchronous, postAction);

    return session.getId();
  }

  @Override
  public long getModificationCount() {
    return myManagingFS.getModificationCount();
  }

  @Override
  public long getStructureModificationCount() {
    return myManagingFS.getStructureModificationCount();
  }

  @Override
  public VirtualFile findFileById(int id) {
    return myManagingFS.findFileById(id);
  }

  @Nonnull
  @Override
  public CharSequence getVFileName(int nameId) {
    return FileNameCache.getVFileName(nameId);
  }

  @Override
  public int storeName(@Nonnull String name) {
    return FileNameCache.storeName(name);
  }
}
