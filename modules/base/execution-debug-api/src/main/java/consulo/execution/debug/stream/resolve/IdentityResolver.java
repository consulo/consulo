// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.TraceUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class IdentityResolver implements ValuesOrderResolver {
    private static final Object NULL_MARKER = ObjectUtil.sentinel("IdentityResolver.NULL_MARKER");

    @Override
    public @Nonnull Result resolve(@Nonnull TraceInfo info) {
        Map<Integer, TraceElement> before = info.getValuesOrderBefore();
        Map<Integer, TraceElement> after = info.getValuesOrderAfter();

        Map<TraceElement, List<TraceElement>> direct = new HashMap<>();
        Map<TraceElement, List<TraceElement>> reverse = new HashMap<>();

        Map<Object, List<TraceElement>> grouped = after.keySet()
            .stream()
            .sorted()
            .map(after::get)
            .collect(Collectors.groupingBy(IdentityResolver::extractKey));

        Map<Object, Integer> key2Index = new HashMap<>();

        for (TraceElement element : before.values()) {
            Object key = extractKey(element);

            List<TraceElement> elements = grouped.get(key);
            if (elements == null || elements.isEmpty()) {
                direct.put(element, Collections.emptyList());
                continue;
            }

            int nextIndex = key2Index.getOrDefault(key, -1) + 1;
            key2Index.put(key, nextIndex);
            TraceElement afterItem = elements.get(nextIndex);

            direct.put(element, Collections.singletonList(afterItem));
            reverse.put(afterItem, Collections.singletonList(element));
        }

        return Result.of(direct, reverse);
    }

    private static @Nonnull Object extractKey(@Nonnull TraceElement element) {
        Object key = TraceUtil.extractKey(element);
        return key == null ? NULL_MARKER : key;
    }
}
