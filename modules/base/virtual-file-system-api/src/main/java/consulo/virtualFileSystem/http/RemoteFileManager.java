/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.virtualFileSystem.http;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.internal.RootComponentHolder;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.http.event.HttpVirtualFileListener;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RemoteFileManager {
  public static RemoteFileManager getInstance() {
    return RootComponentHolder.getRootComponent().getInstance(RemoteFileManager.class);
  }

  public abstract void addRemoteContentProvider(@Nonnull RemoteContentProvider provider, @Nonnull Disposable parentDisposable);

  public abstract void addRemoteContentProvider(@Nonnull RemoteContentProvider provider);

  public abstract void removeRemoteContentProvider(@Nonnull RemoteContentProvider provider);

  public abstract void addFileListener(@Nonnull HttpVirtualFileListener listener);

  public abstract void addFileListener(@Nonnull HttpVirtualFileListener listener, @Nonnull Disposable parentDisposable);

  public abstract void removeFileListener(@Nonnull HttpVirtualFileListener listener);
}
