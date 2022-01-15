// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.util;

import com.intellij.concurrency.AsyncFuture;
import com.intellij.concurrency.AsyncUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author max
 */
public class CollectionQuery<T> implements Query<T> {
  private final Collection<T> myCollection;

  public CollectionQuery(@Nonnull final Collection<T> collection) {
    myCollection = collection;
  }

  @Override
  @Nonnull
  public Collection<T> findAll() {
    return myCollection;
  }

  @Override
  public T findFirst() {
    final Iterator<T> i = iterator();
    return i.hasNext() ? i.next() : null;
  }

  @Override
  public boolean forEach(@Nonnull final Processor<? super T> consumer) {
    return ContainerUtil.process(myCollection, consumer);
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<? super T> consumer) {
    return AsyncUtil.wrapBoolean(forEach(consumer));
  }

  @Nonnull
  @Override
  public T[] toArray(@Nonnull final T[] a) {
    return findAll().toArray(a);
  }

  @Nonnull
  @Override
  public Iterator<T> iterator() {
    return myCollection.iterator();
  }
}
