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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import org.jetbrains.annotations.NonNls;

public class ShowReformatFileDialog extends AnAction implements DumbAware {
  private static final @NonNls String HELP_ID = "editing.codeReformatting";

  @Override
  @RequiredReadAction
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    Editor editor = dataContext.getData(Editor.KEY);
    if (project == null || editor == null) {
      presentation.setEnabled(false);
      return;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null || file.getVirtualFile() == null) {
      presentation.setEnabled(false);
      return;
    }

    if (FormattingModelBuilder.forContext(file) != null) {
      presentation.setEnabled(true);
    }
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    Editor editor = dataContext.getData(Editor.KEY);
    if (project == null || editor == null) {
      presentation.setEnabled(false);
      return;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null || file.getVirtualFile() == null) {
      presentation.setEnabled(false);
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
