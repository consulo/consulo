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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.util.concurrent.QueueProcessor;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.event.AsyncVfsEventsListener;
import consulo.virtualFileSystem.event.AsyncVfsEventsPostProcessor;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * from kotlin
 */
@Singleton
@ServiceImpl
public class AsyncVfsEventsPostProcessorImpl implements AsyncVfsEventsPostProcessor, Disposable {
  private record ListenerAndDisposable(AsyncVfsEventsListener listener, Disposable disposable) {
  }

  private static final Logger LOG = Logger.getInstance(AsyncVfsEventsPostProcessorImpl.class);

  private QueueProcessor<List<? extends VFileEvent>> myQueue = new QueueProcessor<>(this::processEvents);

  private List<ListenerAndDisposable> myListeners = Lists.newLockFreeCopyOnWriteList();

  @Inject
  public AsyncVfsEventsPostProcessorImpl(Application application) {
    application.getMessageBus().connect().subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        myQueue.add(events);
      }
    });
  }

  private void processEvents(List<? extends VFileEvent> events) {
    for (ListenerAndDisposable listenerAndDisposable : myListeners) {
      try {
        Disposable parentDisposable = listenerAndDisposable.disposable();
        AsyncVfsEventsListener listener = listenerAndDisposable.listener();

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
