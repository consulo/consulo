// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * A ranged inlay whose lines can be changed by dragging the edges of its frame.
 */
public interface CodeReviewAdjustableInlayModel extends CodeReviewRangedInlayModel {
    ObservableValue<@Nullable CodeReviewInlayAdjustmentDisabledReason> getAdjustmentDisabledReason();

    void adjustRange(@Nullable Integer newStart, @Nullable Integer newEnd);
}
