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
package consulo.stream;

import consulo.annotations.ReviewAfterMigrationToJRE;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * port java 9 code. Use Stream#methodName after migration to java 9+
 */
@ReviewAfterMigrationToJRE(9)
public interface StreamJava9 {
  /**
   * Returns a sequential ordered {@code Stream} produced by iterative
   * application of the given {@code next} function to an initial element,
   * conditioned on satisfying the given {@code hasNext} predicate.  The
   * stream terminates as soon as the {@code hasNext} predicate returns false.
   *
   * <p>{@code Stream.iterate} should produce the same sequence of elements as
   * produced by the corresponding for-loop:
   * <pre>{@code
   *     for (T index=seed; hasNext.test(index); index = next.apply(index)) {
   *         ...
   *     }
   * }</pre>
   *
   * <p>The resulting sequence may be empty if the {@code hasNext} predicate
   * does not hold on the seed value.  Otherwise the first element will be the
   * supplied {@code seed} value, the next element (if present) will be the
   * result of applying the {@code next} function to the {@code seed} value,
   * and so on iteratively until the {@code hasNext} predicate indicates that
   * the stream should terminate.
   *
   * <p>The action of applying the {@code hasNext} predicate to an element
   * <a href="../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
   * the action of applying the {@code next} function to that element.  The
   * action of applying the {@code next} function for one element
   * <i>happens-before</i> the action of applying the {@code hasNext}
   * predicate for subsequent elements.  For any given element an action may
   * be performed in whatever thread the library chooses.
   *
   * @param <T>     the type of stream elements
   * @param seed    the initial element
   * @param hasNext a predicate to apply to elements to determine when the
   *                stream must terminate.
   * @param next    a function to be applied to the previous element to produce
   *                a new element
   * @return a new sequential {@code Stream}
   * @since 9
   */
  public static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
    Objects.requireNonNull(next);
    Objects.requireNonNull(hasNext);
    Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE) {
      T prev;
      boolean started
              ,
              finished;

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (finished) return false;
        T t;
        if (started) {
          t = next.apply(prev);
        }
        else {
          t = seed;
          started = true;
        }
        if (!hasNext.test(t)) {
          prev = null;
          finished = true;
          return false;
        }
        action.accept(prev = t);
        return true;
      }

      @Override
      public void forEachRemaining(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (finished) return;
        finished = true;
        T t = started ? next.apply(prev) : seed;
        prev = null;
        while (hasNext.test(t)) {
          action.accept(t);
          t = next.apply(t);
        }
      }
    };
    return StreamSupport.stream(spliterator, false);
  }
}
