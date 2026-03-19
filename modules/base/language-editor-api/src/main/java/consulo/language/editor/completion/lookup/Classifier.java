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
package consulo.language.editor.completion.lookup;

import consulo.language.util.ProcessingContext;
import consulo.util.lang.Pair;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author peter
 */
public abstract class Classifier<T> {
  protected final Classifier<T> myNext;
  private final String myName;

  protected Classifier(@Nullable Classifier<T> next, String name) {
    myNext = next;
    myName = name;
  }

  public void addElement(T t, ProcessingContext context) {
    if (myNext != null) {
      myNext.addElement(t, context);
    }
  }

  
  public abstract Iterable<T> classify(Iterable<T> source, ProcessingContext context);

  /**
   * @return a mapping from the given items to objects (e.g. Comparable instances) used to sort the items in {@link #classify(Iterable, ProcessingContext)}.
   * May return an empty list if there are no suitable objects available.
   * Used for diagnostics and statistic collection.
   */
  
  public abstract List<Pair<T, Object>> getSortingWeights(Iterable<T> items, ProcessingContext context);

  public final @Nullable Classifier<T> getNext() {
    return myNext;
  }

  public void removeElement(T element, ProcessingContext context) {
    if (myNext != null) {
      myNext.removeElement(element, context);
    }
  }

  
  public final String getPresentableName() {
    return myName;
  }
}
