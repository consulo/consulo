// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class MapResolver implements ValuesOrderResolver {
    @Override
    public @Nonnull Result resolve(@Nonnull TraceInfo info) {
        Map<Integer, TraceElement> before = info.getValuesOrderBefore();
        Map<Integer, TraceElement> after = info.getValuesOrderAfter();

        Iterator<TraceElement> leftIterator = before.values().iterator();
        Iterator<TraceElement> rightIterator = after.values().iterator();

        Map<TraceElement, List<TraceElement>> forward = new LinkedHashMap<>();
        Map<TraceElement, List<TraceElement>> backward = new LinkedHashMap<>();
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            TraceElement left = leftIterator.next();
            TraceElement right = rightIterator.next();

            forward.put(left, Collections.singletonList(right));
            backward.put(right, Collections.singletonList(left));
        }

        while (leftIterator.hasNext()) {
            forward.put(leftIterator.next(), Collections.emptyList());
        }

        return Result.of(forward, backward);
    }
}
