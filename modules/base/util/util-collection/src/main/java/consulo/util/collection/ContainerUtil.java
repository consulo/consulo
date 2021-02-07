/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.collection;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Based on IDEA code
 */
public class ContainerUtil {
  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> elements) {
    addAllNotNull(collection, elements.iterator());
  }

  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  @SafeVarargs
  @Nonnull
  public static <T, A extends T, C extends Collection<T>> C addAllNotNull(@Nonnull C collection, @Nonnull A... elements) {
    for (T element : elements) {
      if (element != null) {
        collection.add(element);
      }
    }
    return collection;
  }

  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> elements) {
    while (elements.hasNext()) {
      T o = elements.next();
      if (o != null) {
        collection.add(o);
      }
    }
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> appendix) {
    addAll(collection, appendix.iterator());
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> iterator) {
    while (iterator.hasNext()) {
      T o = iterator.next();
      collection.add(o);
    }
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Enumeration<? extends T> enumeration) {
    while (enumeration.hasMoreElements()) {
      T element = enumeration.nextElement();
      collection.add(element);
    }
  }

  @Nonnull
  public static <T, A extends T, C extends Collection<T>> C addAll(@Nonnull C collection, @Nonnull A... elements) {
    //noinspection ManualArrayToCollectionCopy
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }

  @Nullable
  @Contract(pure = true)
  public static <T, V extends T> V find(@Nonnull Iterable<V> iterable, @Nonnull Predicate<T> condition) {
    return find(iterable.iterator(), condition);
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T find(@Nonnull Iterable<? extends T> iterable, @Nonnull final T equalTo) {
    return find(iterable, object -> equalTo == object || equalTo.equals(object));
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T find(@Nonnull Iterator<? extends T> iterator, @Nonnull final T equalTo) {
    return find(iterator, object -> equalTo == object || equalTo.equals(object));
  }

  @Nullable
  public static <T, V extends T> V find(@Nonnull Iterator<V> iterator, @Nonnull Predicate<T> condition) {
    while (iterator.hasNext()) {
      V value = iterator.next();
      if (condition.test(value)) return value;
    }
    return null;
  }

  @Nonnull
  private static <T, C extends Collection<T>> C copy(@Nonnull C collection, @Nonnull Iterable<? extends T> elements) {
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }

  public static <T> void addIfNotNull(@Nonnull Collection<T> result, @Nullable T element) {
    if (element != null) {
      result.add(element);
    }
  }

  @Contract(value = "null -> true", pure = true)
  public static <T> boolean isEmpty(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  @Contract(value = "null -> true", pure = true)
  public static boolean isEmpty(Map map) {
    return map == null || map.isEmpty();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> newArrayList(@Nonnull final T[] elements, final int start, final int end) {
    if (start < 0 || start > end || end > elements.length) {
      throw new IllegalArgumentException("start:" + start + " end:" + end + " length:" + elements.length);
    }

    return new AbstractList<T>() {
      private final int size = end - start;

      @Override
      public T get(final int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index:" + index + " size:" + size);
        return elements[start + index];
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  /**
   * @return iterator with elements from the original {@param iterator} which are valid according to {@param filter} predicate.
   */
  @Contract(pure = true)
  @Nonnull
  public static <T> Iterator<T> filterIterator(final @Nonnull Iterator<? extends T> iterator, final @Nonnull Predicate<? super T> filter) {
    return new Iterator<T>() {
      T next;
      boolean hasNext;

      {
        findNext();
      }

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      private void findNext() {
        hasNext = false;
        while (iterator.hasNext()) {
          T t = iterator.next();
          if (filter.test(t)) {
            next = t;
            hasNext = true;
            break;
          }
        }
      }

      @Override
      public T next() {
        T result;
        if (hasNext) {
          result = next;
          findNext();
        }
        else {
          throw new NoSuchElementException();
        }
        return result;
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  @Contract(pure = true)
  @Nonnull
  public static <T, U> Iterator<U> mapIterator(final @Nonnull Iterator<? extends T> iterator, final @Nonnull Function<? super T, ? extends U> mapper) {
    return new Iterator<U>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public U next() {
        return mapper.apply(iterator.next());
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }
}
