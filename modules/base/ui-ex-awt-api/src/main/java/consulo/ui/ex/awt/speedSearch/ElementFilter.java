// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.speedSearch;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public interface ElementFilter<T> {
  boolean shouldBeShowing(T value);

  interface Active<T> extends ElementFilter<T> {
    @Nonnull
    Promise<?> fireUpdate(@Nullable T preferredSelection, boolean adjustSelection, boolean now);

    void addListener(Listener<T> listener, Disposable parent);

    abstract class Impl<T> implements Active<T> {
      Set<Listener<T>> myListeners = new CopyOnWriteArraySet<>();

      @Override
      @Nonnull
      public Promise<?> fireUpdate(@Nullable T preferredSelection, boolean adjustSelection, boolean now) {
        return Promises.all(ContainerUtil.map(myListeners, listener -> listener.update(preferredSelection, adjustSelection, now)));
      }

      @Override
      public void addListener(final Listener<T> listener, Disposable parent) {
        myListeners.add(listener);
        Disposer.register(parent, new Disposable() {
          @Override
          public void dispose() {
            myListeners.remove(listener);
          }
        });
      }
    }
  }

  interface Listener<T> {
    @Nonnull
    Promise<Void> update(@Nullable T preferredSelection, boolean adjustSelection, boolean now);
  }
}