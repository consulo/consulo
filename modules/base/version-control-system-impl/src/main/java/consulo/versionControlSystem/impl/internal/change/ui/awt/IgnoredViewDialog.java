/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

public class IgnoredViewDialog extends SpecificFilesViewDialog {
  public IgnoredViewDialog(@Nonnull Project project) {
    super(project,
          "Ignored Files",
          ChangesListView.IGNORED_FILES_DATA_KEY,
          ChangeListManagerEx.getInstanceEx(project).getIgnoredFiles());
  }

  @Override
  protected void addCustomActions(@Nonnull DefaultActionGroup group, @Nonnull ActionToolbar actionToolbar) {
    ActionManager actionManager = ActionManager.getInstance();
    AnAction deleteAction =
      EmptyAction.registerWithShortcutSet("ChangesView.DeleteUnversioned.From.Dialog", CommonShortcuts.getDelete(), myView);
    actionManager.addAnActionListener(new AnActionListener() {
      @Override
      public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (action.equals(deleteAction)) {
          refreshView();
          refreshChanges(myProject, getBrowserBase(myView));
        }
      }
    }, myDisposable);
    group.add(deleteAction);
    myView.setMenuActions(new DefaultActionGroup(deleteAction));
  }

  @Nonnull
  @Override
  protected List<VirtualFile> getFiles() {
    return ChangeListManagerEx.getInstanceEx(myProject).getIgnoredFiles();
  }
}
