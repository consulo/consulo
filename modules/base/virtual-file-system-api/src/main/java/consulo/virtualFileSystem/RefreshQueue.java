/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.virtualFileSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.event.VFileEvent;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RefreshQueue {
  public static RefreshQueue getInstance() {
    return Application.get().getInstance(RefreshQueue.class);
  }

  
  public final RefreshSession createSession(boolean async, boolean recursive, @Nullable Runnable finishRunnable) {
    return createSession(async, recursive, finishRunnable, Application.get().getDefaultModalityState());
  }

  
  public abstract RefreshSession createSession(boolean async, boolean recursive, @Nullable Runnable finishRunnable, ModalityState state);

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, VirtualFile... files) {
    refresh(async, recursive, finishRunnable, Application.get().getDefaultModalityState(), files);
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, Collection<? extends VirtualFile> files) {
    refresh(async, recursive, finishRunnable, Application.get().getDefaultModalityState(), files);
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, ModalityState state, VirtualFile... files) {
    RefreshSession session = createSession(async, recursive, finishRunnable, state);
    session.addAllFiles(files);
    session.launch();
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, ModalityState state, Collection<? extends VirtualFile> files) {
    RefreshSession session = createSession(async, recursive, finishRunnable, state);
    session.addAllFiles(files);
    session.launch();
  }

  public abstract void processSingleEvent(VFileEvent event);

  public abstract void cancelSession(long id);

  public abstract boolean isRefreshInProgress();
}
