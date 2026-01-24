package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;

import java.util.*;
import java.util.stream.Collectors;

public class ChunkedResolver implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    Map<Integer, TraceElement> beforeIndex = info.getValuesOrderBefore();
    Map<Integer, TraceElement> afterIndex = info.getValuesOrderAfter();

    Map<Integer, List<Integer>> invertedOrder = new HashMap<>();
    Integer[] beforeTimes = beforeIndex.keySet().stream().sorted().toArray(Integer[]::new);
    Integer[] afterTimes = afterIndex.keySet().stream().sorted().toArray(Integer[]::new);

    int beforeIx = 0;
    for (Integer afterTime : afterTimes) {
      while (beforeIx < beforeTimes.length && beforeTimes[beforeIx] < afterTime) {
        invertedOrder.computeIfAbsent(afterTime, k -> new ArrayList<>()).add(beforeTimes[beforeIx]);
        beforeIx += 1;
      }
    }

    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();
    for (Map.Entry<Integer, TraceElement> entry : afterIndex.entrySet()) {
      Integer timeAfter = entry.getKey();
      TraceElement elementAfter = entry.getValue();

      List<Integer> before = invertedOrder.getOrDefault(timeAfter, Collections.emptyList());
      List<TraceElement> beforeElements = before.stream()
        .map(beforeIndex::get)
        .collect(Collectors.toList());

      for (TraceElement beforeElement : beforeElements) {
        direct.put(beforeElement, Collections.singletonList(elementAfter));
      }
      reverse.put(elementAfter, beforeElements);
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }
}
