// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

public interface CodeReviewCommentableEditorModel {
    Key<CodeReviewEditorGutterControlsModel> KEY = Key.create(CodeReviewCommentableEditorModel.class.getName());

    @RequiredUIAccess
    boolean canCreateComment(int lineIdx);

    @RequiredUIAccess
    void requestNewComment(int lineIdx);

    @RequiredUIAccess
    default void cancelNewComment(int lineIdx) {
    }
}
