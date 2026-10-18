// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.ui.annotation.RequiredUIAccess;

import java.util.Set;

public interface CodeReviewEditorGutterControlsState {
    @RequiredUIAccess
    Set<Integer> getLinesWithComments();

    @RequiredUIAccess
    default Set<Integer> getLinesWithNewComments() {
        return Set.of();
    }

    @RequiredUIAccess
    boolean isLineCommentable(int lineIdx);
}
