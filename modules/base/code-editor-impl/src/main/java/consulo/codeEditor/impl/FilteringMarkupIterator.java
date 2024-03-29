// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.document.MarkupIterator;
import jakarta.annotation.Nonnull;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringMarkupIterator<T> implements MarkupIterator<T> {
  @Nonnull
  private final MarkupIterator<T> myDelegate;
  @Nonnull
  private final Predicate<? super T> myFilter;

  public FilteringMarkupIterator(@Nonnull MarkupIterator<T> delegate, @Nonnull Predicate<? super T> filter) {
    myDelegate = delegate;
    myFilter = filter;
    skipUnrelated();
  }

  @Override
  public void dispose() {
    myDelegate.dispose();
  }

  @Override
  public T peek() throws NoSuchElementException {
    return myDelegate.peek();
  }

  @Override
  public boolean hasNext() {
    return myDelegate.hasNext();
  }

  @Override
  public T next() {
    T result = myDelegate.next();
    skipUnrelated();
    return result;
  }

  private void skipUnrelated() {
    while (myDelegate.hasNext() && !myFilter.test(myDelegate.peek())) myDelegate.next();
  }
}
