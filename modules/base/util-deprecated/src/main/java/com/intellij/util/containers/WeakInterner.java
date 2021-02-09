// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.containers;

import consulo.util.collection.HashingStrategy;
import consulo.util.containers.MapBasedInterner;

import javax.annotation.Nonnull;

/**
 * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
 * and garbage-collected when not needed anymore.
 *
 * @author peter
 */
class WeakInterner<T> extends MapBasedInterner<T> {
  WeakInterner() {
    super(ContainerUtil.createConcurrentWeakKeyWeakValueMap());
  }

  WeakInterner(@Nonnull HashingStrategy<T> strategy) {
    super(ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy));
  }
}
