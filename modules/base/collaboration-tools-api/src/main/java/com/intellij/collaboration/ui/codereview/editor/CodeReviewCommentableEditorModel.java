// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.diff.util.LineRange;
import consulo.util.dataholder.Key;

public interface CodeReviewCommentableEditorModel {
    @RequiresEdt
    boolean canCreateComment(int lineIdx);

    @RequiresEdt
    void requestNewComment(int lineIdx);

    @RequiresEdt
    default void cancelNewComment(int lineIdx) {
    }

    interface WithMultilineComments extends CodeReviewCommentableEditorModel {
        @RequiresEdt
        boolean canCreateComment(LineRange lineRange);

        @RequiresEdt
        void requestNewComment(LineRange lineRange);
    }

    Key<CodeReviewEditorGutterControlsModel> KEY = Key.create(CodeReviewCommentableEditorModel.class.getName());
}
