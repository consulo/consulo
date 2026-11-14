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
package consulo.ai.editor.impl.internal.plan;

import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.project.Project;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.icon.ReviewIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.virtualFileSystem.VirtualFile;

/**
 * A review comment on the selected lines of a plan an agent wrote - the comment box of the review opens under them.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ActionImpl(id = CommentOnPlanAction.ID, parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)))
public class CommentOnPlanAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public static final String ID = "AI.CommentOnPlan";

    public CommentOnPlanAction() {
        super(AIEditorLocalize.actionCommentOnPlanText(), AIEditorLocalize.actionCommentOnPlanDescription(), ReviewIconGroup.comment());
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        VirtualFile file = editor == null ? null : editor.getVirtualFile();
        e.getPresentation().setEnabledAndVisible(project != null && file != null
            && AIPlanReviewManager.getInstance(project).hasReview(file));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        VirtualFile file = editor == null ? null : editor.getVirtualFile();
        if (project == null || editor == null || file == null) {
            return;
        }
        Document document = editor.getDocument();
        SelectionModel selection = editor.getSelectionModel();
        int start = selection.hasSelection() ? selection.getSelectionStart() : editor.getCaretModel().getOffset();
        int end = selection.hasSelection() ? Math.max(start, selection.getSelectionEnd() - 1) : start;
        CodeReviewLineRange lines = new CodeReviewLineRange(document.getLineNumber(start), document.getLineNumber(end));
        selection.removeSelection();
        AIPlanReviewManager.getInstance(project).requestComment(file, lines);
    }
}
