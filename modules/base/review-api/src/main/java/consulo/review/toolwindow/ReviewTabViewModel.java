// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import consulo.localize.LocalizeValue;

/**
 * View model of the review toolwindow tab content
 */
public interface ReviewTabViewModel {
    /**
     * Toolwindow tab title
     */
    LocalizeValue getDisplayName();

    /**
     * Toolwindow tab tooltip
     */
    default LocalizeValue getDescription() {
        return getDisplayName();
    }
}
