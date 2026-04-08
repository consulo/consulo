// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.diff.util.LineRange;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nullable;

public interface CodeReviewInlayModel extends EditorMappedViewModel {
    Object getKey();

    @Override
    StateFlow<Integer> getLine();

    @Override
    StateFlow<Boolean> getIsVisible();

    interface Ranged extends CodeReviewInlayModel {
        StateFlow<LineRange> getRange();

        interface Adjustable extends Ranged {
            StateFlow<AdjustmentDisabledReason> getAdjustmentDisabledReason();

            void adjustRange(@Nullable Integer newStart, @Nullable Integer newEnd);

            default void adjustRange(@Nullable Integer newStart) {
                adjustRange(newStart, null);
            }

            enum AdjustmentDisabledReason {
                SUGGESTED_CHANGE,
                SINGLE_COMMIT_REVIEW,
            }
        }
    }
}
