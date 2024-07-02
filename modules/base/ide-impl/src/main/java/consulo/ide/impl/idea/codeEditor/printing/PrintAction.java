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

package consulo.ide.impl.idea.codeEditor.printing;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;

public class PrintAction extends AnAction implements DumbAware {
  public PrintAction() {
    super();
  }

  @Override
  @RequiredReadAction
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return;
    }
    PrintManager.executePrint(dataContext);
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    VirtualFile file = dataContext.getData(VirtualFile.KEY);
    if (file != null && file.isDirectory()) {
      presentation.setEnabled(true);
      return;
    }
    Editor editor = dataContext.getData(Editor.KEY);
    PsiFile psiFile = dataContext.getData(PsiFile.KEY);
    presentation.setEnabled(psiFile != null || editor != null);
  }
}