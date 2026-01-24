// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.trace.Value;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class FilterResolver implements ValuesOrderResolver {
    @Override
    public @Nonnull Result resolve(@Nonnull TraceInfo info) {
        Map<Integer, TraceElement> before = info.getValuesOrderBefore();
        Map<Integer, TraceElement> after = info.getValuesOrderAfter();
        assert before.size() >= after.size();
        Map<TraceElement, List<TraceElement>> forward = new LinkedHashMap<>();
        Map<TraceElement, List<TraceElement>> backward = new LinkedHashMap<>();

        int[] beforeTimes = before.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] afterTimes = after.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();

        int beforeIndex = 0;
        for (int afterTime : afterTimes) {
            TraceElement afterElement = after.get(afterTime);
            Value afterValue = afterElement.getValue();
            while (beforeIndex < beforeTimes.length) {
                TraceElement beforeElement = before.get(beforeTimes[beforeIndex]);
                if (Objects.equals(beforeElement.getValue(), afterValue)) {
                    forward.put(beforeElement, Collections.singletonList(afterElement));
                    backward.put(afterElement, Collections.singletonList(beforeElement));
                    beforeIndex++;
                    break;
                }

                forward.put(beforeElement, Collections.emptyList());
                beforeIndex++;
            }
        }

        while (beforeIndex < beforeTimes.length) {
            int beforeTime = beforeTimes[beforeIndex];
            forward.put(before.get(beforeTime), Collections.emptyList());
            beforeIndex++;
        }

        return Result.of(forward, backward);
    }
}
