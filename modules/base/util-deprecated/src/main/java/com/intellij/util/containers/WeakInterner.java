// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.containers;

import com.intellij.util.ConcurrencyUtil;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import javax.annotation.Nonnull;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
 * and garbage-collected when not needed anymore.
 *
 * @author peter
 */
public class WeakInterner<T> extends Interner<T> {
  private final ConcurrentMap<T, T> myMap;

  public WeakInterner() {
    myMap = ContainerUtil.createConcurrentWeakKeyWeakValueMap();
  }

  public WeakInterner(@Nonnull TObjectHashingStrategy<T> strategy) {
    myMap = ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy);
  }

  @Override
  @Nonnull
  public T intern(@Nonnull T name) {
    return ConcurrencyUtil.cacheOrGet(myMap, name, name);
  }

  @Override
  public void clear() {
    myMap.clear();
  }

  @Override
  @Nonnull
  public Set<T> getValues() {
    return new THashSet<>(myMap.values());
  }
}
