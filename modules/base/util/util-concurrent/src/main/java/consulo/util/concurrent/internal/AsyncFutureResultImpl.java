/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.concurrent.internal;

import consulo.util.concurrent.AsyncFutureResult;
import consulo.util.concurrent.ResultConsumer;

import jakarta.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * Author: dmitrylomov
 */
public class AsyncFutureResultImpl<V> implements AsyncFutureResult<V> {
  private CompletableFuture<V> myFuture;

  public AsyncFutureResultImpl() {
    myFuture = CompletableFuture.completedFuture(null);
  }

  @Override
  public void addConsumer(Executor executor, final ResultConsumer<V> consumer) {
    myFuture.thenAcceptBothAsync(myFuture, (v, throwable) -> {
      if (throwable != null) {
        consumer.onFailure((Throwable)throwable);
      }
      else {
        consumer.onSuccess(v);
      }
    }, executor);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return myFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return myFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return myFuture.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return myFuture.get();
  }

  @Override
  public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return myFuture.get(timeout, unit);
  }

  @Override
  public void set(V value) {
    myFuture.obtrudeValue(value);
  }

  @Override
  public void setException(Throwable t) {
    myFuture.obtrudeException(t);
  }
}
