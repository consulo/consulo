/*
 * Copyright 2013-2014 must-be.org
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
package org.consulo.compiler.server.fileSystem;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 11:34/14.08.13
 */
public class CompilerServerRefreshQueueImpl extends RefreshQueue {
  @Override
  public RefreshSession createSession(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @NotNull ModalityState state) {
    return new CompilerServerRefreshSessionImpl();
  }

  @Override
  public void processSingleEvent(@NotNull VFileEvent event) {
  }

  @Override
  public void cancelSession(long id) {
  }
}
