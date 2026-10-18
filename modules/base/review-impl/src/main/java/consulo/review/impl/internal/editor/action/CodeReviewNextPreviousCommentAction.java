// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.impl.internal.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.localize.LocalizeValue;
import consulo.review.editor.CodeReviewNavigableEditorViewModel;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Moves the caret to the next or previous discussed line of an editor with review controls, stepping from the caret
 * line.
 */
public abstract class CodeReviewNextPreviousCommentAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    private final BiPredicate<CodeReviewNavigableEditorViewModel, Integer> myCanGotoLineComment;
    private final BiConsumer<CodeReviewNavigableEditorViewModel, Integer> myGotoLineComment;

    protected CodeReviewNextPreviousCommentAction(LocalizeValue text,
                                                  LocalizeValue description,
                                                  Image icon,
                                                  BiPredicate<CodeReviewNavigableEditorViewModel, Integer> canGotoLineComment,
                                                  BiConsumer<CodeReviewNavigableEditorViewModel, Integer> gotoLineComment) {
        super(text, description, icon);
        myCanGotoLineComment = canGotoLineComment;
        myGotoLineComment = gotoLineComment;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.<Object, Boolean>apply(o -> {
            Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
            CodeReviewNavigableEditorViewModel editorModel = getEditorModel(editor);
            if (editor == null || editorModel == null || !editorModel.canNavigate()) {
                e.getPresentation().setEnabledAndVisible(false);
                return false;
            }

            // zero-index
            int editorLine = editor.getCaretModel().getLogicalPosition().line;
            boolean enabled = myCanGotoLineComment.test(editorModel, editorLine);
            e.getPresentation().setEnabled(enabled);
            return enabled;
        }));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        CodeReviewNavigableEditorViewModel editorModel = getEditorModel(editor);
        if (editor == null || editorModel == null || !editorModel.canNavigate()) {
            return;
        }

        // zero-index
        int editorLine = editor.getCaretModel().getLogicalPosition().line;
        myGotoLineComment.accept(editorModel, editorLine);
    }

    private static @Nullable CodeReviewNavigableEditorViewModel getEditorModel(@Nullable Editor editor) {
        return editor == null ? null : editor.getUserData(CodeReviewNavigableEditorViewModel.KEY);
    }
}
