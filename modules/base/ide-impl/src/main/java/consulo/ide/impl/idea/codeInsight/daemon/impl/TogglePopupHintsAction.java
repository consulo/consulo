/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

public class TogglePopupHintsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(TogglePopupHintsAction.class);

  @RequiredReadAction
  private static PsiFile getTargetFile(@Nonnull DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return null;
    }
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    if (files.length == 0) {
      return null;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(files[0]);
    LOG.assertTrue(psiFile != null);
    return psiFile;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    PsiFile psiFile = ReadAction.compute(() -> getTargetFile(e.getDataContext()));
    e.getPresentation().setEnabled(psiFile != null);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    PsiFile psiFile = getTargetFile(e.getDataContext());
    LOG.assertTrue(psiFile != null);
    Project project = e.getRequiredData(Project.KEY);
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
    codeAnalyzer.setImportHintsEnabled(psiFile, !codeAnalyzer.isImportHintsEnabled(psiFile));
  }
}
