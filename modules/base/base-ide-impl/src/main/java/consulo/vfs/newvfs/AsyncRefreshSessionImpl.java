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
package consulo.vfs.newvfs;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    this(false, false, null, ModalityState.defaultModalityState());
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
