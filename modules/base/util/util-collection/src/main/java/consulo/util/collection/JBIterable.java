/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.util.collection.impl.EmptyIterator;
import consulo.util.collection.impl.SingletonIterator;
import consulo.util.lang.Comparing;
import consulo.util.lang.function.Functions;
import consulo.util.lang.function.Predicates;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * An in-house and immutable version of {@code com.google.common.collect.FluentIterable}
 * with some insights from Clojure. Added bonus is that the JBIterator instances are preserved
 * during most transformations, a feature employed by {@link JBTreeTraverser}.
 * <p>
 * <p/>
 * The original JavaDoc ('FluentIterable' replaced by 'JBIterable'):
 * <p/>
 * {@code JBIterable} provides a rich interface for manipulating {@code Iterable} instances in a
 * chained fashion. A {@code JBIterable} can be created from an {@code Iterable}, or from a set
 * of elements. The following types of methods are provided on {@code JBIterable}:
 * <ul>
 * <li>chained methods which return a new {@code JBIterable} based in some way on the contents
 * of the current one (for example {@link #map})
 * <li>conversion methods which copy the {@code JBIterable}'s contents into a new collection or
 * array (for example {@link #toList})
 * <li>element extraction methods which facilitate the retrieval of certain elements (for example
 * {@link #last})
 * </ul>
 * <p/>
 * <p>Here is an example that merges the lists returned by two separate database calls, transforms
 * it by invoking {@code toString()} on each element, and returns the first 10 elements as a
 * {@code List}: <pre>   {@code
 *   JBIterable
 *       .from(database.getClientList())
 *       .filter(activeInLastMonth())
 *       .map(Functions.toStringFunction())
 *       .toList();}</pre>
 * <p/>
 * <p>Anything which can be done using {@code JBIterable} could be done in a different fashion
 * (often with {@code Iterables}), however the use of {@code JBIterable} makes many sets of
 * operations significantly more concise.
 *
 * @author Marcin Mikosik
 * @noinspection unchecked
 */
public abstract class JBIterable<E> implements Iterable<E> {

  /**
   * a Collection, an Iterable, or a single object
   */
  final Object content;

  /**
   * Constructor for use by subclasses.
   */
  protected JBIterable() {
    content = this;
  }

  JBIterable(@Nonnull Object content) {
    this.content = content;
  }

  /**
   * Lambda-friendly construction method.
   */
  @Nonnull
  public static <E> JBIterable<E> create(@Nullable final Supplier<Iterator<E>> producer) {
    if (producer == null) return empty();
    return new JBIterable<E>() {
      @Nonnull
      @Override
      public Iterator<E> iterator() {
        return producer.get();
      }
    };
  }

  /**
   * Returns a {@code JBIterable} that wraps {@code iterable}, or {@code iterable} itself if it
   * is already a {@code JBIterable}.
   */
  @Nonnull
  public static <E> JBIterable<E> from(@Nullable Iterable<? extends E> iterable) {
    if (iterable == null || iterable == EMPTY) return empty();
    if (iterable instanceof JBIterable) return (JBIterable<E>)iterable;
    if (iterable instanceof Collection && ((Collection)iterable).isEmpty()) return empty();
    return new Multi(iterable);
  }

  private static final class Multi<E> extends JBIterable<E> {
    Multi(Iterable<? extends E> iterable) {
      super(iterable);
    }

    public Iterator<E> iterator() {
      return ((Iterable<E>)content).iterator();
    }
  }

  /**
   * Returns a {@code JBIterable} that is generated by {@code generator} function applied to a previous element,
   * the first element is produced by the supplied {@code first} value.
   * Iteration stops when {@code null} is encountered.
   */
  @Nonnull
  public static <E> JBIterable<E> generate(@Nullable final E first, @Nonnull final Function<? super E, ? extends E> generator) {
    if (first == null) return empty();
    return new JBIterable<E>() {
      @Override
      public Iterator<E> iterator() {
        final Function<? super E, ? extends E> fun = Stateful.copy(generator);
        return new JBIterator<E>() {
          E cur = first;

          @Override
          public E nextImpl() {
            E result = cur;
            if (result == null) return stop();
            cur = fun.apply(cur);
            return result;
          }
        };
      }
    };
  }

  @Nonnull
  public static <E> JBIterable<E> generate(@Nullable final E first1,
                                           @Nullable final E first2,
                                           @Nonnull final BiFunction<? super E, ? super E, ? extends E> generator) {
    if (first1 == null) return empty();
    return new JBIterable<E>() {
      @Override
      public Iterator<E> iterator() {
        return new JBIterator<E>() {
          E cur1 = first1;
          E cur2 = first2;

          @Override
          public E nextImpl() {
            E result = cur1;
            cur1 = cur2;
            cur2 = generator.apply(result, cur2);
            if (result == null) return stop();
            return result;
          }
        };
      }
    };
  }

  /**
   * Returns a {@code JBIterable} containing the one {@code element} if is not null.
   */
  @Nonnull
  public static <E> JBIterable<E> of(@Nullable E element) {
    if (element == null) return empty();
    return new Single(element);
  }

  private static final class Single<E> extends JBIterable<E> {
    Single(@Nonnull Object content) {
      super(content);
    }

    @Override
    public Iterator<E> iterator() {
      return new SingletonIterator(content);
    }
  }

  /**
   * Returns a {@code JBIterable} containing {@code elements} in the specified order.
   */
  @Nonnull
  public static <E> JBIterable<E> of(@Nullable E... elements) {
    return elements == null || elements.length == 0 ? JBIterable.<E>empty() : from(Arrays.asList(elements));
  }

  private static final JBIterable EMPTY = new Empty();

  private static final class Empty extends JBIterable {
    @Override
    public Iterator iterator() {
      return EmptyIterator.getInstance();
    }
  }

  @Nonnull
  public static <E> JBIterable<E> empty() {
    return (JBIterable<E>)EMPTY;
  }

  @Nonnull
  public static <E> JBIterable<E> once(@Nonnull Iterator<E> iterator) {
    return of(SimpleReference.create(iterator)).intercept(iterator1 -> {
      SimpleReference<Iterator<E>> ref = iterator1.next();
      Iterator<E> result = ref.get();
      if (result == null) throw new UnsupportedOperationException();
      ref.set(null);
      return result;
    });
  }

  /**
   * Returns iterator, useful for graph traversal.
   *
   * @see TreeTraversal.TracingIt
   */
  @Nonnull
  public <T extends Iterator<E>> T typedIterator() {
    return (T)iterator();
  }

  public final boolean processEach(@Nonnull Predicate<E> processor) {
    return ContainerUtil.process(this, processor);
  }

  public final void consumeEach(@Nonnull Consumer<E> consumer) {
    for (E e : this) {
      consumer.accept(e);
    }
  }

  /**
   * Returns a string representation of this iterable for debugging purposes.
   */
  @Nonnull
  @Override
  public String toString() {
    return content == this ? JBIterable.class.getSimpleName() : String.valueOf(content);
  }

  /**
   * Returns the number of elements in this iterable.
   */
  public final int size() {
    Collection<E> col = asCollection();
    if (col != null) return col.size();
    Iterable<E> itt = asIterable();
    if (itt == null) return 1;
    int count = 0;
    for (E ignored : itt) {
      count++;
    }
    return count;
  }

  /**
   * Returns {@code true} if this iterable contains any object for which
   * {@code equals(element)} is true.
   */
  public final boolean contains(@Nullable Object element) {
    Collection col = asCollection();
    if (col != null) return col.contains(element);
    Iterable<E> itt = asIterable();
    if (itt == null) return Comparing.equal(content, element);
    for (E e : itt) {
      if (Comparing.equal(e, element)) return true;
    }
    return false;
  }

  /**
   * Returns element at index if it is present; otherwise {@code null}
   */
  @Nullable
  public final E get(int index) {
    List<E> list = asRandomAccess();
    if (list != null) {
      return index >= list.size() ? null : list.get(index);
    }
    Iterable<E> itt = asIterable();
    if (itt == null) {
      return index == 0 ? (E)content : null;
    }
    return skip(index).first();
  }

  @Nullable
  private List<E> asRandomAccess() {
    //noinspection CastConflictsWithInstanceof
    return content instanceof RandomAccess ? (List<E>)content : null;
  }

  @Nullable
  private Collection<E> asCollection() {
    //noinspection CastConflictsWithInstanceof
    return content instanceof Collection ? (Collection<E>)content : null;
  }

  @Nullable
  private Iterable<E> asIterable() {
    //noinspection CastConflictsWithInstanceof
    return content instanceof Iterable ? (Iterable<E>)content : null;
  }

  @Nullable
  private E asElement() {
    //noinspection unchecked
    return this instanceof Single ? (E)content : null;
  }

  @Nonnull
  public final JBIterable<E> repeat(int count) {
    Function<JBIterable<E>, JBIterable<E>> fun = Functions.identity();
    return generate(this, fun).take(count).flatten(fun);
  }

  /**
   * Returns a {@code JBIterable} which iterators traverse first the elements of this iterable,
   * followed by those of {@code other}. The iterators are not polled until necessary.
   */
  @Nonnull
  public final JBIterable<E> append(@Nullable Iterable<? extends E> other) {
    if (other == null || other == EMPTY) return this;
    if (this == EMPTY) return from(other);
    Appended parent = this instanceof Appended ? (Appended)this : new Appended<E>(this, null);
    // to keep append lazy, ignore the fact that 'other' can also be an Appended
    return new Appended<E>(other, parent);
  }

  private static final class Appended<E> extends JBIterable<E> {

    final Iterable<? extends E> iterable;
    final Appended<E> parent;

    Appended(@Nonnull Iterable<? extends E> iterable, @Nullable Appended<E> parent) {
      this.iterable = iterable;
      this.parent = parent;
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
      return new FlattenFun.FlattenIt<E, E>(
        Arrays.<Iterable<E>>asList(getIterables()).iterator(),
        Functions.<E, Iterable<E>>identity());
    }

    @Nonnull
    Iterable[] getIterables() {
      int size = 0;
      for (Appended p = this; p != null; p = p.parent) size++;
      Iterable[] iterables = new Iterable[size];
      int i = 0;
      for (Appended p = this; p != null; p = p.parent) iterables[size - (++i)] = p.iterable;
      return iterables;
    }
  }

  @Nonnull
  public final <T> JBIterable<E> append(@Nullable Iterable<T> other, @Nonnull Function<? super T, ? extends Iterable<? extends E>> fun) {
    return other == null ? this :
      this == EMPTY ? from(other).flatten(fun) :
        append(from(other).flatten(fun));
  }

  /**
   * Returns a {@code JBIterable} which iterators traverse first the elements of this iterable,
   * followed by the {@code elements}.
   */
  @Nonnull
  public final JBIterable<E> append(@Nonnull E[] elements) {
    return this == EMPTY ? of(elements) : append(of(elements));
  }

  /**
   * Returns a {@code JBIterable} which iterators traverse first the elements of this iterable,
   * followed by {@code element} if it is not null.
   */
  @Nonnull
  public final JBIterable<E> append(@Nullable E element) {
    return element == null ? this : this == EMPTY ? of(element) : append(of(element));
  }

  /**
   * Returns the elements from this iterable that satisfy a condition.
   */
  @Nonnull
  public final JBIterable<E> filter(@Nonnull final Predicate<? super E> condition) {
    return intercept(iterator -> JBIterator.from(iterator).filter(Stateful.copy(condition)));
  }

  /**
   * Returns the elements from this iterable that are instances of class {@code type}.
   *
   * @param type the type of elements desired
   */
  @Nonnull
  public final <T> JBIterable<T> filter(@Nonnull Class<T> type) {
    return (JBIterable<T>)filter(Predicates.instanceOf(type));
  }

  @Nonnull
  public final JBIterable<E> filterNotNull() {
    return filter(Objects::nonNull);
  }

  @Nonnull
  public final JBIterable<E> take(final int count) {
    return intercept(iterator -> JBIterator.from(iterator).take(count));
  }

  @Nonnull
  public final JBIterable<E> takeWhile(@Nonnull final Predicate<? super E> condition) {
    return intercept(iterator -> JBIterator.from(iterator).takeWhile(Stateful.copy(condition)));
  }

  @Nonnull
  public final JBIterable<E> skip(final int count) {
    return intercept(iterator -> JBIterator.from(iterator).skip(count));
  }

  @Nonnull
  public final JBIterable<E> skipWhile(@Nonnull final Predicate<? super E> condition) {
    return intercept(iterator -> JBIterator.from(iterator).skipWhile(Stateful.copy(condition)));
  }

  /**
   * Returns a {@code JBIterable} that applies {@code function} to each element of this iterable.
   */
  @Nonnull
  public final <T> JBIterable<T> map(@Nonnull final Function<? super E, T> function) {
    return intercept(iterator -> JBIterator.from(iterator).map(Stateful.copy(function)));
  }

  /**
   * A synonym for {@link JBIterable#map(Function)}.
   * Note: {@code map} is shorter and shall be preferred.
   *
   * @see JBIterable#map(Function)
   */
  @Nonnull
  public final <T> JBIterable<T> transform(@Nonnull Function<? super E, T> function) {
    return map(function);
  }


  /**
   * Returns a {@code JBIterable} that applies {@code function} to each element of this
   * iterable and concatenates the produced iterables in one.
   * Nulls are supported and silently skipped.
   */
  @Nonnull
  public <T> JBIterable<T> flatten(@Nonnull final Function<? super E, ? extends Iterable<? extends T>> function) {
    return intercept(new FlattenFun<E, T>(function));
  }

  private static final class FlattenFun<E, T> implements Function<Iterator<E>, Iterator<T>> {
    final Function<? super E, ? extends Iterable<? extends T>> function;

    FlattenFun(Function<? super E, ? extends Iterable<? extends T>> function) {
      this.function = function;
    }

    @Override
    public Iterator<T> apply(final Iterator<E> iterator) {
      return new FlattenIt<E, T>(iterator, Stateful.copy(function));
    }

    static final class FlattenIt<E, T> extends JBIterator<T> {
      final Iterator<E> original;
      final Function<? super E, ? extends Iterable<? extends T>> function;
      Iterator<? extends T> cur;

      public FlattenIt(Iterator<E> iterator, Function<? super E, ? extends Iterable<? extends T>> fun) {
        original = iterator;
        function = fun;
      }

      @Override
      public T nextImpl() {
        if (cur != null && cur.hasNext()) return cur.next();
        if (!original.hasNext()) return stop();
        Iterable<? extends T> next = function.apply(original.next());
        cur = next == null ? null : next.iterator();
        return skip();
      }
    }
  }

  /**
   * Filters out duplicate items.
   */
  @Nonnull
  public final JBIterable<E> unique() {
    return unique(Function.identity());
  }

  /**
   * Filters out duplicate items, where an element identity is provided by the specified function.
   */
  @Nonnull
  public final JBIterable<E> unique(@Nonnull final Function<? super E, ?> identity) {
    return filter(new SCond<E>() {
      HashSet<Object> visited;

      @Override
      public boolean test(E e) {
        if (visited == null) visited = new HashSet<Object>();
        return visited.add(identity.apply(e));
      }
    });
  }

  /**
   * The most generic iterator transformation.
   */
  @Nonnull
  public final <T, X extends Iterator<E>> JBIterable<T> intercept(@Nonnull final Function<X, ? extends Iterator<T>> function) {
    if (this == EMPTY) return empty();
    if (this instanceof Intercepted) {
      return new Intercepted<E, T, X>(
        ((Intercepted)this).original,
        Functions.compose(((Intercepted)this).interceptor, function));
    }
    return new Intercepted<E, T, X>(this, function);
  }

  private static final class Intercepted<E, T, X> extends JBIterable<T> {
    final JBIterable<E> original;
    private final Function<X, ? extends Iterator<T>> interceptor;

    public Intercepted(@Nonnull JBIterable<E> original, Function<X, ? extends Iterator<T>> interceptor) {
      this.original = original;
      this.interceptor = interceptor;
    }

    @Override
    public Iterator<T> iterator() {
      return interceptor.apply((X)original.iterator());
    }
  }

  /**
   * Returns the first element in this iterable or null.
   */
  @Nullable
  public final E first() {
    List<E> list = asRandomAccess();
    if (list != null) {
      return list.isEmpty() ? null : list.get(0);
    }
    Iterable<E> itt = asIterable();
    if (itt == null) {
      return (E)content;
    }
    Iterator<E> iterator = itt.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  /**
   * Returns the first element if it is an instance of the specified class, otherwise null.
   */
  @Nullable
  public final <T> T first(@Nonnull Class<T> type) {
    E first = first();
    return type.isInstance(first) ? (T)first : null;
  }

  /**
   * Returns the first element if it satisfies the condition, otherwise null.
   */
  @Nullable
  public final E first(@Nonnull Predicate<? super E> condition) {
    E first = first();
    return condition.test(first) ? first : null;
  }

  /**
   * Returns the first element if it is the only one, otherwise null.
   */
  @Nullable
  public final E single() {
    List<E> list = asRandomAccess();
    if (list != null) {
      return list.size() != 1 ? null : list.get(0);
    }
    Iterable<E> itt = asIterable();
    if (itt == null) {
      return (E)content;
    }
    Iterator<E> iterator = itt.iterator();
    E first = iterator.hasNext() ? iterator.next() : null;
    return iterator.hasNext() ? null : first;
  }

  /**
   * Returns the last element in this iterable or null.
   */
  @Nullable
  public final E last() {
    List<E> list = asRandomAccess();
    if (list != null) {
      return list.isEmpty() ? null : list.get(list.size() - 1);
    }
    Iterable<E> itt = asIterable();
    if (itt == null) {
      return (E)content;
    }
    E cur = null;
    for (E e : itt) {
      cur = e;
    }
    return cur;
  }

  /**
   * Perform calculation over this iterable.
   */
  public final <T> T reduce(@Nullable T first, @Nonnull BiFunction<T, ? super E, T> function) {
    T cur = first;
    for (E e : this) {
      cur = function.apply(cur, e);
    }
    return cur;
  }

  /**
   * Returns the index of the first matching element.
   */
  public final E find(@Nonnull Predicate<? super E> condition) {
    return filter(condition).first();
  }

  /**
   * Returns the index of the matching element.
   */
  public final int indexOf(@Nonnull Predicate<? super E> condition) {
    int index = 0;
    for (E e : this) {
      if (condition.test(e)) {
        return index;
      }
      index++;
    }
    return -1;
  }

  /**
   * Synonym for map(..).filter(notNull()).
   *
   * @see JBIterable#map(Function)
   * @see JBIterable#filter(Predicate)
   */
  @Nonnull
  public final <T> JBIterable<T> filterMap(@Nonnull Function<? super E, T> function) {
    return map(function).filter(Predicates.<T>notNull());
  }

  /**
   * "Maps" and "flattens" this iterable.
   *
   * @see JBIterable#map(Function)
   * @see JBIterable#flatten(Function)
   */
  @Nonnull
  public final <T> JBIterable<T> flatMap(Function<? super E, ? extends Iterable<? extends T>> function) {
    return map(function).flatten(Function.identity());
  }

  /**
   * Returns the iterable which elements are interleaved with the separator.
   */
  @Nonnull
  public final JBIterable<E> join(@Nullable final E separator) {
    return intercept(iterator -> {
      final Iterator<E> original = iterator;
      return new JBIterator<E>() {
        boolean flag;

        @Override
        protected E nextImpl() {
          if (!original.hasNext()) return stop();
          return (flag = !flag) ? original.next() : separator;
        }
      };
    });
  }


  /**
   * Splits this {@code JBIterable} into iterable of lists of the specified size.
   * If 'strict' flag is true only groups of size 'n' are returned.
   */
  @Nonnull
  public final JBIterable<List<E>> split(final int size, final boolean strict) {
    return split(size).filterMap(es -> {
      List<E> list = es.addAllTo(new ArrayList<E>(size));
      return strict && list.size() < size ? null : list;
    });
  }

  /**
   * Splits this {@code JBIterable} into iterable of iterables of the specified size.
   * All iterations are performed in-place without data copying.
   */
  @Nonnull
  public final JBIterable<JBIterable<E>> split(final int size) {
    if (size <= 0) throw new IllegalArgumentException(size + " <= 0");
    return intercept(iterator -> {
      final Iterator<E> orig = iterator;
      return new JBIterator<JBIterable<E>>() {
        JBIterator<E> it;

        @Override
        protected JBIterable<E> nextImpl() {
          // iterate through the previous result fully before proceeding
          while (it != null && it.advance()) /* no-op */ ;
          it = null;
          return orig.hasNext() ? once((it = JBIterator.wrap(orig)).take(size)) : stop();
        }
      };
    });
  }

  public enum Split {
    AFTER,
    BEFORE,
    AROUND,
    OFF,
    GROUP
  }

  /**
   * Splits this {@code JBIterable} into iterable of iterables with separators matched by the specified condition.
   * All iterations are performed in-place without data copying.
   */
  @Nonnull
  public final JBIterable<JBIterable<E>> split(final Split mode, final Predicate<? super E> separator) {
    return intercept(iterator -> {
      final Iterator<E> orig = iterator;
      final Predicate<? super E> condition = Stateful.copy(separator);
      return new JBIterator<JBIterable<E>>() {
        JBIterator<E> it;
        E stored;
        int st; // encode transitions: -2:sep->sep, -1:val->sep, 1:sep->val, 2:val->val

        @Override
        protected JBIterable<E> nextImpl() {
          // iterate through the previous result fully before proceeding
          while (it != null && it.advance()) /* no-op */ ;
          it = null;
          // empty case: check hasNext() only if nothing is stored to be compatible with JBIterator#cursor()
          if (stored == null && !orig.hasNext()) {
            if (st < 0 && mode != Split.BEFORE && mode != Split.GROUP) {
              st = 1;
              return empty();
            }
            return stop();
          }
          // general case: add empty between 2 separators in KEEP mode; otherwise go with some state logic
          if (st == -2 && mode == Split.AROUND) {
            st = -1;
            return empty();
          }
          E tmp = stored;
          stored = null;
          return of(tmp).append(once((it = JBIterator.wrap(orig)).takeWhile(new Predicate<E>() {
            @Override
            public boolean test(E e) {
              boolean sep = condition.test(e);
              int st0 = st;
              st = st0 < 0 && sep ? -2 : st0 > 0 && !sep ? 2 : sep ? -1 : 1;
              boolean result;
              switch (mode) {
                case AFTER:
                  result = st != -2 && (st != 1 || st0 == 0);
                  break;
                case BEFORE:
                  result = st != -2 && st != -1;
                  break;
                case AROUND:
                  result = st0 >= 0 && st > 0;
                  break;
                case GROUP:
                  result = st0 >= 0 && st > 0 || st0 <= 0 && st < 0;
                  break;
                case OFF:
                  result = st > 0;
                  break;
                default:
                  throw new AssertionError(st);
              }
              stored = !result && mode != Split.OFF ? e : null;
              return result;
            }
          })));
        }
      };
    });
  }

  /**
   * Determines whether this iterable is empty.
   */
  public final boolean isEmpty() {
    if (this == EMPTY) return true;
    Collection<E> col = asCollection();
    if (col != null) {
      return col.isEmpty();
    }
    Iterable<E> itt = asIterable();
    if (itt == null) return false;
    return !itt.iterator().hasNext();
  }

  /**
   * Determines whether this iterable is not empty.
   */
  public final boolean isNotEmpty() {
    return !isEmpty();
  }

  /**
   * Collects all items into the specified collection and returns it wrapped in a new {@code JBIterable}.
   * This is equivalent to calling {@code JBIterable.from(addAllTo(c))}.
   */
  @Nonnull
  public final JBIterable<E> collect(@Nonnull Collection<E> collection) {
    return from(addAllTo(collection));
  }

  /**
   * Collects all items into an {@link ArrayList} and returns it wrapped in a new {@code JBIterable}.
   *
   * @see JBIterable#collect(Collection)
   */
  @Nonnull
  public final JBIterable<E> collect() {
    if (content instanceof Collection) return this;
    return collect(new ArrayList<E>());
  }

  /**
   * Collects all items into an {@link ArrayList}, sorts it and returns it wrapped in a new {@code JBIterable}.
   *
   * @see JBIterable#collect(Collection)
   */
  @Nonnull
  public final JBIterable<E> sorted(@Nonnull Comparator<E> comparator) {
    ArrayList<E> list = addAllTo(new ArrayList<E>());
    Collections.sort(list, comparator);
    return from(list);
  }

  /**
   * Returns a {@code List} containing all the elements from this iterable in
   * proper sequence.
   */
  @Nonnull
  public final List<E> toList() {
    if (this == EMPTY) return Collections.emptyList();
    E single = asElement();
    if (single != null) return Collections.singletonList(single);
    List<E> result = ContainerUtil.newArrayList(this);
    return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(result);
  }

  /**
   * Synonym for {@code toList().toAfunctionrray)}.
   *
   * @see List#toArray(Object[])
   */
  public final E[] toArray(@Nonnull IntFunction<E[]> function) {
    if (this == EMPTY) return function.apply(0);
    E single = asElement();
    if (single != null) return Collections.singletonList(single).toArray(function);
    return ContainerUtil.newArrayList(this).toArray(function);
  }

  /**
   * Synonym for {@code toList().toArray(array)}.
   *
   * @see List#toArray(Object[])
   */
  @Nonnull
  public final E[] toArray(@Nonnull E[] array) {
    if (this == EMPTY) return array;
    E single = asElement();
    if (single != null) return Collections.singletonList(single).toArray(array);
    return ContainerUtil.newArrayList(this).toArray(array);
  }

  /**
   * Returns a {@code Set} containing all the elements from this iterable, no duplicates.
   */
  @Nonnull
  public final Set<E> toSet() {
    if (this == EMPTY) return Collections.emptySet();
    E single = asElement();
    if (single != null) return Collections.singleton(single);
    LinkedHashSet<E> result = new LinkedHashSet<>();
    for (E e : this) {
      result.add(e);
    }
    return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
  }

  /**
   * Returns a {@code Map} for which the keys and values are defined by the specified converters.
   * {@code {@link java.util.LinkedHashMap}} is used, so the order is preserved.
   */
  @Nonnull
  public final <K, V> Map<K, V> toMap(@Nonnull Function<E, K> toKey, @Nonnull Function<E, V> toValue) {
    Map<K, V> map = new LinkedHashMap<K, V>();
    for (E e : this) map.put(toKey.apply(e), toValue.apply(e));
    return map.isEmpty() ? Collections.<K, V>emptyMap() : Collections.unmodifiableMap(map);
  }

  /**
   * A synonym for {@code toMap(Convertor.SELF, toValue)}
   *
   * @see JBIterable#toMap(Convertor, Convertor)
   */
  @Nonnull
  public final <V> Map<E, V> toMap(Function<E, V> toValue) {
    return toMap(Function.identity(), toValue);
  }

  /**
   * A synonym for {@code toMap(toKey, Convertor.SELF)}
   *
   * @see JBIterable#toMap(Convertor, Convertor)
   */
  @Nonnull
  public final <K> Map<K, E> toReverseMap(Function<E, K> toKey) {
    return toMap(toKey, Function.identity());
  }

  /**
   * Copies all the elements from this iterable to {@code collection}. This is equivalent to
   * calling {@code Iterables.addAll(collection, this)}.
   *
   * @param collection the collection to copy elements to
   * @return {@code collection}, for convenience
   */
  @Nonnull
  public final <C extends Collection<? super E>> C addAllTo(@Nonnull C collection) {
    Collection<E> col = asCollection();
    if (col != null) {
      collection.addAll(col);
    }
    else {
      Iterable<E> itt = asIterable();
      if (itt == null) {
        collection.add((E)content);
      }
      else {
        for (E item : itt) {
          collection.add(item);
        }
      }
    }
    return collection;
  }

  public abstract static class Stateful<Self extends Stateful> implements Cloneable {

    @Nonnull
    static <T> T copy(@Nonnull T o) {
      if (!(o instanceof Stateful)) return o;
      return (T)((Stateful)o).clone();
    }

    public Self clone() {
      try {
        return (Self)super.clone();
      }
      catch (CloneNotSupportedException e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Stateful {@link Predicates}: a separate cloned instance is used for each iterator.
   */
  public abstract static class SCond<T> extends Stateful<SCond> implements Predicate<T> {
  }

  /**
   * Stateful {@link Function}: a separate cloned instance is used for each iterator.
   */
  public abstract static class SFun<S, T> extends Stateful<SFun> implements Function<S, T> {
  }

}
