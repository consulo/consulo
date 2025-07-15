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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesBrowserUseCase;
import consulo.ide.impl.idea.openapi.vcs.vfs.ContentRevisionVirtualFile;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */                                                                            
public class OpenRepositoryVersionAction extends AnAction implements DumbAware {
  public OpenRepositoryVersionAction() {
    super(VcsLocalize.openRepositoryVersionText(), VcsLocalize.openRepositoryVersionDescription(), PlatformIconGroup.nodesPpweb());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    Change[] changes = e.getRequiredData(VcsDataKeys.SELECTED_CHANGES);
    for (Change change: changes) {
      ContentRevision revision = change.getAfterRevision();
      if (revision == null || revision.getFile().isDirectory()) continue;
      VirtualFile vFile = ContentRevisionVirtualFile.create(revision);
      Navigatable navigatable = new OpenFileDescriptorImpl(project, vFile);
      navigatable.navigate(true);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    Change[] changes = e.getData(VcsDataKeys.SELECTED_CHANGES);
    e.getPresentation().setEnabled(
      project != null && changes != null
        && (!CommittedChangesBrowserUseCase.IN_AIR.equals(e.getDataContext().getData(CommittedChangesBrowserUseCase.DATA_KEY)))
        && hasValidChanges(changes)
        && IdeaModalityState.nonModal().equals(IdeaModalityState.current())
    );
  }

  private static boolean hasValidChanges(final Change[] changes) {
    for (Change c: changes) {
      final ContentRevision contentRevision = c.getAfterRevision();
      if (contentRevision != null && !contentRevision.getFile().isDirectory()) {
        return true;
      }
    }
    return false;
  }
}
