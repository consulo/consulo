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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.dumb.DumbAware;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserBase;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListView;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.Streams;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.internal.ChangesBrowserApi;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yole
 * @since 2006-11-02
 */
public class ScheduleForAdditionAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent e) {
    boolean enabled =
      e.getData(Project.KEY) != null && !Streams.isEmpty(getUnversionedFiles(e, e.getData(Project.KEY)));

    e.getPresentation().setEnabled(enabled);
    if (ActionPlaces.ACTION_PLACE_VCS_QUICK_LIST_POPUP_ACTION.equals(e.getPlace()) || ActionPlaces.CHANGES_VIEW_POPUP.equals(e.getPlace())) {
      e.getPresentation().setVisible(enabled);
    }
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    List<VirtualFile> unversionedFiles = getUnversionedFiles(e, project).collect(Collectors.toList());

    addUnversioned(project, unversionedFiles, this::isStatusForAddition, (ChangesBrowserBase) e.getData(ChangesBrowserApi.DATA_KEY));
  }

  @RequiredUIAccess
  public static boolean addUnversioned(
    @Nonnull Project project,
    @Nonnull List<VirtualFile> files,
    @Nonnull Predicate<FileStatus> unversionedFileCondition,
    @Nullable ChangesBrowserBase browser
  ) {
    boolean result = true;

    if (!files.isEmpty()) {
      FileDocumentManager.getInstance().saveAllDocuments();

      @SuppressWarnings("unchecked") Consumer<List<Change>> consumer = browser == null ? null : changes -> {
        browser.rebuildList();
        browser.getViewer().excludeChanges((List)files);
        browser.getViewer().includeChanges((List)changes);
      };
      ChangeListManagerImpl manager = ChangeListManagerImpl.getInstanceImpl(project);
      LocalChangeList targetChangeList =
        browser == null ? manager.getDefaultChangeList() : (LocalChangeList)browser.getSelectedChangeList();
      List<VcsException> exceptions = manager.addUnversionedFiles(targetChangeList, files, unversionedFileCondition, consumer);

      result = exceptions.isEmpty();
    }

    return result;
  }

  @Nonnull
  private Stream<VirtualFile> getUnversionedFiles(@Nonnull AnActionEvent e, @Nonnull Project project) {
    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
    boolean hasExplicitUnversioned = !Streams.isEmpty(e.getData(ChangesListView.UNVERSIONED_FILES_DATA_KEY));

    return hasExplicitUnversioned
      ? e.getRequiredData(ChangesListView.UNVERSIONED_FILES_DATA_KEY)
      : checkVirtualFiles(e) ? Streams.notNullize(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM))
                                      .filter(file -> isFileUnversioned(file, vcsManager, fileStatusManager)) : Stream.empty();
  }

  private boolean isFileUnversioned(@Nonnull VirtualFile file,
                                    @Nonnull ProjectLevelVcsManager vcsManager,
                                    @Nonnull FileStatusManager fileStatusManager) {
    AbstractVcs vcs = vcsManager.getVcsFor(file);
    return vcs != null && !vcs.areDirectoriesVersionedItems() && file.isDirectory() || isStatusForAddition(fileStatusManager.getStatus(file));
  }

  protected boolean isStatusForAddition(FileStatus status) {
    return status == FileStatus.UNKNOWN;
  }

  /**
   * {@link #isStatusForAddition(FileStatus)} checks file status to be {@link FileStatus.UNKNOWN} (if not overridden).
   * As an optimization, we assume that if {@link ChangesListView.UNVERSIONED_FILES_DATA_KEY} is empty, but {@link VcsDataKeys.CHANGES} is
   * not, then there will be either versioned (files from changes, hijacked files, locked files, switched files) or ignored files in
   * {@link VcsDataKeys.VIRTUAL_FILE_STREAM}. So there will be no files with {@link FileStatus.UNKNOWN} status and we should not explicitly
   * check {@link VcsDataKeys.VIRTUAL_FILE_STREAM} files in this case.
   */
  protected boolean checkVirtualFiles(@Nonnull AnActionEvent e) {
    return ArrayUtil.isEmpty(e.getData(VcsDataKeys.CHANGES));
  }
}
