/*
 * Copyright 2013-2025 consulo.io
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
package consulo.virtualFileSystem.impl.internal.entry;

import consulo.util.collection.impl.map.LinkedHashMap;
import consulo.util.dataholder.internal.keyFMap.ArrayBackedFMap;
import consulo.util.dataholder.internal.keyFMap.KeyFMap;
import consulo.util.dataholder.internal.keyFMap.OneElementFMap;
import consulo.util.dataholder.internal.keyFMap.PairElementsFMap;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * @author peter
 */
class UserDataInterner {
  private static final Map<MapReference, MapReference> ourCache = new LinkedHashMap<MapReference, MapReference>(20, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<MapReference, MapReference> eldest) {
      return size() > 15;
    }
  };

  static KeyFMap internUserData(@Nonnull KeyFMap map) {
    if (shouldIntern(map)) {
      MapReference key = new MapReference(map);
      synchronized (ourCache) {
        KeyFMap cached = SoftReference.dereference(ourCache.get(key));
        if (cached != null) return cached;

        ourCache.put(key, key);
      }
      return map;
    }
    return map;
  }

  private static boolean shouldIntern(@Nonnull KeyFMap map) {
    return map instanceof OneElementFMap ||
           map instanceof PairElementsFMap ||
           map instanceof ArrayBackedFMap && ((ArrayBackedFMap)map).getKeyIds().length <= 5;
  }
}
