/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.lookup;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.FlatteningIterator;
import consulo.language.editor.completion.lookup.Classifier;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * @author peter
 */
public abstract class ComparingClassifier<T> extends Classifier<T> {
  private final boolean myNegated;

  protected ComparingClassifier(Classifier<T> next, String name, boolean negated) {
    super(next, name);
    myNegated = negated;
  }

  @Nullable
  public abstract Comparable getWeight(T t, ProcessingContext context);

  @Nonnull
  @Override
  public Iterable<T> classify(@Nonnull final Iterable<T> source, @Nonnull final ProcessingContext context) {
    List<T> nulls = null;
    TreeMap<Comparable, List<T>> map = new TreeMap<Comparable, List<T>>();
    for (T t : source) {
      final Comparable weight = getWeight(t, context);
      if (weight == null) {
        if (nulls == null) nulls = new SmartList<T>();
        nulls.add(t);
      } else {
        List<T> list = map.get(weight);
        if (list == null) {
          map.put(weight, list = new SmartList<T>());
        }
        list.add(t);
      }
    }

    final List<List<T>> values = new ArrayList<List<T>>();
    values.addAll(myNegated ? map.descendingMap().values() : map.values());
    ContainerUtil.addIfNotNull(values, nulls);

    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new FlatteningIterator<List<T>, T>(values.iterator()) {
          @Override
          protected Iterator<T> createValueIterator(List<T> group) {
            return myNext.classify(group, context).iterator();
          }
        };
      }
    };
  }

  @Nonnull
  @Override
  public List<Pair<T, Object>> getSortingWeights(@Nonnull Iterable<T> items, @Nonnull final ProcessingContext context) {
    return ContainerUtil.map(items, t -> new Pair<T, Object>(t, getWeight(t, context)));
  }
}
