// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.action;

import com.intellij.collaboration.ui.codereview.editor.CodeReviewNavigableEditorViewModel;
import com.intellij.collaboration.ui.codereview.timeline.thread.CodeReviewTrackableItemViewModel;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataManager;
import consulo.diff.DiffDataKeys;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

abstract class CodeReviewNextPreviousCommentAction extends DumbAwareAction {
    private final @Nonnull BiPredicate<CodeReviewNavigableEditorViewModel, String> canGotoThreadComment;
    private final @Nonnull BiPredicate<CodeReviewNavigableEditorViewModel, Integer> canGotoLineComment;
    private final @Nonnull BiConsumer<CodeReviewNavigableEditorViewModel, String> gotoThreadComment;
    private final @Nonnull BiConsumer<CodeReviewNavigableEditorViewModel, Integer> gotoLineComment;

    CodeReviewNextPreviousCommentAction(
        @Nonnull @NlsActions.ActionText String text,
        @Nonnull @NlsActions.ActionDescription String description,
        @Nonnull BiPredicate<CodeReviewNavigableEditorViewModel, String> canGotoThreadComment,
        @Nonnull BiPredicate<CodeReviewNavigableEditorViewModel, Integer> canGotoLineComment,
        @Nonnull BiConsumer<CodeReviewNavigableEditorViewModel, String> gotoThreadComment,
        @Nonnull BiConsumer<CodeReviewNavigableEditorViewModel, Integer> gotoLineComment
    ) {
        super(text, description, null);
        this.canGotoThreadComment = canGotoThreadComment;
        this.canGotoLineComment = canGotoLineComment;
        this.gotoThreadComment = gotoThreadComment;
        this.gotoLineComment = gotoLineComment;
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getData(DiffDataKeys.CURRENT_EDITOR);
        if (editor == null) {
            editor = e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
        }
        CodeReviewNavigableEditorViewModel editorModel = editor != null
            ? editor.getUserData(CodeReviewNavigableEditorViewModel.KEY)
            : null;
        if (editor == null || editorModel == null || !editorModel.getCanNavigate()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        String focused = findFocusedThreadId(project);
        if (focused != null) {
            e.getPresentation().setEnabled(canGotoThreadComment.test(editorModel, focused));
        }
        else {
            int editorLine = editor.getCaretModel().getLogicalPosition().line; // zero-index
            e.getPresentation().setEnabled(canGotoLineComment.test(editorModel, editorLine));
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getData(DiffDataKeys.CURRENT_EDITOR);
        if (editor == null) {
            editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        }
        CodeReviewNavigableEditorViewModel editorModel = editor != null
            ? editor.getUserData(CodeReviewNavigableEditorViewModel.KEY)
            : null;
        if (editor == null || editorModel == null || !editorModel.getCanNavigate()) {
            return;
        }

        String focused = findFocusedThreadId(project);
        if (focused != null) {
            gotoThreadComment.accept(editorModel, focused);
        }
        else {
            int editorLine = editor.getCaretModel().getLogicalPosition().line; // zero-index
            gotoLineComment.accept(editorModel, editorLine);
        }
    }

    @ApiStatus.Internal
    public static @Nullable String findFocusedThreadId(@Nonnull Project project) {
        Component focusedComponent = IdeFocusManager.getInstance(project).getFocusOwner();
        if (focusedComponent == null) {
            return null;
        }
        var focusedData = DataManager.getInstance().getDataContext(focusedComponent);
        CodeReviewTrackableItemViewModel trackable = focusedData.getData(CodeReviewTrackableItemViewModel.TRACKABLE_ITEM_KEY);
        return trackable != null ? trackable.getTrackingId() : null;
    }
}
