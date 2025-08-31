/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Functions;
import consulo.util.lang.function.MonoFunction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Iterator that accumulates transformations and filters keeping its instance.
 * So JBIterable#filter() and JBIterable#transform() preserve the underlying iterator API.
 *
 * <h3>Supported contracts:</h3>
 * <ul>
 *   <li>Classic iterator: hasNext() / next()</li>
 *   <li>Cursor: advance() / current()</li>
 *   <li>One-time iterable: cursor()</li>
 * </ul>
 *
 * Implementors should provide nextImpl() method which can call stop()/skip().
 *
 * @see JBIterable#map(Function)
 * @see JBIterable#filter(Predicate)
 * @see TreeTraversal.TracingIt
 *
 * @author gregsh
 *
 * @noinspection unchecked, TypeParameterHidesVisibleType, AssignmentToForLoopParameter
 */
public abstract class JBIterator<E> implements Iterator<E> {

  @Nonnull
  public static <E extends JBIterator<?>> JBIterable<E> cursor(@Nonnull E iterator) {
    return JBIterable.<E>generate(iterator, Functions.<E>id()).intercept(CURSOR_NEXT);
  }

  @Nonnull
  public static <E> JBIterator<E> from(@Nonnull Iterator<E> it) {
    return it instanceof JBIterator ? (JBIterator<E>)it : wrap(it);
  }

  @Nonnull
  static <E> JBIterator<E> wrap(@Nonnull final Iterator<E> it) {
    return new JBIterator<E>() {
      @Override
      protected E nextImpl() {
        return it.hasNext() ? it.next() : stop();
      }
    };
  }

  private enum Do {INIT, STOP, SKIP}
  private Object myCurrent = Do.INIT;
  private Object myNext = Do.INIT;

  private Op myFirstOp = new NextOp();
  private Op myLastOp = myFirstOp;

  /**
   * Returns the next element if any; otherwise calls stop() or skip().
   */
  protected abstract E nextImpl();

  /**
   * Called right after the new current value is set.
   */
  protected void currentChanged() { }

  /**
   * Notifies the iterator that there's no more elements.
   */
  @Nullable
  protected final E stop() {
    myNext = Do.STOP;
    return null;
  }

  /**
   * Notifies the iterator to skip and re-invoke nextImpl().
   */
  @Nullable
  protected final E skip() {
    myNext = Do.SKIP;
    return null;
  }

  @Override
  public final boolean hasNext() {
    peekNext();
    return myNext != Do.STOP;
  }

  @Override
  public final E next() {
    advance();
    return current();
  }

  /**
   * Proceeds to the next element if any and returns true; otherwise false.
   */
  public final boolean advance() {
    myCurrent = Do.INIT;
    peekNext();
    if (myNext == Do.STOP) return false;
    myCurrent = myNext;
    myNext = Do.INIT;
    if (myFirstOp instanceof JBIterator.CursorOp) {
      ((CursorOp)myFirstOp).advance(myCurrent);
    }
    currentChanged();
    return true;
  }

  /**
   * Returns the current element if any; otherwise throws exception.
   */
  public final E current() {
    if (myCurrent == Do.INIT) {
      throw new NoSuchElementException();
    }
    return (E)myCurrent;
  }

  private void peekNext() {
    if (myNext != Do.INIT) return;
    Object o = Do.INIT;
    for (Op op = myFirstOp; op != null; op = op == null ? myFirstOp : op.nextOp) {
      o = op.apply(op.impl == null ? nextImpl() : o);
      if (myNext == Do.STOP) return;
      if (myNext == Do.SKIP) {
        o = myNext = Do.INIT;
        if (op.impl == null) {
          // rollback all prepended takeWhile conditions if nextImpl() votes SKIP
          for (Op op2 = myFirstOp; op2.impl instanceof CountDown; op2 = op2.nextOp) {
            ((CountDown)op2.impl).cur ++;
          }
        }
        op = null;
      }
    }
    myNext = o;
  }

  @Nonnull
  public final <T> JBIterator<T> map(@Nonnull Function<? super E, T> function) {
    return addOp(true, new TransformOp<E, T>(function));
  }

  @Nonnull
  public final JBIterator<E> filter(@Nonnull Predicate<? super E> condition) {
    return addOp(true, new FilterOp<E>(condition));
  }

  @Nonnull
  public final JBIterator<E> take(int count) {
    // add first so that the underlying iterator stay on 'count' position
    return addOp(!(myLastOp instanceof NextOp), new WhileOp<E>(new CountDown<E>(count)));
  }

