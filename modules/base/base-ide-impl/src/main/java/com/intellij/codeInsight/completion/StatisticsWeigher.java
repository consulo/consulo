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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.Classifier;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.statistics.StatisticsInfo;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.MultiMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

/**
 * @author peter
 */
public class StatisticsWeigher extends CompletionWeigher {
  private static final Logger LOG = Logger.getInstance(StatisticsWeigher.class);
  private static final Key<StatisticsInfo> BASE_STATISTICS_INFO = Key.create("Base statistics info");

  @Override
  public Comparable weigh(@Nonnull final LookupElement item, @Nonnull final CompletionLocation location) {
    throw new UnsupportedOperationException();
  }

  public static class LookupStatisticsWeigher extends Classifier<LookupElement> {
    private final CompletionLocation myLocation;
    private final Map<LookupElement, StatisticsComparable> myWeights = ContainerUtil.newIdentityHashMap();
    private final Set<String> myStringsWithWeights = ContainerUtil.newTroveSet();
    private final Set<LookupElement> myNoStats = ContainerUtil.newIdentityTroveSet();

    public LookupStatisticsWeigher(CompletionLocation location, Classifier<LookupElement> next) {
      super(next, "stats");
      myLocation = location;
    }

    @Override
    public void addElement(@Nonnull LookupElement element, @Nonnull ProcessingContext context) {
      StatisticsInfo baseInfo = getBaseStatisticsInfo(element, myLocation);
      int weight = weigh(baseInfo);
      if (weight != 0) {
        myWeights.put(element, new StatisticsComparable(weight, baseInfo));
        myStringsWithWeights.add(element.getLookupString());
      }
      if (baseInfo == StatisticsInfo.EMPTY) {
        myNoStats.add(element);
      }
      super.addElement(element, context);
    }

    @Nonnull
    @Override
    public Iterable<LookupElement> classify(@Nonnull Iterable<LookupElement> source, @Nonnull final ProcessingContext context) {
      List<LookupElement> initialList = getInitialNoStatElements(source, context);
      Iterable<LookupElement> rest = withoutInitial(source, initialList);
      Collection<List<LookupElement>> byWeight = buildMapByWeight(rest).descendingMap().values();

      return JBIterable.from(initialList).append(JBIterable.from(byWeight).flatten(group -> myNext.classify(group, context)));
    }

    private static Iterable<LookupElement> withoutInitial(Iterable<LookupElement> allItems, List<LookupElement> initial) {
      Set<LookupElement> initialSet = ContainerUtil.newIdentityTroveSet(initial);
      return JBIterable.from(allItems).filter(element -> !initialSet.contains(element));
    }

    private List<LookupElement> getInitialNoStatElements(Iterable<LookupElement> source, ProcessingContext context) {
      List<LookupElement> initialList = new ArrayList<>();
      for (LookupElement next : myNext.classify(source, context)) {
        if (myNoStats.contains(next)) {
          initialList.add(next);
        }
        else {
          break;
        }
      }
      return initialList;
    }

    private TreeMap<Integer, List<LookupElement>> buildMapByWeight(Iterable<LookupElement> source) {
      MultiMap<String, LookupElement> byName = MultiMap.create();
      List<LookupElement> noStats = new ArrayList<>();
      for (LookupElement element : source) {
        String string = element.getLookupString();
        if (myStringsWithWeights.contains(string)) {
          byName.putValue(string, element);
        } else {
          noStats.add(element);
        }
      }

      TreeMap<Integer, List<LookupElement>> map = new TreeMap<>();
      map.put(0, noStats);
      for (String s : byName.keySet()) {
        List<LookupElement> group = (List<LookupElement>)byName.get(s);
        Collections.sort(group, Comparator.comparing(this::getScalarWeight).reversed());
        map.computeIfAbsent(getMaxWeight(group), __ -> new ArrayList<>()).addAll(group);
      }
      return map;
    }

    private int getMaxWeight(List<LookupElement> group) {
      int max = 0;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < group.size(); i++) {
        max = Math.max(max, getScalarWeight(group.get(i)));
      }
      return max;
    }

    private int getScalarWeight(LookupElement e) {
      StatisticsComparable comparable = myWeights.get(e);
      return comparable == null ? 0 : comparable.getScalar();
    }

    private StatisticsComparable getWeight(LookupElement t) {
      StatisticsComparable w = myWeights.get(t);
      if (w == null) {
        StatisticsInfo info = getBaseStatisticsInfo(t, myLocation);
        myWeights.put(t, w = new StatisticsComparable(weigh(info), info));
      }
      return w;
    }

    private static int weigh(final StatisticsInfo baseInfo) {
      if (baseInfo == StatisticsInfo.EMPTY) {
        return 0;
      }
      int minRecency = baseInfo.getLastUseRecency();
      return minRecency == Integer.MAX_VALUE ? 0 : StatisticsManager.RECENCY_OBLIVION_THRESHOLD - minRecency;
    }

    @Nonnull
    @Override
    public List<Pair<LookupElement, Object>> getSortingWeights(@Nonnull Iterable<LookupElement> items, @Nonnull final ProcessingContext context) {
      return ContainerUtil.map(items, lookupElement -> new Pair<LookupElement, Object>(lookupElement, getWeight(lookupElement)));
    }

    @Override
    public void removeElement(@Nonnull LookupElement element, @Nonnull ProcessingContext context) {
      myWeights.remove(element);
      myNoStats.remove(element);
      super.removeElement(element, context);
    }
  }

  public static void clearBaseStatisticsInfo(LookupElement item) {
    item.putUserData(BASE_STATISTICS_INFO, null);
  }

  @Nonnull
  public static StatisticsInfo getBaseStatisticsInfo(LookupElement item, @Nullable CompletionLocation location) {
    StatisticsInfo info = BASE_STATISTICS_INFO.get(item);
    if (info == null) {
      if (location == null) {
        return StatisticsInfo.EMPTY;
      }
      BASE_STATISTICS_INFO.set(item, info = calcBaseInfo(item, location));
    }
    return info;
  }

  @Nonnull
  private static StatisticsInfo calcBaseInfo(LookupElement item, @Nonnull CompletionLocation location) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
    }
    StatisticsInfo info = StatisticsManager.serialize(CompletionService.STATISTICS_KEY, item, location);
    return info == null ? StatisticsInfo.EMPTY : info;
  }

}
