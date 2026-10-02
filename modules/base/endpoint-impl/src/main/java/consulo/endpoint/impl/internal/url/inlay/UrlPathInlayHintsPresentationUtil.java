// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.endpoint.url.inlay.ProviderGroupKey;
import consulo.endpoint.url.inlay.UrlPathInlayHintsProviderSemElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UrlPathInlayHintsPresentationUtil {
    private static final Comparator<UrlPathInlayHintsProviderSemElement> PRIORITY_COMPARATOR =
        Comparator.comparingInt(it -> it.getGroupInfo().getPriority());

    private UrlPathInlayHintsPresentationUtil() {
    }

    public static List<UrlPathInlayHintsProviderSemElement> selectProvidersFromGroups(
        List<UrlPathInlayHintsProviderSemElement> providers
    ) {
        Map<ProviderGroupKey, List<UrlPathInlayHintsProviderSemElement>> groups = new LinkedHashMap<>();
        for (UrlPathInlayHintsProviderSemElement element : providers) {
            ProviderGroupKey key = element.getGroupInfo().getKey();
            List<UrlPathInlayHintsProviderSemElement> accumulator = groups.get(key);
            if (key.equals(ProviderGroupKey.DEFAULT_KEY)) {
                if (accumulator != null) {
                    accumulator.add(element);
                }
                else {
                    List<UrlPathInlayHintsProviderSemElement> list = new ArrayList<>();
                    list.add(element);
                    groups.put(key, list);
                }
            }
            else {
                UrlPathInlayHintsProviderSemElement single = accumulator != null && accumulator.size() == 1 ? accumulator.get(0) : null;
                List<UrlPathInlayHintsProviderSemElement> list = new ArrayList<>();
                list.add(single != null ? maxOf(single, element) : element);
                groups.put(key, list);
            }
        }

        List<UrlPathInlayHintsProviderSemElement> result = new ArrayList<>();
        for (List<UrlPathInlayHintsProviderSemElement> group : groups.values()) {
            result.addAll(group);
        }
        return result;
    }

    private static UrlPathInlayHintsProviderSemElement maxOf(UrlPathInlayHintsProviderSemElement a, UrlPathInlayHintsProviderSemElement b) {
        return PRIORITY_COMPARATOR.compare(a, b) >= 0 ? a : b;
    }
}
