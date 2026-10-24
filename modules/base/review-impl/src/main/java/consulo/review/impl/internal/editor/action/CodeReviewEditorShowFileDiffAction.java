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
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;

@ActionImpl(id = "Code.Review.Editor.Show.Diff", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)))
public class CodeReviewEditorShowFileDiffAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CodeReviewEditorShowFileDiffAction() {
        super(ReviewLocalize.reviewDiffActionShowText(), ReviewLocalize.reviewDiffActionShowDescription(), PlatformIconGroup.actionsDiff());
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        CodeReviewEditorGutterActionableChangesModel model =
            editor == null ? null : editor.getUserData(CodeReviewEditorGutterActionableChangesModel.KEY);
        Presentation presentation = e.getPresentation();
        presentation.setText(ReviewLocalize.reviewDiffActionShowText());
        presentation.setDescription(ReviewLocalize.reviewDiffActionShowDescription());
        presentation.setEnabledAndVisible(model != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        if (editor == null) {
            return;
        }
        CodeReviewEditorGutterActionableChangesModel model = editor.getUserData(CodeReviewEditorGutterActionableChangesModel.KEY);
        if (model == null) {
            return;
        }
        Caret caret = e.getData(Caret.KEY);
        model.showDiff(caret == null ? null : caret.getLogicalPosition().line);
    }
}
