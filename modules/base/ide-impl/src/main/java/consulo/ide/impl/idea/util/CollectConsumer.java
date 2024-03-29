// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util;

import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author peter
 */
public class CollectConsumer<T> implements Consumer<T> {
  private final Collection<T> myResult;

  public CollectConsumer(@Nonnull Collection<T> result) {
    myResult = result;
  }

  public CollectConsumer() {
    this(new SmartList<>());
  }

  @Override
  public void accept(T t) {
    myResult.add(t);
  }

  @Nonnull
  public Collection<T> getResult() {
    return myResult;
  }
}
