// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util;

import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;

import java.util.Collection;
import java.util.List;

public final class ExternalSystemTrustUtil {
    private ExternalSystemTrustUtil() {
    }

    /**
     * This is a simple, dependency-free counterpart of the icu-based joining, that can be used early in startup and from EDT.
     */
    public static LocalizeValue naturalJoinSystemIds(Collection<ProjectSystemId> systemIds) {
        List<String> names = systemIds.stream()
            .map(it -> it.getDisplayName().get())
            .distinct()
            .sorted(StringUtil::naturalCompare)
            .toList();
        return switch (names.size()) {
            case 0 -> LocalizeValue.empty();
            case 1 -> LocalizeValue.of(names.get(0));
            case 2 -> ExternalSystemLocalize.externalSystemIdsJoinTwo(names.get(0), names.get(1));
            default -> ExternalSystemLocalize.externalSystemIdsJoinMore(
                String.join(", ", names.subList(0, names.size() - 1)),
                names.get(names.size() - 1)
            );
        };
    }
}
