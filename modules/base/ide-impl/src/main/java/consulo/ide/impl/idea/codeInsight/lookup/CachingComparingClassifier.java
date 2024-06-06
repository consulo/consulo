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

import consulo.ide.impl.idea.codeInsight.completion.CompletionLookupArranger;
import consulo.util.lang.Comparing;
import consulo.language.ForceableComparable;
import consulo.language.editor.completion.lookup.Classifier;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.language.util.ProcessingContext;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class CachingComparingClassifier extends ComparingClassifier<LookupElement> {
  private final Map<LookupElement, Comparable> myWeights = new IdentityHashMap<LookupElement, Comparable>();
  private final LookupElementWeigher myWeigher;
  private Ref<Comparable> myFirstWeight;
  private boolean myPrimitive = true;
  private int myPrefixChanges = -1;

  public CachingComparingClassifier(Classifier<LookupElement> next, LookupElementWeigher weigher) {
    super(next, weigher.toString(), weigher.isNegated());
    myWeigher = weigher;
  }

  @Nullable
  @Override
  public final Comparable getWeight(LookupElement element, ProcessingContext context) {
    Comparable w = myWeights.get(element);
    if (w == null && myWeigher.isPrefixDependent()) {
      myWeights.put(element, w = myWeigher.weigh(element, context.get(CompletionLookupArranger.WEIGHING_CONTEXT)));
    }
    return w;
  }

  @Override
  public void removeElement(@Nonnull LookupElement element, @Nonnull ProcessingContext context) {
    myWeights.remove(element);
    super.removeElement(element, context);
  }

  @Nonnull
  @Override
  public Iterable<LookupElement> classify(@Nonnull Iterable<LookupElement> source, @Nonnull ProcessingContext context) {
    if (!myWeigher.isPrefixDependent() && myPrimitive) {
      return myNext.classify(source, context);
    }
    checkPrefixChanged(context);

    return super.classify(source, context);
  }

  private void checkPrefixChanged(ProcessingContext context) {
    int actualPrefixChanges = context.get(CompletionLookupArranger.PREFIX_CHANGES).intValue();
    if (myWeigher.isPrefixDependent() && myPrefixChanges != actualPrefixChanges) {
      myPrefixChanges = actualPrefixChanges;
      myWeights.clear();
    }
  }

  @Nonnull
  @Override
  public List<Pair<LookupElement, Object>> getSortingWeights(@Nonnull Iterable<LookupElement> items, @Nonnull ProcessingContext context) {
    checkPrefixChanged(context);
    return super.getSortingWeights(items, context);
  }

  @Override
  public void addElement(@Nonnull LookupElement t, @Nonnull ProcessingContext context) {
    Comparable weight = myWeigher.weigh(t, context.get(CompletionLookupArranger.WEIGHING_CONTEXT));
    if (weight instanceof ForceableComparable) {
      ((ForceableComparable)weight).force();
    }
    if (!myWeigher.isPrefixDependent() && myPrimitive) {
      if (myFirstWeight == null) {
        myFirstWeight = Ref.create(weight);
      } else if (!Comparing.equal(myFirstWeight.get(), weight)) {
        myPrimitive = false;
      }
    }
    myWeights.put(t, weight);
    super.addElement(t, context);
  }

}
