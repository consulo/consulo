/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "ShowReformatFileDialog")
public class ShowReformatFileDialog extends AnAction implements DumbAware {
    private static final String HELP_ID = "editing.codeReformatting";

    public ShowReformatFileDialog() {
        super(ActionLocalize.actionShowreformatfiledialogText());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null || file.getVirtualFile() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        if (FormattingModelBuilder.forContext(file) != null) {
            e.getPresentation().setEnabled(true);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getRequiredData(Editor.KEY);

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null || file.getVirtualFile() == null) {
            return;
        }

        boolean hasSelection = editor.getSelectionModel().hasSelection();
        LayoutCodeDialog dialog = new LayoutCodeDialog(project, file, hasSelection, HELP_ID);
        dialog.show();

        if (dialog.isOK()) {
            new FileInEditorProcessor(file, editor, dialog.getRunOptions()).processCode();
        }
    }
}
