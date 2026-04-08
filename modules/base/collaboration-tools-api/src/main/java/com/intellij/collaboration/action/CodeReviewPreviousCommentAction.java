// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.action;

import com.intellij.collaboration.ui.codereview.editor.CodeReviewNavigableEditorViewModel;
import consulo.collaboration.localize.CollaborationToolsLocalize;

final class CodeReviewPreviousCommentAction extends CodeReviewNextPreviousCommentAction {
    CodeReviewPreviousCommentAction() {
        super(
            CollaborationToolsLocalize.actionCodereviewPreviouscommentText().get(),
            CollaborationToolsLocalize.actionCodereviewPreviouscommentDescription().get(),
            CodeReviewNavigableEditorViewModel::canGotoPreviousComment,
            CodeReviewNavigableEditorViewModel::canGotoPreviousComment,
            CodeReviewNavigableEditorViewModel::gotoPreviousComment,
            CodeReviewNavigableEditorViewModel::gotoPreviousComment
        );
    }
}
