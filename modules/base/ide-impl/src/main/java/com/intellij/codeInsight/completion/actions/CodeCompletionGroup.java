/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.completion.actions;

import com.intellij.openapi.actionSystem.*;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class CodeCompletionGroup extends DefaultActionGroup implements DumbAware {

  @Override
  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null){
      presentation.setEnabled(false);
      return;
    }

    Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    if (editor == null){
      presentation.setEnabled(false);
      return;
    }
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null){
      presentation.setEnabled(false);
      return;
    }

    presentation.setEnabled(true);
  }

  @Override
  public boolean disableIfNoVisibleChildren() {
    return false;
  }
}
