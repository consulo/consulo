// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.TraceUtil;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class PairMapResolver implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

    List<TraceElement> valuesBefore = TraceUtil.sortedByTime(info.getValuesOrderBefore().values());
    List<TraceElement> valuesAfter = TraceUtil.sortedByTime(info.getValuesOrderAfter().values());

    Iterator<TraceElement> beforeIterator = valuesBefore.iterator();
    Iterator<TraceElement> afterIterator = valuesAfter.iterator();

    TraceElement after = null;
    while (beforeIterator.hasNext()) {
      TraceElement before = beforeIterator.next();
      if (after != null) {
        add(direct, before, after);
        add(reverse, after, before);
      }
      if (afterIterator.hasNext()) {
        after = afterIterator.next();
        add(direct, before, after);
        add(reverse, after, before);
      }
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }

  private void add(Map<TraceElement, List<TraceElement>> map, TraceElement key, TraceElement value) {
    map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }
}
