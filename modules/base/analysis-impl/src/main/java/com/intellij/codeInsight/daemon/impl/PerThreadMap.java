/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.Pair;
import consulo.injecting.InjectingContainer;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * User: cdr
 */
public abstract class PerThreadMap<T, KeyT extends UserDataHolder> {
  private volatile int version;
  @Nonnull
  public abstract Collection<T> initialValue(@Nonnull KeyT key);

  // pair(version, map)
  private final ThreadLocal<Pair<Integer, Map<KeyT,List<T>>>> CACHE = new ThreadLocal<Pair<Integer, Map<KeyT,List<T>>>>(){
    @Override
    protected Pair<Integer, Map<KeyT,List<T>>> initialValue() {
      return Pair.<Integer, Map<KeyT,List<T>>>create(version, new HashMap<KeyT, List<T>>());
    }
  };

  @SuppressWarnings("unchecked")
  @Nonnull
  private List<T> cloneTemplates(@Nonnull Collection<T> templates) {
    List<T> result = new ArrayList<T>(templates.size());
    InjectingContainer container = Application.get().getInjectingContainer();
    for (T template : templates) {
      Class<? extends T> aClass = (Class<? extends T>)template.getClass();
      result.add(container.getUnbindedInstance(aClass));
    }
    return result;
  }

  @Nonnull
  public List<T> get(@Nonnull KeyT key) {
    Pair<Integer, Map<KeyT, List<T>>> pair = CACHE.get();
    Integer mapVersion = pair.getFirst();
    if (version != mapVersion) {
      CACHE.remove();
      pair = CACHE.get();
    }
    Map<KeyT, List<T>> map = pair.getSecond();
    List<T> cached = map.get(key);
    if (cached == null) {
      Collection<T> templates = initialValue(key);
      cached = cloneTemplates(templates);
      map.put(key, cached);
    }
    return cached;
  }

  public void clear() {
    version++; //we don't care about atomicity here
  }
}
