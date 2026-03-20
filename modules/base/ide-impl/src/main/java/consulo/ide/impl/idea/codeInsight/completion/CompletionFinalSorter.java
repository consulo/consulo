/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.util.lang.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * IMPORTANT: DO NOT USE IT
 * Supposed to be used ONLY by plugin allowing to sort completion using ml-ranking algorithm.
 * Needed to sort items from different sorters together.
 */
public abstract class CompletionFinalSorter {

  
  public abstract Iterable<? extends LookupElement> sort(Iterable<? extends LookupElement> initial, CompletionParameters parameters);

  /**
   * For debugging purposes, provide weights by which completion will be sorted.
   */
  public abstract Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(Iterable<LookupElement> elements);

  //@ApiStatus.Internal
  public interface Factory {
    
    CompletionFinalSorter newSorter();
  }

  
  public static CompletionFinalSorter newSorter() {
    return EMPTY_SORTER;
  }

  private static final CompletionFinalSorter EMPTY_SORTER = new CompletionFinalSorter() {
    
    @Override
    public Iterable<? extends LookupElement> sort(Iterable<? extends LookupElement> initial, CompletionParameters parameters) {
      return initial;
    }

    
    @Override
    public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(Iterable<LookupElement> elements) {
      return Collections.emptyMap();
    }
  };
}

