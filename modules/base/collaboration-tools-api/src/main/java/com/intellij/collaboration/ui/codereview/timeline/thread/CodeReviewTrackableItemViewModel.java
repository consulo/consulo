// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline.thread;

import jakarta.annotation.Nonnull;

@ApiStatus.Internal
public interface CodeReviewTrackableItemViewModel {
    /**
     * Identifier used to track the comment inside the IDE.
     * This does not necessarily match the actual GitLab/GitHub ID.
     * Do not use this for API calls.
     */
    @Nonnull
    String getTrackingId();

    DataKey<CodeReviewTrackableItemViewModel> TRACKABLE_ITEM_KEY = DataKey.create("CodeReview.TrackableItem");
}
