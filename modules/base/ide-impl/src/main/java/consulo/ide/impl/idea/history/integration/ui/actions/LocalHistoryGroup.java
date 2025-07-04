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

package consulo.ide.impl.idea.history.integration.ui.actions;

import consulo.application.dumb.DumbAware;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.NonTrivialActionGroup;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public class LocalHistoryGroup extends NonTrivialActionGroup implements DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    VirtualFile file = e.getData(VirtualFile.KEY);
    PsiElement element = e.getData(PsiElement.KEY);
    if (project == null || ActionPlaces.isPopupPlace(e.getPlace()) && (file != null && !file.isInLocalFileSystem() || file == null && element != null)) {
      e.getPresentation().setEnabledAndVisible(false);
    }
    else {
      super.update(e);
    }
  }
}

