// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Set;

//@Debug.Renderer(text = "\"size = \" + size()", hasChildren = "!isEmpty()", childrenArray = "entrySet().toArray()")
public interface IntObjectMap<V> {
  V put(int key, @Nonnull V value);

  V get(int key);

  V remove(int key);

  boolean containsKey(int key);

  void clear();

  @Nonnull
  int[] keys();

  int size();

  boolean isEmpty();

  @Nonnull
  Collection<V> values();

  boolean containsValue(@Nonnull V value);

  //@Debug.Renderer(text = "getKey() + \" -> \\\"\" + getValue() + \"\\\"\"")
  interface Entry<V> {
    int getKey();

    @Nonnull
    V getValue();
  }

  @Nonnull
  Set<Entry<V>> entrySet();
}
