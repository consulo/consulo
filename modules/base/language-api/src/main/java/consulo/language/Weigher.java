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
package consulo.language;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to add custom logic to common comparators. Should be registered under "weigher" extension point with "key" parameter specified.
 * It's almost a must to specify how your weigher relates to the others by priority (see {@link LoadingOrder}).
 * <p>
 * Known key values include:
 * <li> "proximity" to measure proximity level of an element in a particular place (location)  PsiProximityComparator.WEIGHER_KEY
 * <li> "completion" ({@link CompletionService#RELEVANCE_KEY}) - to compare lookup elements by relevance and move preferred items to the top
 * <p>
 * Your weigher should return {@link Comparable} instances of the same type.
 *
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Weigher<T, Location> {
  private static final ExtensionPointCacheKey<Weigher, Map<Key, List<Weigher>>> KEY = ExtensionPointCacheKey.create("Weigher", walker -> {
    Map<Key, List<Weigher>> map = new HashMap<>();
    walker.walk(weigher -> map.computeIfAbsent(weigher.getKey(), key -> new ArrayList<>()).add(weigher));
    return map;
  });

  @Nonnull
  public static  List<Weigher> forKey(@Nonnull Key key) {
    Map<Key, List<Weigher>> map = Application.get().getExtensionPoint(Weigher.class).getOrBuildCache(KEY);
    return map.getOrDefault(key, List.of());
  }

  private final String myDebugName;

  public Weigher() {
    ExtensionImpl extensionImplAnnotation = getClass().getAnnotation(ExtensionImpl.class);
    if (extensionImplAnnotation != null) {
      myDebugName = extensionImplAnnotation.id();
    } else {
      throw new IllegalArgumentException("@ExtensionImpl expected");
    }
  }

  protected Weigher(String debugName) {
    myDebugName = debugName;
  }

  @Override
  public String toString() {
    return myDebugName == null ? super.toString() : myDebugName;
  }

  @Nullable
  public abstract Comparable weigh(@Nonnull T element, @Nonnull Location location);

  @Nonnull
  public abstract Key<?> getKey();
}
