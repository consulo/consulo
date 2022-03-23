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
package com.intellij.compiler.actions;

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.*;
import consulo.compiler.CompilerManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.UpdateInBackground;

import javax.annotation.Nonnull;

public abstract class CompileActionBase extends AnAction implements DumbAware, UpdateInBackground {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
    if (file != null && editor != null && !DumbService.getInstance(project).isDumb()) {
      DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, file); //let autoimport complete
    }
    doAction(dataContext, project);
  }

  @RequiredUIAccess
  protected abstract void doAction(final DataContext dataContext, final Project project);

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    super.update(e);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || !project.isInitialized()) {
      e.getPresentation().setEnabled(false);
    }
    else {
      e.getPresentation().setEnabled(!CompilerManager.getInstance(project).isCompilationActive());
    }
  }
}
