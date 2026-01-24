// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class FilteredMapResolver implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    Map<Integer, TraceElement> before = info.getValuesOrderBefore();
    Map<Integer, TraceElement> after = info.getValuesOrderAfter();

    Map<Integer, Integer> invertedOrder = new HashMap<>();
    int[] beforeTimes = before.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
    int[] afterTimes = after.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();

    int beforeIndex = 0;
    for (int afterTime : afterTimes) {
      while (beforeIndex < beforeTimes.length && afterTime > beforeTimes[beforeIndex]) {
        beforeIndex += 1;
      }
      int beforeTime = beforeTimes[beforeIndex - 1];
      if (beforeTime < afterTime) {
        invertedOrder.put(afterTime, beforeTime);
      }
    }

    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

    for (Map.Entry<Integer, Integer> entry : invertedOrder.entrySet()) {
      int afterTime = entry.getKey();
      int beforeTime = entry.getValue();

      TraceElement beforeElement = before.get(beforeTime);
      TraceElement afterElement = after.get(afterTime);
      direct.put(beforeElement, Collections.singletonList(afterElement));
      reverse.put(afterElement, Collections.singletonList(beforeElement));
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }
}
