// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class IntervalMapResolver extends PartialReductionResolverBase {
  @Override
  protected Result buildResult(Map<TraceElement, List<TraceElement>> mapping) {
    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

    List<TraceElement> sortedAfter = mapping.keySet().stream()
      .sorted(Comparator.comparingInt(TraceElement::getTime))
      .collect(Collectors.toList());

    for (TraceElement valueAfter : sortedAfter) {
      List<TraceElement> valuesBefore = mapping.get(valueAfter).stream()
        .sorted(Comparator.comparingInt(TraceElement::getTime))
        .collect(Collectors.toList());

      List<TraceElement> reverseMapping = new ArrayList<>();
      if (!valuesBefore.isEmpty()) {
        TraceElement first = valuesBefore.get(0);
        direct.put(first, Collections.singletonList(valueAfter));
        reverseMapping.add(first);

        if (valuesBefore.size() > 1) {
          TraceElement last = valuesBefore.get(valuesBefore.size() - 1);
          direct.put(last, Collections.singletonList(valueAfter));
          reverseMapping.add(last);
        }
      }

      reverse.put(valueAfter, reverseMapping);
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }
}
