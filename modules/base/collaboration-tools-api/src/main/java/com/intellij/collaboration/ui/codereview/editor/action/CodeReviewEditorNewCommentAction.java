// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import com.intellij.collaboration.ui.codereview.editor.CodeReviewCommentableEditorModel;
import consulo.codeEditor.ScrollType;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.diff.util.LineRange;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

final class CodeReviewEditorNewCommentAction extends DumbAwareAction {
    CodeReviewEditorNewCommentAction() {
        super(
            CollaborationToolsLocalize.reviewEditorActionAddCommentText(),
            CollaborationToolsLocalize.reviewEditorActionAddCommentDescription()
        );
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        EditorEx editor = ObjectUtils.tryCast(e.getData(Editor.KEY), EditorEx.class);
        if (editor == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        CodeReviewCommentableEditorModel model = editor.getUserData(CodeReviewCommentableEditorModel.KEY);
        if (model == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);

        Caret caret = editor.getCaretModel().getCurrentCaret();
        if (model instanceof CodeReviewCommentableEditorModel.WithMultilineComments multilineModel) {
            LineRange selectedRange = getSelectedLinesRange(caret);
            if (selectedRange != null && !selectedRange.isEmpty()) {
                e.getPresentation().setTextValue(CollaborationToolsLocalize.reviewEditorActionAddCommentMultilineText());
                e.getPresentation()
                    .setDescriptionValue(CollaborationToolsLocalize.reviewEditorActionAddCommentMultilineDescription());
                e.getPresentation().setEnabled(multilineModel.canCreateComment(selectedRange));
                return;
            }
        }

        int caretLine = caret.getLogicalPosition().line;
        if (caretLine < 0) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setTextValue(CollaborationToolsLocalize.reviewEditorActionAddCommentText());
        e.getPresentation().setDescriptionValue(CollaborationToolsLocalize.reviewEditorActionAddCommentDescription());
        e.getPresentation().setEnabled(model.canCreateComment(caretLine));
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        EditorEx editor = ObjectUtils.tryCast(e.getData(Editor.KEY), EditorEx.class);
        if (editor == null) {
            return;
        }
        CodeReviewCommentableEditorModel model = editor.getUserData(CodeReviewCommentableEditorModel.KEY);
        if (model == null) {
            return;
        }
        Caret caret = editor.getCaretModel().getCurrentCaret();
        var scrollingModel = editor.getScrollingModel();

        if (model instanceof CodeReviewCommentableEditorModel.WithMultilineComments multilineModel) {
            LineRange selectedRange = getSelectedLinesRange(caret);
            if (selectedRange != null && !selectedRange.isEmpty()) {
                scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
                scrollingModel.runActionOnScrollingFinished(() -> multilineModel.requestNewComment(selectedRange));
                return;
            }
        }

        int caretLine = caret.getLogicalPosition().line;
        if (caretLine < 0) {
            return;
        }
        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
        scrollingModel.runActionOnScrollingFinished(() -> model.requestNewComment(caretLine));
    }

    private static @Nullable LineRange getSelectedLinesRange(@Nonnull Caret caret) {
        var selectionRange = caret.getSelectionRange();
        if (selectionRange.getLength() == 0) {
            return null;
        }
        return new LineRange(
            caret.getEditor().offsetToLogicalPosition(selectionRange.getStartOffset()).line,
            caret.getEditor().offsetToLogicalPosition(selectionRange.getEndOffset()).line
        );
    }
}
