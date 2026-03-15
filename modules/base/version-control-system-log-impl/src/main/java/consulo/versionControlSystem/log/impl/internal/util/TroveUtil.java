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
package consulo.versionControlSystem.log.impl.internal.util;

import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.ref.Ref;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import org.jspecify.annotations.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TroveUtil {
  
  public static <T> Stream<T> streamValues(TIntObjectHashMap<T> map) {
    TIntObjectIterator<T> it = map.iterator();
    return Stream.generate(() -> {
      it.advance();
      return it.value();
    }).limit(map.size());
  }

  
  public static IntStream streamKeys(TIntObjectHashMap<?> map) {
    TIntObjectIterator<?> it = map.iterator();
    return IntStream.generate(() -> {
      it.advance();
      return it.key();
    }).limit(map.size());
  }

  
  public static IntStream stream(IntList list) {
    return list.stream();
  }

  
  public static Set<Integer> intersect(IntSet... sets) {
    IntSet result = null;

    Arrays.sort(sets, (set1, set2) -> {
      if (set1 == null) return -1;
      if (set2 == null) return 1;
      return set1.size() - set2.size();
    });
    for (IntSet set : sets) {
      result = intersect(result, set);
    }

    if (result == null) return new HashSet<>();
    return createJavaSet(result);
  }

  @Nullable
  private static IntSet intersect(@Nullable IntSet set1, @Nullable IntSet set2) {
    if (set1 == null) return set2;
    if (set2 == null) return set1;

    IntSet result = IntSets.newHashSet();

    if (set1.size() < set2.size()) {
      set1.forEach(value -> {
        if (set2.contains(value)) {
          result.add(value);
        }
      });
    }
    else {
      set2.forEach(value -> {
        if (set1.contains(value)) {
          result.add(value);
        }
      });
    }

    return result;
  }

  
  private static Set<Integer> createJavaSet(IntSet set) {
    Set<Integer> result = new HashSet<>(set.size());
    set.forEach(result::add);
    return result;
  }

  public static void addAll(TIntHashSet where, TIntHashSet what) {
    what.forEach(value -> {
      where.add(value);
      return true;
    });
  }

  
  public static IntStream stream(TIntHashSet set) {
    TIntIterator it = set.iterator();
    return IntStream.generate(it::next).limit(set.size());
  }

  
  public static <T> List<T> map(TIntHashSet set, IntFunction<T> function) {
    return stream(set).mapToObj(function).collect(Collectors.toList());
  }

  public static void processBatches(IntStream stream, int batchSize, Consumer<TIntHashSet> consumer) {
    Ref<TIntHashSet> batch = new Ref<>(new TIntHashSet());
    stream.forEach(commit -> {
      batch.get().add(commit);
      if (batch.get().size() >= batchSize) {
        try {
          consumer.accept(batch.get());
        }
        finally {
          batch.set(new TIntHashSet());
        }
      }
    });

    if (!batch.get().isEmpty()) {
      consumer.accept(batch.get());
    }
  }
}
