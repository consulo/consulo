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
package com.intellij.vfs;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.concurrency.QueueProcessor;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

/**
 * from kotlin
 */
@Singleton
public class AsyncVfsEventsPostProcessorImpl implements AsyncVfsEventsPostProcessor, Disposable {
  private static class ListenerAndDisposable {
    private AsyncVfsEventsListener myListener;
    private Disposable myDisposable;

    private ListenerAndDisposable(AsyncVfsEventsListener listener, Disposable disposable) {
      myListener = listener;
      myDisposable = disposable;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ListenerAndDisposable that = (ListenerAndDisposable)o;
      return Objects.equals(myListener, that.myListener) && Objects.equals(myDisposable, that.myDisposable);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myListener, myDisposable);
    }
  }

  private static final Logger LOG = Logger.getInstance(AsyncVfsEventsPostProcessorImpl.class);

  private QueueProcessor<List<? extends VFileEvent>> myQueue = new QueueProcessor<>(this::processEvents);

  private List<ListenerAndDisposable> myListeners = ContainerUtil.<ListenerAndDisposable>createConcurrentList();

  @Inject
  public AsyncVfsEventsPostProcessorImpl(Application application) {
    application.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        myQueue.add(events);
      }
    });
  }

  private void processEvents(List<? extends VFileEvent> events) {
    for (ListenerAndDisposable listenerAndDisposable : myListeners) {
      try {
        Disposable parentDisposable = listenerAndDisposable.myDisposable;
        AsyncVfsEventsListener listener = listenerAndDisposable.myListener;

        BackgroundTaskUtil.runUnderDisposeAwareIndicator(parentDisposable, () -> listener.filesChanged(events));
      }
      catch (ProcessCanceledException e) {
        // move to the next task
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void addListener(@Nonnull AsyncVfsEventsListener listener, @Nonnull Disposable disposable) {
    ListenerAndDisposable element = new ListenerAndDisposable(listener, disposable);
    Disposer.register(disposable, () -> myListeners.remove(element));
    myListeners.add(element);
  }

  @Override
  public void dispose() {
    myQueue.clear();
    myListeners.clear();
  }
}
