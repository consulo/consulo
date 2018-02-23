/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.concurrency.AsyncFuture;
import com.intellij.concurrency.AsyncFutureFactory;
import com.intellij.concurrency.AsyncFutureResult;
import com.intellij.concurrency.FinallyFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author peter
 */
public abstract class AbstractQuery<Result> implements Query<Result> {
  private boolean myIsProcessing = false;

  @Override
  @Nonnull
  public Collection<Result> findAll() {
    assertNotProcessing();
    final CommonProcessors.CollectProcessor<Result> processor = new CommonProcessors.CollectProcessor<Result>();
    forEach(processor);
    return processor.getResults();
  }

  @Override
  public Iterator<Result> iterator() {
    assertNotProcessing();
    return new UnmodifiableIterator<Result>(findAll().iterator());
  }

  @Override
  @Nullable
  public Result findFirst() {
    assertNotProcessing();
    final CommonProcessors.FindFirstProcessor<Result> processor = new CommonProcessors.FindFirstProcessor<Result>();
    forEach(processor);
    return processor.getFoundValue();
  }

  private void assertNotProcessing() {
    assert !myIsProcessing : "Operation is not allowed while query is being processed";
  }

  @Nonnull
  @Override
  public Result[] toArray(@Nonnull Result[] a) {
    assertNotProcessing();

    final Collection<Result> all = findAll();
    return all.toArray(a);
  }

  @Override
  public boolean forEach(@Nonnull Processor<Result> consumer) {
    assertNotProcessing();

    myIsProcessing = true;
    try {
      return processResults(consumer);
    }
    finally {
      myIsProcessing = false;
    }
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<Result> consumer) {
    assertNotProcessing();
    myIsProcessing = true;
    return new FinallyFuture<Boolean>(processResultsAsync(consumer), new Runnable() {
      @Override
      public void run() {
        myIsProcessing = false;
      }
    });
  }

  protected abstract boolean processResults(@Nonnull Processor<Result> consumer);

  protected AsyncFuture<Boolean> processResultsAsync(@Nonnull Processor<Result> consumer) {
    final AsyncFutureResult<Boolean> result = AsyncFutureFactory.getInstance().createAsyncFutureResult();
    try {
      result.set(processResults(consumer));
    } catch (Throwable t) {
      result.setException(t);
    }
    return result;
  }
}
