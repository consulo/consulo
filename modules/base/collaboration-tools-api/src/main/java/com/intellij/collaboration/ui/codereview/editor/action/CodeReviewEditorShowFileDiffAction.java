// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public final class CodeReviewEditorShowFileDiffAction extends DumbAwareAction {
    public CodeReviewEditorShowFileDiffAction() {
        super(CollaborationToolsLocalize.reviewDiffActionShowText());
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        CodeReviewEditorGutterActionableChangesModel model =
            editor != null ? editor.getUserData(CodeReviewEditorGutterActionableChangesModel.KEY) : null;
        e.getPresentation().setTextValue(CollaborationToolsLocalize.reviewDiffActionShowText());
        e.getPresentation().setDescriptionValue(CollaborationToolsLocalize.reviewDiffActionShowDescription());
        e.getPresentation().setEnabledAndVisible(model != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        if (editor == null) {
            return;
        }
        CodeReviewEditorGutterActionableChangesModel model = editor.getUserData(CodeReviewEditorGutterActionableChangesModel.KEY);
        if (model == null) {
            return;
        }
        var caret = e.getData(Caret.KEY);
        Integer line = caret != null ? caret.getLogicalPosition().line : null;
        model.showDiff(line);
    }
}
