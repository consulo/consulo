// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.TraceUtil;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class PartialReductionResolverBase implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    List<TraceElement> valuesBefore = TraceUtil.sortedByTime(info.getValuesOrderBefore().values());
    List<TraceElement> valuesAfter = TraceUtil.sortedByTime(info.getValuesOrderAfter().values());

    Map<TraceElement, List<TraceElement>> reverseMapping = new HashMap<>();
    int i = 0;
    for (TraceElement valueAfter : valuesAfter) {
      List<TraceElement> reverseList = new ArrayList<>();
      while (i + 1 < valuesBefore.size() && valuesBefore.get(i + 1).getTime() < valueAfter.getTime()) {
        reverseList.add(valuesBefore.get(i));
        i++;
      }

      reverseMapping.put(valueAfter, reverseList);
    }

    if (!valuesAfter.isEmpty() && !valuesBefore.isEmpty()) {
      reverseMapping.get(valuesAfter.get(valuesAfter.size() - 1)).add(valuesBefore.get(valuesBefore.size() - 1));
    }

    return buildResult(reverseMapping);
  }

  protected abstract Result buildResult(Map<TraceElement, List<TraceElement>> mapping);
}
