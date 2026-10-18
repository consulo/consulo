// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * A UI model for an editor gutter with review controls
 * <p>
 * This model should exist in the same scope as the gutter
 * <p>
 * One model - one gutter
 */
public interface CodeReviewEditorGutterControlsModel extends CodeReviewCommentableEditorModel {
    ObservableValue<@Nullable CodeReviewEditorGutterControlsState> getGutterControlsState();

    @RequiredUIAccess
    @Override
    default boolean canCreateComment(int lineIdx) {
        CodeReviewEditorGutterControlsState state = getGutterControlsState().get();
        return state != null && state.isLineCommentable(lineIdx);
    }

    @RequiredUIAccess
    void toggleComments(int lineIdx);
}
