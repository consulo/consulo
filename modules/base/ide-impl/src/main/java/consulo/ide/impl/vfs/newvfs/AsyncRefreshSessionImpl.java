/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.vfs.newvfs;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-09-11
 */
public class AsyncRefreshSessionImpl extends RefreshSession {
  private final boolean myAsync;

  public AsyncRefreshSessionImpl(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull ModalityState context) {
    myAsync = async;
  }

  public AsyncRefreshSessionImpl(@Nonnull List<? extends VFileEvent> events) {
    this(false, false, null, IdeaModalityState.defaultModalityState());
    //myEvents.addAll(events);
  }

  @Override
  public boolean isAsynchronous() {
    return myAsync;
  }

  @Override
  public void addFile(@Nonnull VirtualFile file) {

  }

  @Override
  public void addAllFiles(@Nonnull Collection<? extends VirtualFile> files) {

  }

  @Override
  public void launch() {

  }
}
