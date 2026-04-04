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
package consulo.component.util;

import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public interface BusyObject {
  AsyncResult<Void> getReady(Object requestor);

  abstract class Impl implements BusyObject {
    private final Map<Object, AsyncResult<Void>> myReadyCallbacks = new WeakHashMap<>();

    public abstract boolean isReady();

    public final void onReady() {
      onReady(null);
    }

    public final void onReady(@Nullable Object readyRequestor) {
      if (!isReady()) return;

      if (readyRequestor != null) {
        ReadyCallbacks callbacks = getReadyCallbacks(readyRequestor);
        callbacks.done().setDone();
        for (AsyncResult<Void> rejected : callbacks.rejected()) {
          rejected.setRejected();
        }
      }
      else {
        AsyncResult<Void>[] callbacks = getReadyCallbacks();
        for (AsyncResult<Void> each : callbacks) {
          each.setDone();
        }
      }

      onReadyWasSent();
    }

    protected void onReadyWasSent() {
    }

    @Override
    
    public final AsyncResult<Void> getReady(Object requestor) {
      if (isReady()) {
        return AsyncResult.done(null);
      }
      else {
        return addReadyCallback(requestor);
      }
    }

    private AsyncResult<Void> addReadyCallback(Object requestor) {
      synchronized (myReadyCallbacks) {
        AsyncResult<Void> cb = myReadyCallbacks.get(requestor);
        if (cb == null) {
          cb = new AsyncResult<>();
          myReadyCallbacks.put(requestor, cb);
        }

        return cb;
      }
    }

    private AsyncResult<Void>[] getReadyCallbacks() {
      synchronized (myReadyCallbacks) {
        AsyncResult<Void>[] result = myReadyCallbacks.values().toArray(new AsyncResult[myReadyCallbacks.size()]);
        myReadyCallbacks.clear();
        return result;
      }
    }

    private ReadyCallbacks getReadyCallbacks(Object readyRequestor) {
      synchronized (myReadyCallbacks) {
        AsyncResult<Void> done = myReadyCallbacks.get(readyRequestor);
        if (done == null) {
          done = new AsyncResult<>();
        }

        myReadyCallbacks.remove(readyRequestor);
        List<AsyncResult<Void>> rejected = new ArrayList<>(myReadyCallbacks.values());
        myReadyCallbacks.clear();
        return new ReadyCallbacks(done, rejected);
      }
    }

    private record ReadyCallbacks(AsyncResult<Void> done, List<AsyncResult<Void>> rejected) {
    }

    public static class Simple extends Impl {
      private final AtomicInteger myBusyCount = new AtomicInteger();

      @Override
      public boolean isReady() {
        return myBusyCount.get() == 0;
      }

      public ActionCallback execute(ActiveRunnable runnable) {
        myBusyCount.addAndGet(1);
        ActionCallback cb = runnable.run();
        cb.doWhenProcessed(() -> {
          myBusyCount.addAndGet(-1);
          if (isReady()) {
            onReady();
          }
        });
        return cb;
      }
    }
  }
}
