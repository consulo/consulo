/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.completion.impl;

import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.FlatteningIterator;
import consulo.language.editor.completion.lookup.Classifier;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.FilteringIterator;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

import static consulo.ide.impl.idea.util.containers.ContainerUtil.newIdentityHashMap;
import static consulo.ide.impl.idea.util.containers.ContainerUtil.newIdentityTroveSet;

/**
 * @author peter
 */
public class LiftShorterItemsClassifier extends Classifier<LookupElement> {
  private final TreeSet<String> mySortedStrings = new TreeSet<>();
  private final MultiMap<String, LookupElement> myElements = createMultiMap(false);
  private final MultiMap<LookupElement, LookupElement> myToLift = createMultiMap(true);
  private final MultiMap<LookupElement, LookupElement> myReversedToLift = createMultiMap(true);
  private final LiftingCondition myCondition;
  private final boolean myLiftBefore;
  private int myCount = 0;

  public LiftShorterItemsClassifier(String name, Classifier<LookupElement> next, LiftingCondition condition, boolean liftBefore) {
    super(next, name);
    myCondition = condition;
    myLiftBefore = liftBefore;
  }

  @Override
  public void addElement(@Nonnull LookupElement added, @Nonnull ProcessingContext context) {
    myCount++;

    for (String string : CompletionUtil.iterateLookupStrings(added)) {
      if (string.length() == 0) continue;

      myElements.putValue(string, added);
      mySortedStrings.add(string);
      NavigableSet<String> after = mySortedStrings.tailSet(string, false);
      for (String s : after) {
        if (!s.startsWith(string)) {
          break;
        }
        for (LookupElement longer : myElements.get(s)) {
          updateLongerItem(added, longer);
        }
      }
    }
    super.addElement(added, context);

    calculateToLift(added);
  }

  private void updateLongerItem(LookupElement shorter, LookupElement longer) {
    if (myCondition.shouldLift(shorter, longer)) {
      myToLift.putValue(longer, shorter);
      myReversedToLift.putValue(shorter, longer);
    }
  }

  private void calculateToLift(LookupElement element) {
    for (String string : CompletionUtil.iterateLookupStrings(element)) {
      for (int len = 1; len < string.length(); len++) {
        String prefix = string.substring(0, len);
        for (LookupElement shorterElement : myElements.get(prefix)) {
          if (myCondition.shouldLift(shorterElement, element)) {
            myToLift.putValue(element, shorterElement);
            myReversedToLift.putValue(shorterElement, element);
          }
        }
      }
    }
  }

  @Nonnull
  @Override
  public Iterable<LookupElement> classify(@Nonnull Iterable<LookupElement> source, @Nonnull ProcessingContext context) {
    return liftShorterElements(source, null, context);
  }

  private Iterable<LookupElement> liftShorterElements(Iterable<LookupElement> source,
                                                      @Nullable Set<LookupElement> lifted, ProcessingContext context) {
    Set<LookupElement> srcSet = newIdentityTroveSet(source instanceof Collection ? ((Collection)source).size() : myCount);
    ContainerUtil.addAll(srcSet, source);

    if (srcSet.size() < 2) {
      return myNext.classify(source, context);
    }

    return new LiftingIterable(srcSet, context, source, lifted);
  }

  @Nonnull
  @Override
  public List<Pair<LookupElement, Object>> getSortingWeights(@Nonnull Iterable<LookupElement> items, @Nonnull ProcessingContext context) {
    Set<LookupElement> lifted = newIdentityTroveSet();
    Iterable<LookupElement> iterable = liftShorterElements(ContainerUtil.newArrayList(items), lifted, context);
    return ContainerUtil.map(iterable, element -> new Pair<LookupElement, Object>(element, lifted.contains(element)));
  }

  @Override
  public void removeElement(@Nonnull LookupElement element, @Nonnull ProcessingContext context) {
    for (String s : CompletionUtil.iterateLookupStrings(element)) {
      myElements.remove(s, element);
      if (myElements.get(s).isEmpty()) {
        mySortedStrings.remove(s);
      }
    }

    removeFromMap(element, myToLift, myReversedToLift);
    removeFromMap(element, myReversedToLift, myToLift);

    super.removeElement(element, context);
  }

  private static void removeFromMap(LookupElement key,
                                    MultiMap<LookupElement, LookupElement> mainMap,
                                    MultiMap<LookupElement, LookupElement> inverseMap) {
    Collection<LookupElement> removed = mainMap.remove(key);
    if (removed == null) return;

    for (LookupElement reference : ContainerUtil.newArrayList(removed)) {
      inverseMap.remove(reference, key);
    }
  }

  public static class LiftingCondition {
    public boolean shouldLift(LookupElement shorterElement, LookupElement longerElement) {
      return true;
    }
  }

  private class LiftingIterable implements Iterable<LookupElement> {
    private final Set<LookupElement> mySrcSet;
    private final ProcessingContext myContext;
    private final Iterable<LookupElement> mySource;
    private final Set<LookupElement> myLifted;

    public LiftingIterable(Set<LookupElement> srcSet,
                           ProcessingContext context,
                           Iterable<LookupElement> source,
                           Set<LookupElement> lifted) {
      mySrcSet = srcSet;
      myContext = context;
      mySource = source;
      myLifted = lifted;
    }

    @Override
    public Iterator<LookupElement> iterator() {
      final Set<LookupElement> processed = newIdentityTroveSet(mySrcSet.size());
      final Set<Collection<LookupElement>> arraysProcessed = newIdentityTroveSet();

      Iterable<LookupElement> next = myNext.classify(mySource, myContext);
      Iterator<LookupElement> base = FilteringIterator.create(next.iterator(), element -> processed.add(element));
      return new FlatteningIterator<>(base) {
        @Override
        protected Iterator<LookupElement> createValueIterator(LookupElement element) {
          List<LookupElement> shorter = addShorterElements(myToLift.get(element));
          List<LookupElement> singleton = Collections.singletonList(element);
          if (shorter != null) {
            if (myLifted != null) {
              myLifted.addAll(shorter);
            }
            Iterable<LookupElement> lifted = myNext.classify(shorter, myContext);
            return (myLiftBefore ? ContainerUtil.concat(lifted, singleton) : ContainerUtil.concat(singleton, lifted)).iterator();
          }
          return singleton.iterator();
        }

        @Nullable
        private List<LookupElement> addShorterElements(@Nullable Collection<LookupElement> from) {
          List<LookupElement> toLift = null;
          if (from == null) {
            return null;
          }

          if (arraysProcessed.add(from)) {
            for (LookupElement shorterElement : from) {
              if (mySrcSet.contains(shorterElement) && processed.add(shorterElement)) {
                if (toLift == null) {
                  toLift = new ArrayList<>();
                }
                toLift.add(shorterElement);
              }
            }

          }
          return toLift;
        }

      };
    }
  }

  @Nonnull
  private static <K, V> MultiMap<K, V> createMultiMap(final boolean identityKeys) {
    return new MultiMap<K, V>(identityKeys ? newIdentityHashMap() : new HashMap<>()) {

      @Override
      public boolean remove(K key, V value) {
        List<V> elements = (List<V>)get(key);
        int i = ContainerUtil.indexOfIdentity(elements, value);
        if (i >= 0) {
          elements.remove(i);
          if (elements.isEmpty()) {
            remove(key);
          }
          return true;
        }
        return false;
      }

    };
  }
}
