/*
 * Copyright 2013-2018 consulo.io
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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.vfs.newvfs.RefreshQueueImpl;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2018-05-13
 *
 * Implementation of {@link RefreshQueueImpl} without sync write version
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class AsyncRefreshQueueImpl extends RefreshQueue implements Disposable {
  @Nonnull
  private final Application myApplication;

  @Inject
  public AsyncRefreshQueueImpl(@Nonnull Application application) {
    myApplication = application;
  }

  @Nonnull
  @Override
  public RefreshSession createSession(boolean async, boolean recursively, @Nullable Runnable finishRunnable, @Nonnull ModalityState state) {
    return new AsyncRefreshSessionImpl(async, recursively, finishRunnable, state);
  }

  @Override
  public void processSingleEvent(@Nonnull VFileEvent event) {
    new AsyncRefreshSessionImpl(Collections.singletonList(event)).launch();
  }

  @Override
  public void cancelSession(long id) {

  }

  @Override
  public boolean isRefreshInProgress() {
    return false;
  }

  @Override
  public void dispose() {

  }
}
