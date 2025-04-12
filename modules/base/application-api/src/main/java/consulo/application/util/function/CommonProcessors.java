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
package consulo.application.util.function;

import consulo.annotation.DeprecationInfo;
import consulo.util.collection.ArrayFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * @author max
 */
public class CommonProcessors {
  public static class CollectProcessor<T> implements Processor<T> {
    private final Collection<T> myCollection;

    public CollectProcessor(Collection<T> collection) {
      myCollection = collection;
    }

    public CollectProcessor() {
      myCollection = new ArrayList<>();
    }

    @Override
    public boolean process(T t) {
      if (accept(t)) {
        myCollection.add(t);
      }
      return true;
    }

    protected boolean accept(T t) {
      return true;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo(value = "Please use #toArray(@NotNull ArrayFactory<T>)")
    public T[] toArray(@Nonnull T[] a) {
      return myCollection.toArray(a);
    }

    @Nonnull
    public T[] toArray(@Nonnull IntFunction<T[]> factory) {
      return ContainerUtil.toArray(myCollection, factory);
    }

    public Collection<T> getResults() {
      return myCollection;
    }

  }

  @Nonnull
  public static <T> Processor<T> notNullProcessor(@Nonnull final Predicate<T> processor) {
    return processor::test;
  }

  public static class CollectUniquesProcessor<T> implements Processor<T> {
    private final Set<T> myCollection;

    public CollectUniquesProcessor() {
      myCollection = new HashSet<>();
    }

    @Override
    public boolean process(T t) {
      myCollection.add(t);
      return true;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo(value = "Please use #toArray(@NotNull ArrayFactory<T>)")
    public T[] toArray(@Nonnull T[] a) {
      return myCollection.toArray(a);
    }

    @Nonnull
    public T[] toArray(@Nonnull ArrayFactory<T> factory) {
      return ContainerUtil.toArray(myCollection, factory);
    }

    public Collection<T> getResults() {
      return myCollection;
    }
  }

  public static class UniqueProcessor<T> implements Processor<T> {
    private final Set<T> processed;
    private final Predicate<T> myDelegate;

    public UniqueProcessor(Predicate<T> delegate) {
      this(delegate, HashingStrategy.canonical());
    }
    public UniqueProcessor(Predicate<T> delegate, HashingStrategy<T> strategy) {
      myDelegate = delegate;
      processed = Sets.newHashSet(strategy);
    }

    @Override
    public boolean process(T t) {
      synchronized (processed) {
        if (!processed.add(t)) {
          return true;
        }
      }
      return myDelegate.test(t);
    }
  }

  public abstract static class FindProcessor<T> implements Processor<T> {
    private T myValue;

    public boolean isFound() {
      return myValue != null;
    }

    @Nullable
    public T getFoundValue() {
      return myValue;
    }

    @Nullable
    public T reset() {
      T prev = myValue;
      myValue = null;
      return prev;
    }

    @Override
    public boolean process(T t) {
      if (accept(t)) {
        myValue = t;
        return false;
      }
      return true;
    }

    protected abstract boolean accept(T t);
  }

  public static class FindFirstAndOnlyProcessor<T> extends FindFirstProcessor<T> {

    @Override
    public boolean process(T t) {
      boolean firstFound = getFoundValue() != null;
      boolean result = super.process(t);
      if (!result) {
        if (firstFound) reset();
        return !firstFound;
      }
      return true;
    }
  }

  public static class FindFirstProcessor<T> extends FindProcessor<T> {
    @Override
    protected boolean accept(T t) {
      return true;
    }
  }

  private static final Processor FALSE = t -> false;
  private static final Processor TRUE = t -> true;

  @SuppressWarnings({"unchecked"})
  public static <T> Processor<T> alwaysFalse() {
    return FALSE;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Processor<T> alwaysTrue() {
    return TRUE;
  }
}
