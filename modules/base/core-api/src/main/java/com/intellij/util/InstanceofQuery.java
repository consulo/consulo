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
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InstanceofQuery<T> implements Query<T> {
  private final Class<? extends T>[] myClasses;
  private final Query<?> myDelegate;

  public InstanceofQuery(Query<?> delegate, Class<? extends T>... aClasses) {
    myClasses = aClasses;
    myDelegate = delegate;
  }

  @Override
  @Nonnull
  public Collection<T> findAll() {
    ArrayList<T> result = new ArrayList<T>();
    Collection all = myDelegate.findAll();
    for (Object o : all) {
      for (Class aClass : myClasses) {
        if (aClass.isInstance(o)) {
          result.add((T)o);
        }
      }
    }
    return result;
  }

  @Override
  public T findFirst() {
    final CommonProcessors.FindFirstProcessor<T> processor = new CommonProcessors.FindFirstProcessor<T>();
    forEach(processor);
    return processor.getFoundValue();
  }

  @Override
  public boolean forEach(@Nonnull final Processor<T> consumer) {
    return myDelegate.forEach(new MyProcessor(consumer));
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<T> consumer) {
    return myDelegate.forEachAsync(new MyProcessor(consumer));
  }

  @Nonnull
  @Override
  public T[] toArray(@Nonnull T[] a) {
    final Collection<T> all = findAll();
    return all.toArray(a);
  }

  @Override
  public Iterator<T> iterator() {
    return new UnmodifiableIterator<T>(findAll().iterator());
  }

  private class MyProcessor<T> implements Processor<T> {
    private final Processor<T> myConsumer;

    public MyProcessor(Processor<T> consumer) {
      myConsumer = consumer;
    }

    @Override
    public boolean process(T o) {
      for (Class aClass : myClasses) {
        if (aClass.isInstance(o)) {
          return myConsumer.process(((T)o));
        }
      }
      return true;
    }
  }
}
