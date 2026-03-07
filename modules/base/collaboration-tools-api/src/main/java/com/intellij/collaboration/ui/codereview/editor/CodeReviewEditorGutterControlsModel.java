// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * A UI model for an editor gutter with review controls
 * This model should exist in the same scope as the gutter
 * One model - one gutter
 */
public interface CodeReviewEditorGutterControlsModel extends CodeReviewCommentableEditorModel {
    StateFlow<ControlsState> getGutterControlsState();

    @RequiresEdt
    @Override
    default boolean canCreateComment(int lineIdx) {
        ControlsState state = getGutterControlsState().getValue();
        return state != null && state.isLineCommentable(lineIdx);
    }

    @RequiresEdt
    void toggleComments(int lineIdx);

    interface ControlsState {
        @RequiresEdt
        Set<Integer> getLinesWithComments();

        @RequiresEdt
        default Set<Integer> getLinesWithNewComments() {
            return Set.of();
        }

        @RequiresEdt
        boolean isLineCommentable(int lineIdx);
    }
}
