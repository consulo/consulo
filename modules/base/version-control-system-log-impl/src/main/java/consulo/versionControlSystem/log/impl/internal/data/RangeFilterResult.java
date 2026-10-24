// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log.impl.internal.data;

import java.util.Set;

sealed interface RangeFilterResult {
    record Commits(Set<Integer> commits) implements RangeFilterResult {
    }

    record InvalidRange() implements RangeFilterResult {
    }

    record Error() implements RangeFilterResult {
    }
}
