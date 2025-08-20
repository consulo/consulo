/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.index.io;

import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class InvertedIndexUtil {
  @Nonnull
  public static <K, V, I> IntSet collectInputIdsContainingAllKeys(@Nonnull InvertedIndex<? super K, V, I> index,
                                                                  @Nonnull Collection<? extends K> dataKeys,
                                                                  @Nullable Predicate<? super K> keyChecker,
                                                                  @Nullable Predicate<? super V> valueChecker,
                                                                  @Nullable ValueContainer.IntPredicate idChecker) throws StorageException {
    IntSet mainIntersection = null;

    for (K dataKey : dataKeys) {
      if (keyChecker != null && !keyChecker.test(dataKey)) continue;

      final IntSet copy = IntSets.newHashSet();
      final ValueContainer<V> container = index.getData(dataKey);

      for (ValueContainer.ValueIterator<V> valueIt = container.getValueIterator(); valueIt.hasNext(); ) {
        final V value = valueIt.next();
        if (valueChecker != null && !valueChecker.test(value)) {
          continue;
        }

        ValueContainer.IntIterator iterator = valueIt.getInputIdsIterator();

        final ValueContainer.IntPredicate predicate;
        if (mainIntersection == null || iterator.size() < mainIntersection.size() || (predicate = valueIt.getValueAssociationPredicate()) == null) {
          while (iterator.hasNext()) {
            final int id = iterator.next();
            if (mainIntersection == null && (idChecker == null || idChecker.contains(id)) || mainIntersection != null && mainIntersection.contains(id)) {
              copy.add(id);
            }
          }
        }
        else {
          mainIntersection.forEach(id -> {
            if (predicate.contains(id)) copy.add(id);
          });
        }
      }

      mainIntersection = copy;
      if (mainIntersection.isEmpty()) {
        return IntSet.of();
      }
    }

    return mainIntersection == null ? IntSet.of() : mainIntersection;
  }
}
