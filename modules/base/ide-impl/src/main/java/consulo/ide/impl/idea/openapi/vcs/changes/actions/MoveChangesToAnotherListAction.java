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

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangesViewManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangeListChooser;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesListView;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

import static consulo.language.editor.CommonDataKeys.PROJECT;

/**
 * @author max
 */
public class MoveChangesToAnotherListAction extends AnAction implements DumbAware {

  public MoveChangesToAnotherListAction() {
    super(ActionsBundle.actionText(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST),
          ActionsBundle.actionDescription(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST),
          AllIcons.Actions.MoveToAnotherChangelist);
  }

  public void update(@Nonnull AnActionEvent e) {
    boolean isEnabled = isEnabled(e);

    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setEnabledAndVisible(isEnabled);
    }
    else {
      e.getPresentation().setEnabled(isEnabled);
    }
  }

  protected boolean isEnabled(@Nonnull AnActionEvent e) {
    Project project = e.getData(PROJECT);
    if (project == null || !ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) {
      return false;
    }

    return !VcsUtil.isEmpty(e.getData(ChangesListView.UNVERSIONED_FILES_DATA_KEY)) ||
           !ArrayUtil.isEmpty(e.getData(VcsDataKeys.CHANGES)) ||
           !ArrayUtil.isEmpty(e.getData(VirtualFile.KEY_OF_ARRAY));
  }

  @Nonnull
  private static List<Change> getChangesForSelectedFiles(@Nonnull Project project,
                                                         @Nonnull VirtualFile[] selectedFiles,
                                                         @Nonnull List<VirtualFile> unversionedFiles,
                                                         @Nonnull List<VirtualFile> changedFiles) {
    List<Change> changes = new ArrayList<>();
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);

    for (VirtualFile vFile : selectedFiles) {
      Change change = changeListManager.getChange(vFile);
      if (change == null) {
        FileStatus status = changeListManager.getStatus(vFile);
        if (FileStatus.UNKNOWN.equals(status)) {
          unversionedFiles.add(vFile);
          changedFiles.add(vFile);
        }
        else if (FileStatus.NOT_CHANGED.equals(status) && vFile.isDirectory()) {
          addAllChangesUnderPath(changeListManager, VcsUtil.getFilePath(vFile), changes, changedFiles);
        }
      }
      else {
        FilePath afterPath = ChangesUtil.getAfterPath(change);
        if (afterPath != null && afterPath.isDirectory()) {
          addAllChangesUnderPath(changeListManager, afterPath, changes, changedFiles);
        }
        else {
          changes.add(change);
          changedFiles.add(vFile);
        }
      }
    }
    return changes;
  }

  private static void addAllChangesUnderPath(@Nonnull ChangeListManager changeListManager,
                                             @Nonnull FilePath file,
                                             @Nonnull List<Change> changes,
                                             @Nonnull List<VirtualFile> changedFiles) {
    for (Change change : changeListManager.getChangesIn(file)) {
      changes.add(change);

      FilePath path = ChangesUtil.getAfterPath(change);
      if (path != null && path.getVirtualFile() != null) {
        changedFiles.add(path.getVirtualFile());
      }
    }
  }

  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    List<Change> changesList = ContainerUtil.newArrayList();

    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    if (changes != null) {
      ContainerUtil.addAll(changesList, changes);
    }

    List<VirtualFile> unversionedFiles = ContainerUtil.newArrayList();
    final List<VirtualFile> changedFiles = ContainerUtil.newArrayList();
    VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
    if (files != null) {
      changesList.addAll(getChangesForSelectedFiles(project, files, unversionedFiles, changedFiles));
    }

    if (changesList.isEmpty() && unversionedFiles.isEmpty()) {
      VcsBalloonProblemNotifier.showOverChangesView(project, "Nothing is selected that can be moved", NotificationType.INFORMATION);
      return;
    }

    if (!askAndMove(project, changesList, unversionedFiles)) return;
    if (!changedFiles.isEmpty()) {
      selectAndShowFile(project, changedFiles.get(0));
    }
  }

  private static void selectAndShowFile(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(VcsToolWindow.ID);

    if (!window.isVisible()) {
      window.activate(() -> ChangesViewManager.getInstance(project).selectFile(file));
    }
  }

  public static boolean askAndMove(@Nonnull Project project,
                                   @Nonnull Collection<Change> changes,
                                   @Nonnull List<VirtualFile> unversionedFiles) {
    if (changes.isEmpty() && unversionedFiles.isEmpty()) return false;

    LocalChangeList targetList = askTargetList(project, changes);

    if (targetList != null) {
      ChangeListManagerImpl listManager = ChangeListManagerImpl.getInstanceImpl(project);

      listManager.moveChangesTo(targetList, ArrayUtil.toObjectArray(changes, Change.class));
      if (!unversionedFiles.isEmpty()) {
        listManager.addUnversionedFiles(targetList, unversionedFiles);
      }
      return true;
    }
    return false;
  }

  @Nullable
  private static LocalChangeList askTargetList(@Nonnull Project project, @Nonnull Collection<Change> changes) {
    ChangeListManagerImpl listManager = ChangeListManagerImpl.getInstanceImpl(project);
    List<LocalChangeList> preferredLists = getPreferredLists(listManager.getChangeListsCopy(), changes);
    List<LocalChangeList> listsForChooser =
            preferredLists.isEmpty() ? Collections.singletonList(listManager.getDefaultChangeList()) : preferredLists;
    ChangeListChooser chooser = new ChangeListChooser(project, listsForChooser, guessPreferredList(preferredLists),
                                                      ActionsBundle.message("action.ChangesView.Move.text"), null);
    chooser.show();

    return chooser.getSelectedList();
  }

  @Nullable
  private static ChangeList guessPreferredList(@Nonnull List<LocalChangeList> lists) {
    LocalChangeList activeChangeList = ContainerUtil.find(lists, LocalChangeList::isDefault);
    if (activeChangeList != null) return activeChangeList;

    LocalChangeList emptyList = ContainerUtil.find(lists, list -> list.getChanges().isEmpty());

    return ObjectUtil.chooseNotNull(emptyList, ContainerUtil.getFirstItem(lists));
  }

  @Nonnull
  private static List<LocalChangeList> getPreferredLists(@Nonnull List<LocalChangeList> lists, @Nonnull Collection<Change> changes) {
    final Set<Change> changesSet = ContainerUtil.newHashSet(changes);

    return ContainerUtil.findAll(lists, list -> !ContainerUtil.intersects(changesSet, list.getChanges()));
  }
}
