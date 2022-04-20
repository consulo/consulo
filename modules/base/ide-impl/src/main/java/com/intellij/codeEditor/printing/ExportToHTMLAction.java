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

package com.intellij.codeEditor.printing;

import consulo.application.CommonBundle;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.project.Project;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import javax.swing.*;
import java.io.FileNotFoundException;

public class ExportToHTMLAction extends AnAction {

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    try {
      ExportToHTMLManager.executeExport(dataContext);
    }
    catch (FileNotFoundException ex) {
      JOptionPane.showMessageDialog(null, CodeEditorBundle.message("file.not.found", ex.getMessage()), CommonBundle.getErrorTitle(), JOptionPane.ERROR_MESSAGE);
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    PsiElement psiElement = dataContext.getData(LangDataKeys.PSI_ELEMENT);
    if (psiElement instanceof PsiDirectory) {
      presentation.setEnabled(true);
      return;
    }
    PsiFile psiFile = dataContext.getData(LangDataKeys.PSI_FILE);
    presentation.setEnabled(psiFile != null && psiFile.getContainingDirectory() != null);
  }
}