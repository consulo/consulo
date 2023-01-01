/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.project.ui.view.ProjectView;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;

public class GoToLinkTargetAction extends DumbAwareAction {
  @Override
  public void update(AnActionEvent e) {
    Project project = e == null ? null : e.getData(CommonDataKeys.PROJECT);
    VirtualFile file = e.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
    e.getPresentation().setEnabledAndVisible(project != null && file != null && file.is(VFileProperty.SYMLINK));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e == null ? null : e.getData(CommonDataKeys.PROJECT);
    VirtualFile file = e.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
    if (project != null && file != null && file.is(VFileProperty.SYMLINK)) {
      VirtualFile target = file.getCanonicalFile();
      if (target != null) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFileSystemItem psiFile = target.isDirectory() ? psiManager.findDirectory(target) : psiManager.findFile(target);
        if (psiFile != null) {
          ProjectView.getInstance(project).select(psiFile, target, false);
        }
      }
    }
  }
}