  @Nonnull
  public final JBIterator<E> takeWhile(@Nonnull Predicate<? super E> condition) {
    return addOp(true, new WhileOp<E>(condition));
  }

  @Nonnull
  public final JBIterator<E> skip(int count) {
    return skipWhile(new CountDown<E>(count));
  }

  @Nonnull
  public final JBIterator<E> skipWhile(@Nonnull Predicate<? super E> condition) {
    return addOp(true, new SkipOp<E>(condition));
  }

  @Nonnull
  private <T> T addOp(boolean last, @Nonnull Op op) {
    if (op.impl == null) {
      myFirstOp = myLastOp = op;
    }
    else if (last) {
      myLastOp.nextOp = op;
      myLastOp = myLastOp.nextOp;
    }
    else {
      op.nextOp = myFirstOp;
      myFirstOp = op;
    }
    return (T)this;
  }

  @Override
  public final void remove() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public final List<E> toList() {
    return Collections.unmodifiableList(ContainerUtil.newArrayList(JBIterable.once(this)));
  }

  @Override
  public String toString() {
    List<Op> ops = operationsImpl().toList();
    return "{cur=" + myCurrent + "; next=" + myNext + (ops.size() < 2 ? "" : "; ops=" + ops) + "}";
  }

  @Nonnull
  public final JBIterable<Function<Object, Object>> getTransformations() {
    return (JBIterable<Function<Object, Object>>)(JBIterable)operationsImpl().map(op -> op.impl).filter(Function.class);
  }

  @Nonnull
  private JBIterable<Op> operationsImpl() {
    return JBIterable.generate(myFirstOp, op -> op.nextOp);
  }

  @Nonnull
  static String toShortString(@Nonnull Object o) {
    String name = o.getClass().getName();
    int idx = name.lastIndexOf('$');
    if (idx > 0 && idx < name.length() && StringUtil.isJavaIdentifierStart(name.charAt(idx + 1))) {
      return name.substring(idx + 1);
    }
    return name.substring(name.lastIndexOf('.') + 1);
  }

  private static final MonoFunction CURSOR_NEXT = new MonoFunction<JBIterator<?>>() {
    @Override
    public JBIterator<?> apply(JBIterator<?> iterator) {
      return iterator.addOp(false, iterator.new CursorOp());
    }
  };

  private static class Op<T> {
    final T impl;
    Op nextOp;

    public Op(T impl) {
      this.impl = impl;
    }

    Object apply(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return toShortString(impl == null ? this : impl);
    }
  }

  private static class CountDown<A> implements Predicate<A> {
    int cur;

    public CountDown(int count) {
      cur = count;
    }

    @Override
    public boolean test(A a) {
      return cur > 0 && cur-- != 0;
    }
  }

  private static class TransformOp<E, T> extends Op<Function<? super E, T>> {
    TransformOp(Function<? super E, T> function) {
      super(function);
    }

    @Override
    Object apply(Object o) {
      return impl.apply((E)o);
    }
  }

  private class FilterOp<E> extends Op<Predicate<? super E>> {
    FilterOp(Predicate<? super E> condition) {
      super(condition);
    }

    @Override
    Object apply(Object o) {
      return impl.test((E)o) ? o : skip();
    }
  }

  private class WhileOp<E> extends Op<Predicate<? super E>> {

    WhileOp(Predicate<? super E> condition) {
      super(condition);
    }
    @Override
    Object apply(Object o) {
      return impl.test((E)o) ? o : stop();
    }
  }

  private class SkipOp<E> extends Op<Predicate<? super E>> {
    boolean active = true;

    SkipOp(Predicate<? super E> condition) {
      super(condition);
    }

    @Override
    Object apply(Object o) {
      if (active && impl.test((E)o)) return skip();
      active = false;
      return o;
    }
  }

  private static class NextOp extends Op<Void> {
    NextOp() {
      super(null);
    }

    @Override
    Object apply(Object o) {
      return o;
    }
  }

  private class CursorOp extends Op<Void> {
    boolean advanced;

    CursorOp() {
      super(null);
    }

    @Override
    Object apply(Object o) {
      JBIterator<?> it = (JBIterator<?>)o;
      return ((advanced = nextOp != null) ? it.advance() : it.hasNext()) ? it : stop();
    }

    void advance(Object o) {
      if (advanced || !(o instanceof JBIterator)) return;
      ((JBIterator)o).advance();
      advanced = true;
    }
  }
}
