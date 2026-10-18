// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * An editor model which can start a comment on several lines at once.
 */
public interface CodeReviewMultilineCommentableEditorModel extends CodeReviewCommentableEditorModel {
    @RequiredUIAccess
    boolean canCreateComment(CodeReviewLineRange lineRange);

    @RequiredUIAccess
    void requestNewComment(CodeReviewLineRange lineRange);
}
