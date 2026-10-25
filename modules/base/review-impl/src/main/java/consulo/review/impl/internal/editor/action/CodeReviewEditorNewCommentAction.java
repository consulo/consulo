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

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.review.editor.CodeReviewCommentableEditorModel;
import consulo.review.editor.CodeReviewEditorDocumentUtil;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewMultilineCommentableEditorModel;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

@ActionImpl(
    id = "Code.Review.Editor.New.Comment",
    parents = {
        @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)),
        @ActionParentRef(value = @ActionRef(type = CodeReviewDiffEditorPopupGroup.class), anchor = ActionRefAnchor.FIRST)
    }
)
public class CodeReviewEditorNewCommentAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public CodeReviewEditorNewCommentAction() {
        super(ReviewLocalize.reviewEditorActionAddCommentText(), ReviewLocalize.reviewEditorActionAddCommentDescription());
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.<Object, Boolean>apply(o -> {
            updateOnUIThread(e);
            return true;
        }));
    }

    @RequiredUIAccess
    private static void updateOnUIThread(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Editor editor = e.getData(Editor.KEY);
        CodeReviewCommentableEditorModel model = editor == null ? null : getModel(editor);
        if (editor == null || model == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setVisible(true);

        Caret caret = editor.getCaretModel().getCurrentCaret();
        if (model instanceof CodeReviewMultilineCommentableEditorModel multiline) {
            CodeReviewLineRange selectedRange = getSelectedLinesRange(editor, caret);
            if (selectedRange != null) {
                presentation.setText(ReviewLocalize.reviewEditorActionAddCommentMultilineText());
                presentation.setDescription(ReviewLocalize.reviewEditorActionAddCommentMultilineDescription());
                presentation.setEnabled(!CodeReviewEditorDocumentUtil.isLastBlankLine(editor.getDocument(), selectedRange.end())
                    && multiline.canCreateComment(selectedRange));
                return;
            }
        }

        int caretLine = caret.getLogicalPosition().line;
        presentation.setText(ReviewLocalize.reviewEditorActionAddCommentText());
        presentation.setDescription(ReviewLocalize.reviewEditorActionAddCommentDescription());
        presentation.setEnabled(caretLine >= 0
            && !CodeReviewEditorDocumentUtil.isLastBlankLine(editor.getDocument(), caretLine)
            && model.canCreateComment(caretLine));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        if (editor == null) {
            return;
        }
        CodeReviewCommentableEditorModel model = getModel(editor);
        if (model == null) {
            return;
        }
        Caret caret = editor.getCaretModel().getCurrentCaret();
        ScrollingModel scrollingModel = editor.getScrollingModel();

        if (model instanceof CodeReviewMultilineCommentableEditorModel multiline) {
            CodeReviewLineRange selectedRange = getSelectedLinesRange(editor, caret);
            if (selectedRange != null && multiline.canCreateComment(selectedRange)) {
                scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
                scrollingModel.runActionOnScrollingFinished(() -> multiline.requestNewComment(selectedRange));
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

    private static @Nullable CodeReviewCommentableEditorModel getModel(Editor editor) {
        return editor.getUserData(CodeReviewCommentableEditorModel.KEY);
    }

    private static @Nullable CodeReviewLineRange getSelectedLinesRange(Editor editor, Caret caret) {
        int start = caret.getSelectionStart();
        int end = caret.getSelectionEnd();
        if (start == end) {
            return null;
        }
        CodeReviewLineRange lines = new CodeReviewLineRange(
            editor.offsetToLogicalPosition(start).line,
            editor.offsetToLogicalPosition(end).line
        );
        return lines.start() == lines.end() ? null : lines;
    }
}
