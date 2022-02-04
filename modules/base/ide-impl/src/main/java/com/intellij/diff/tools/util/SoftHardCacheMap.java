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
package com.intellij.diff.tools.util;

import com.intellij.util.containers.SLRUMap;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class SoftHardCacheMap<K, V> {
  @Nonnull
  private final SLRUMap<K, V> mySLRUMap;
  @Nonnull
  private final Map<K, V> mySoftLinkMap;

  public SoftHardCacheMap(final int protectedQueueSize, final int probationalQueueSize) {
    mySLRUMap = new SLRUMap<K, V>(protectedQueueSize, probationalQueueSize);
    mySoftLinkMap = ContainerUtil.createSoftValueMap();
  }

  @Nullable
  public V get(@Nonnull K key) {
    V val = mySLRUMap.get(key);
    if (val != null) return val;

    val = mySoftLinkMap.get(key);
    if (val != null) mySLRUMap.put(key, val);

    return val;
  }

  public void put(@Nonnull K key, @Nonnull V value) {
    mySLRUMap.put(key, value);
    mySoftLinkMap.put(key, value);
  }

  public boolean remove(@Nonnull K key) {
    boolean remove1 = mySLRUMap.remove(key);
    boolean remove2 = mySoftLinkMap.remove(key) != null;
    return remove1 || remove2;
  }

  public void clear() {
    mySLRUMap.clear();
    mySoftLinkMap.clear();
  }
}
