// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.TraceUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * @author Vitaliy.Bibaev
 */
public class AppendResolver implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

    List<TraceElement> valuesBefore = TraceUtil.sortedByTime(info.getValuesOrderBefore().values());
    List<TraceElement> valuesAfter = TraceUtil.sortedByTime(info.getValuesOrderAfter().values());

    int size = Math.min(valuesBefore.size(), valuesAfter.size());
    for (int i = 0; i < size; i++) {
      TraceElement before = valuesBefore.get(i);
      TraceElement after = valuesAfter.get(i);

      List<TraceElement> afterList = new ArrayList<>();
      afterList.add(after);
      direct.put(before, afterList);

      List<TraceElement> beforeList = new ArrayList<>();
      beforeList.add(before);
      reverse.put(after, beforeList);
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }
}
