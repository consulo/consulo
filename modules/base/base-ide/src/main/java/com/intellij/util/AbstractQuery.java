// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.concurrency.AsyncFuture;
import com.intellij.concurrency.AsyncUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadActionProcessor;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author peter
 */
public abstract class AbstractQuery<Result> implements Query<Result> {
  private final ThreadLocal<Boolean> myIsProcessing = new ThreadLocal<>();

  // some clients rely on the (accidental) order of found result
  // to discourage them, randomize the results sometimes to induce errors caused by the order reliance
  private static final boolean RANDOMIZE = ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isInternal();
  private static final Comparator<Object> CRAZY_ORDER = (o1, o2) -> -Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));

  @Override
  @Nonnull
  public Collection<Result> findAll() {
    assertNotProcessing();
    List<Result> result = new ArrayList<>();
    Processor<Result> processor = Processors.cancelableCollectProcessor(result);
    forEach(processor);
    if (RANDOMIZE && result.size() > 1) {
      result.sort(CRAZY_ORDER);
    }
    return result;
  }

  @Nonnull
  @Override
  public Iterator<Result> iterator() {
    assertNotProcessing();
    return new UnmodifiableIterator<>(findAll().iterator());
  }

  @Override
  @Nullable
  public Result findFirst() {
    assertNotProcessing();
    final CommonProcessors.FindFirstProcessor<Result> processor = new CommonProcessors.FindFirstProcessor<>();
    forEach(processor);
    return processor.getFoundValue();
  }

  private void assertNotProcessing() {
    assert myIsProcessing.get() == null : "Operation is not allowed while query is being processed";
  }

  @Nonnull
  @Override
  public Result[] toArray(@Nonnull Result[] a) {
    assertNotProcessing();

    final Collection<Result> all = findAll();
    return all.toArray(a);
  }

  @Nonnull
  @Override
  public Query<Result> allowParallelProcessing() {
    return new AbstractQuery<Result>() {
      @Override
      protected boolean processResults(@Nonnull Processor<? super Result> consumer) {
        return AbstractQuery.this.doProcessResults(consumer);
      }
    };
  }

  @Nonnull
  private Processor<Result> threadSafeProcessor(@Nonnull Processor<? super Result> consumer) {
    Object lock = ObjectUtil.sentinel("AbstractQuery lock");
    return e -> {
      synchronized (lock) {
        return consumer.process(e);
      }
    };
  }

  @Override
  public boolean forEach(@Nonnull Processor<? super Result> consumer) {
    return doProcessResults(threadSafeProcessor(consumer));
  }

  private boolean doProcessResults(@Nonnull Processor<? super Result> consumer) {
    assertNotProcessing();

    myIsProcessing.set(true);
    try {
      return processResults(consumer);
    }
    finally {
      myIsProcessing.remove();
    }
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<? super Result> consumer) {
    return AsyncUtil.wrapBoolean(forEach(consumer));
  }

  /**
   * Assumes consumer being capable of processing results in parallel
   */
  protected abstract boolean processResults(@Nonnull Processor<? super Result> consumer);

  /**
   * Should be called only from {@link #processResults} implementations to delegate to another query
   */
  protected static <T> boolean delegateProcessResults(@Nonnull Query<T> query, @Nonnull Processor<? super T> consumer) {
    if (query instanceof AbstractQuery) {
      return ((AbstractQuery<T>)query).doProcessResults(consumer);
    }
    return query.forEach(consumer);
  }

  @Nonnull
  public static <T> Query<T> wrapInReadAction(@Nonnull final Query<? extends T> query) {
    return new AbstractQuery<T>() {
      @Override
      protected boolean processResults(@Nonnull Processor<? super T> consumer) {
        return AbstractQuery.delegateProcessResults(query, ReadActionProcessor.wrapInReadAction(consumer));
      }
    };
  }
}
