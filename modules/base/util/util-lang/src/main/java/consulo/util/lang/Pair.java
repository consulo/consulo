/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.util.lang;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Pair<A extends @Nullable Object, B extends @Nullable Object> implements Map.Entry<A, B> {
  public final A first;
  public final B second;

  public static <A extends @Nullable Object, B extends @Nullable Object> Pair<A, B> create(A first, B second) {
    //noinspection DontUsePairConstructor
    return new Pair<>(first, second);
  }

  public static <A, B> NonNull<A, B> createNonNull(A first, B second) {
    return new NonNull<>(first, second);
  }

  @SuppressWarnings("MethodNamesDifferingOnlyByCase")
  public static <A extends @Nullable Object, B extends @Nullable Object> Pair<A, B> pair(A first, B second) {
    //noinspection DontUsePairConstructor
    return new Pair<>(first, second);
  }

  public static <A, B> Function<A, Pair<A, B>> createFunction(B value) {
    return a -> create(a, value);
  }

  public static <T> @Nullable T getFirst(@Nullable Pair<T, ?> pair) {
    return pair != null ? pair.first : null;
  }

  public static <T> @Nullable T getSecond(@Nullable Pair<?, T> pair) {
    return pair != null ? pair.second : null;
  }

  @SuppressWarnings({"NullAway", "unchecked"})
  private static final Pair EMPTY = create(null, null);

  @SuppressWarnings("unchecked")
  public static <A extends @Nullable Object, B extends @Nullable Object> Pair<A, B> empty() {
    return (Pair) EMPTY;
  }

  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public final A getFirst() {
    return first;
  }

  public final B getSecond() {
    return second;
  }

  @Override
  public A getKey() {
    return getFirst();
  }

  @Override
  public B getValue() {
    return getSecond();
  }

  @Override
  public B setValue(B value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean equals(Object o) {
    return o instanceof Pair && Comparing.equal(first, ((Pair)o).first) && Comparing.equal(second, ((Pair)o).second);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hashCode(first) + Objects.hashCode(second);
  }

  @Override
  public String toString() {
    return "<" + first + "," + second + ">";
  }

  public static class NonNull<A, B> extends Pair<A, B> {
    public NonNull(A first, B second) {
      super(first, second);
    }
  }

  /**
   * @param <A> first value type (Comparable)
   * @param <B> second value type
   * @return a comparator that compares pair values by first value
   */
  public static <A extends Comparable<? super A>, B> Comparator<Pair<A, B>> comparingByFirst() {
    return (o1, o2) -> Comparing.compare(o1.first, o2.first);
  }

  /**
   * @param <A> first value type
   * @param <B> second value type (Comparable)
   * @return a comparator that compares pair values by second value
   */
  public static <A, B extends Comparable<? super B>> Comparator<Pair<A, B>> comparingBySecond() {
    return (o1, o2) -> Comparing.compare(o1.second, o2.second);
  }
}
