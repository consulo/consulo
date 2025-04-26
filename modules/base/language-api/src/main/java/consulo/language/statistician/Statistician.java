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
package consulo.language.statistician;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Statistician<T, Loc, Stat extends Statistician<T, Loc, Stat>> {
  private static final ExtensionPointCacheKey<Statistician, MultiMap<Key, Statistician>> CACHE_KEY = ExtensionPointCacheKey.create("Statistician", walker -> {
    MultiMap<Key, Statistician> map = MultiMap.create();
    walker.walk(statistician -> map.putValue(statistician.getKey(), statistician));
    return map;
  });

  @Nonnull
  public static Collection<Statistician> forKey(@Nonnull Key<? extends Statistician> key) {
    MultiMap<Key, Statistician> map = Application.get().getExtensionPoint(Statistician.class).getOrBuildCache(CACHE_KEY);
    return map.get(key);
  }

  @Nullable
  public abstract StatisticsInfo serialize(T element, Loc location);

  @Nonnull
  public abstract Key<Stat> getKey();
}
