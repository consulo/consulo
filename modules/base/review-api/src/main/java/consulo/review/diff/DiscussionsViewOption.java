// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.diff;

import consulo.localize.LocalizeValue;
import consulo.review.localize.ReviewLocalize;

public enum DiscussionsViewOption {
    ALL,
    UNRESOLVED_ONLY,
    DONT_SHOW;

    public LocalizeValue toActionName() {
        return switch (this) {
            case ALL -> ReviewLocalize.reviewDiffDiscussionsViewOptionAll();
            case UNRESOLVED_ONLY -> ReviewLocalize.reviewDiffDiscussionsViewOptionUnresolvedOnly();
            case DONT_SHOW -> ReviewLocalize.reviewDiffDiscussionsViewOptionDoNotShow();
        };
    }
}
