// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.timeline.thread;

import consulo.util.dataholder.Key;

public interface CodeReviewTrackableItemViewModel {
    Key<CodeReviewTrackableItemViewModel> TRACKABLE_ITEM_KEY = Key.create("CodeReview.TrackableItem");

    /**
     * Identifier used to track the comment inside the IDE.
     * This does not necessary match the actual GitLab/GitHub ID.
     * Do not use this for API calls.
     */
    String getTrackingId();
}
