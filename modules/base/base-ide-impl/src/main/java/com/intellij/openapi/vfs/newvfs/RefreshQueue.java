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
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author max
 */
public abstract class RefreshQueue {
  public static RefreshQueue getInstance() {
    return ServiceManager.getService(RefreshQueue.class);
  }

  @Nonnull
  public final RefreshSession createSession(boolean async, boolean recursive, @Nullable Runnable finishRunnable) {
    return createSession(async, recursive, finishRunnable, ModalityState.defaultModalityState());
  }

  @Nonnull
  public abstract RefreshSession createSession(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull ModalityState state);

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull VirtualFile... files) {
    refresh(async, recursive, finishRunnable, ModalityState.defaultModalityState(), files);
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull Collection<? extends VirtualFile> files) {
    refresh(async, recursive, finishRunnable, ModalityState.defaultModalityState(), files);
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull ModalityState state, @Nonnull VirtualFile... files) {
    RefreshSession session = createSession(async, recursive, finishRunnable, state);
    session.addAllFiles(files);
    session.launch();
  }

  public final void refresh(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull ModalityState state, @Nonnull Collection<? extends VirtualFile> files) {
    RefreshSession session = createSession(async, recursive, finishRunnable, state);
    session.addAllFiles(files);
    session.launch();
  }

  public abstract void processSingleEvent(@Nonnull VFileEvent event);

  public abstract void cancelSession(long id);

  public abstract boolean isRefreshInProgress();
}
