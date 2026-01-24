// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.TraceUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class PrependResolver implements ValuesOrderResolver {
  @Nonnull
  @Override
  public Result resolve(TraceInfo info) {
    Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
    Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

    List<TraceElement> valuesBefore = TraceUtil.sortedByTime(info.getValuesOrderBefore().values());
    List<TraceElement> valuesAfter = TraceUtil.sortedByTime(info.getValuesOrderAfter().values());

    if (!valuesBefore.isEmpty()) {
      TraceElement firstBefore = valuesBefore.get(0);
      int indexOfFirstItemFromSource = -1;
      for (int i = 0; i < valuesAfter.size(); i++) {
        if (valuesAfter.get(i).getTime() > firstBefore.getTime()) {
          indexOfFirstItemFromSource = i;
          break;
        }
      }

      if (indexOfFirstItemFromSource != -1) {
        List<TraceElement> afterSubList = valuesAfter.subList(indexOfFirstItemFromSource, valuesAfter.size());
        int size = Math.min(valuesBefore.size(), afterSubList.size());
        for (int i = 0; i < size; i++) {
          TraceElement before = valuesBefore.get(i);
          TraceElement after = afterSubList.get(i);

          direct.put(before, Collections.singletonList(after));
          reverse.put(after, Collections.singletonList(before));
        }
      }
    }

    return ValuesOrderResolver.Result.of(direct, reverse);
  }
}
