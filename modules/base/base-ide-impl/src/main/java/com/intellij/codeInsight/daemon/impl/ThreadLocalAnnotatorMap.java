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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.application.ApplicationManager;
import consulo.injecting.InjectingContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * User: cdr
 */
abstract class ThreadLocalAnnotatorMap<K, V> {
  private volatile int version;
  @Nonnull
  public abstract Collection<V> initialValue(@Nonnull K key);

  private static class VersionedMap<K, V> extends HashMap<K, List<V>> {
    private final int version;

    private VersionedMap(int version) {
      this.version = version;
    }
  }

  private final ThreadLocal<VersionedMap<K, V>> CACHE = new ThreadLocal<VersionedMap<K, V>>(){
    @Override
    protected VersionedMap<K, V> initialValue() {
      return new VersionedMap<>(version);
    }
  };

  @SuppressWarnings("unchecked")
  @Nonnull
  private List<V> cloneTemplates(@Nonnull Collection<V> templates) {
    List<V> result = new ArrayList<>(templates.size());
    InjectingContainer container = ApplicationManager.getApplication().getInjectingContainer();
    for (V template : templates) {
      Class<? extends V> aClass = (Class<? extends V>)template.getClass();
      result.add(container.getUnbindedInstance(aClass));
    }
    return result;
  }

  @Nonnull
  public List<V> get(@Nonnull K key) {
    VersionedMap<K, V> map = CACHE.get();
    if (version != map.version) {
      CACHE.remove();
      map = CACHE.get();
    }
    List<V> cached = map.get(key);
    if (cached == null) {
      Collection<V> templates = initialValue(key);
      cached = cloneTemplates(templates);
      map.put(key, cached);
    }
    return cached;
  }

  public void clear() {
    version++; //we don't care about atomicity here
  }
}
