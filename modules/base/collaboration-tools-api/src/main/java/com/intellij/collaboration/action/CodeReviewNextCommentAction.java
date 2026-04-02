// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.action;

import com.intellij.collaboration.ui.codereview.editor.CodeReviewNavigableEditorViewModel;
import consulo.collaboration.localize.CollaborationToolsLocalize;

final class CodeReviewNextCommentAction extends CodeReviewNextPreviousCommentAction {
    CodeReviewNextCommentAction() {
        super(
            CollaborationToolsLocalize.actionCodereviewNextcommentText().get(),
            CollaborationToolsLocalize.actionCodereviewNextcommentDescription().get(),
            CodeReviewNavigableEditorViewModel::canGotoNextComment,
            CodeReviewNavigableEditorViewModel::canGotoNextComment,
            CodeReviewNavigableEditorViewModel::gotoNextComment,
            CodeReviewNavigableEditorViewModel::gotoNextComment
        );
    }
}
