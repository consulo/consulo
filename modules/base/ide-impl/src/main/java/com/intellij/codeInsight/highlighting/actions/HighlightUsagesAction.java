/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.codeInsight.highlighting.actions;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.DumbService;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class HighlightUsagesAction extends AnAction implements DumbAware {
  public HighlightUsagesAction() {
    setInjectedContext(true);
  }

  @Override
  public void update(final AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    final DataContext dataContext = event.getDataContext();
    presentation.setEnabled(dataContext.getData(CommonDataKeys.PROJECT) != null &&
                            dataContext.getData(PlatformDataKeys.EDITOR) != null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getDataContext().getData(PlatformDataKeys.EDITOR);
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (editor == null || project == null) return;
    String commandName = getTemplatePresentation().getText();
    if (commandName == null) commandName = "";

    CommandProcessor.getInstance().executeCommand(
      project,
      new Runnable() {
        @Override
        @RequiredUIAccess
        public void run() {
          PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
          try {
            HighlightUsagesHandler.invoke(project, editor, psiFile);
          }
          catch (IndexNotReadyException ex) {
            DumbService.getInstance(project).showDumbModeNotification(ActionsBundle.message("action.HighlightUsagesInFile.not.ready"));
          }
        }
      },
      commandName,
      null
    );
  }
}
