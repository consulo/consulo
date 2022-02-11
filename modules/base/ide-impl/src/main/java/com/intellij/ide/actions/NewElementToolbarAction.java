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

package com.intellij.ide.actions;

import consulo.dataContext.DataManager;
import com.intellij.ide.projectView.ProjectView;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;

/**
 * @author yole
 */
public class NewElementToolbarAction extends NewElementAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getData(LangDataKeys.IDE_VIEW) == null) {
      final Project project = e.getData(CommonDataKeys.PROJECT);
      final PsiFileSystemItem psiFile = e.getData(LangDataKeys.PSI_FILE).getParent();
      ProjectView.getInstance(project).selectCB(psiFile, psiFile.getVirtualFile(), true).doWhenDone(new Runnable() {
        @Override
        public void run() {
          showPopup(DataManager.getInstance().getDataContext());
        }
      });
    }
    else {
      super.actionPerformed(e);
    }
  }

  @Override
  public void update(AnActionEvent event) {
    super.update(event);
    if (event.getData(LangDataKeys.IDE_VIEW) == null) {
      Project project = event.getData(CommonDataKeys.PROJECT);
      PsiFile psiFile = event.getData(LangDataKeys.PSI_FILE);
      if (project != null && psiFile != null) {
        final ToolWindow projectViewWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (projectViewWindow.isVisible()) {
          event.getPresentation().setEnabled(true);
        }
      }
    }
  }
}
