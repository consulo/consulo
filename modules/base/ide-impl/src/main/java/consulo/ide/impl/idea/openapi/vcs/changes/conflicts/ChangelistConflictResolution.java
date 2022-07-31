/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.conflicts;

import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelveChangesCommitExecutor;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.CommitChangeListDialog;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public enum ChangelistConflictResolution {

  SHELVE {
    @Override
    public boolean resolveConflict(Project project, Collection<Change> changes, VirtualFile selected) {
      LocalChangeList changeList = getManager(project).getChangeList(changes.iterator().next());
      return CommitChangeListDialog.commitChanges(project, changes, changeList, new ShelveChangesCommitExecutor(project), null);
    }},

  MOVE {
    @Override
    public boolean resolveConflict(Project project, Collection<Change> changes, VirtualFile selected) {
      ChangeListManagerImpl manager = getManager(project);
      Set<ChangeList> changeLists = new HashSet<>();
      for (Change change : changes) {
        LocalChangeList list = manager.getChangeList(change);
        if (list != null) {
          changeLists.add(list);
        }
      }
      if (changeLists.isEmpty()) {
        Messages.showInfoMessage(project, "The conflict seems to be resolved", "No Conflict Found");
        return true;
      }
      MoveChangesDialog dialog = new MoveChangesDialog(project, changes, changeLists, selected);
      if (dialog.showAndGet()) {
        manager.moveChangesTo(manager.getDefaultChangeList(), dialog.getIncludedChanges().toArray(new Change[changes.size()]));
        return true;
      }
      return false;
    }},

  SWITCH {
    @Override
    public boolean resolveConflict(Project project, Collection<Change> changes, VirtualFile selected) {
      LocalChangeList changeList = getManager(project).getChangeList(changes.iterator().next());
      assert changeList != null;
      getManager(project).setDefaultChangeList(changeList);
      return true;
    }},

  IGNORE {
    @Override
    public boolean resolveConflict(Project project, Collection<Change> changes, VirtualFile selected) {
      ChangeListManagerImpl manager = getManager(project);
      for (Change change : changes) {
        VirtualFile file = change.getVirtualFile();
        if (file != null) {
          manager.getConflictTracker().ignoreConflict(file, true);
        }
      }
      return true;
    }};

  public abstract boolean resolveConflict(Project project, Collection<Change> changes, VirtualFile selected);

  private static ChangeListManagerImpl getManager(Project project) {
    return (ChangeListManagerImpl)ChangeListManager.getInstance(project);
  }
}
