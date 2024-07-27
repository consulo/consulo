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

package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class ChangeListManager {
  @Nonnull
  public static ChangeListManager getInstance(Project project) {
    return project.getInstance(ChangeListManager.class);
  }

  public abstract void scheduleUpdate();

  public abstract void scheduleUpdate(boolean updateUnversionedFiles);

  public abstract void invokeAfterUpdate(final Runnable afterUpdate,
                                         final InvokeAfterUpdateMode mode,
                                         final String title,
                                         final ModalityState state);

  public abstract void invokeAfterUpdate(final Runnable afterUpdate,
                                         final InvokeAfterUpdateMode mode,
                                         final String title,
                                         final Consumer<VcsDirtyScopeManager> dirtyScopeManager,
                                         final ModalityState state);


  @Nonnull
  public abstract LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment, @Nullable Object data);

  public abstract LocalChangeList addChangeList(@Nonnull String name, @Nullable final String comment);

  public abstract void setDefaultChangeList(@Nonnull LocalChangeList list);

  public abstract void removeChangeList(final String name);

  public abstract void removeChangeList(final LocalChangeList list);

  public abstract void moveChangesTo(final LocalChangeList list, final Change... changes);

  // added - since ChangeListManager wouldn't pass internal lists, only copies
  public abstract boolean setReadOnly(final String name, final boolean value);

  public abstract boolean editName(@Nonnull String fromName, @Nonnull String toName);

  @Nullable
  public abstract String editComment(@Nonnull String fromName, final String newComment);

  @TestOnly
  public abstract boolean ensureUpToDate(boolean canBeCanceled);

  public abstract int getChangeListsNumber();

  public abstract List<LocalChangeList> getChangeListsCopy();

  @Nonnull
  public abstract List<LocalChangeList> getChangeLists();

  @Nonnull
  public abstract List<LocalChangeList> getChangeLists(@Nonnull Change change);

  @Nonnull
  public abstract List<LocalChangeList> getChangeLists(@Nonnull VirtualFile file);

  public abstract List<File> getAffectedPaths();

  @Nonnull
  public abstract List<VirtualFile> getAffectedFiles();

  public abstract boolean isFileAffected(final VirtualFile file);

  /**
   * @return all changes in all changelists.
   */
  @Nonnull
  public abstract Collection<Change> getAllChanges();

  @Nullable
  public abstract LocalChangeList findChangeList(final String name);

  @Nullable
  public abstract LocalChangeList getChangeList(String id);

  /**
   * Returns currently active changelist
   *
   * @return active changelist
   */
  public abstract LocalChangeList getDefaultChangeList();

  public abstract boolean isDefaultChangeList(ChangeList list);

  @Nullable
  public abstract LocalChangeList getChangeList(@Nonnull Change change);

  @Nullable
  public abstract String getChangeListNameIfOnlyOne(Change[] changes);

  @Nonnull
  public abstract Runnable prepareForChangeDeletion(final Collection<Change> changes);

  @Nullable
  public abstract Change getChange(@Nonnull VirtualFile file);

  @Nullable
  public abstract LocalChangeList getChangeList(@Nonnull VirtualFile file);

  @Nullable
  public abstract Change getChange(FilePath file);

  public abstract boolean isUnversioned(VirtualFile file);

  @Nonnull
  public abstract FileStatus getStatus(VirtualFile file);

  @Nonnull
  public abstract Collection<Change> getChangesIn(VirtualFile dir);

  @Nonnull
  public abstract Collection<Change> getChangesIn(FilePath path);

  @Nullable
  public abstract AbstractVcs getVcsFor(@Nonnull Change change);

//  public abstract void removeChangeList(final LocalChangeList list);

//  public abstract void moveChangesTo(final LocalChangeList list, final Change[] changes);

  public abstract void addChangeListListener(ChangeListListener listener);

  public abstract void removeChangeListListener(ChangeListListener listener);

  public abstract void registerCommitExecutor(CommitExecutor executor);

  public abstract void commitChanges(LocalChangeList changeList, List<Change> changes);

  public abstract void commitChangesSynchronously(LocalChangeList changeList, List<Change> changes);

  /**
   * @return if commit successful
   */
  public abstract boolean commitChangesSynchronouslyWithResult(LocalChangeList changeList, List<Change> changes);

  public abstract void reopenFiles(List<FilePath> paths);

  public abstract List<CommitExecutor> getRegisteredExecutors();

  public abstract void addFilesToIgnore(final IgnoredFileBean... ignoredFiles);

  public abstract void addDirectoryToIgnoreImplicitly(@Nonnull String path);

  public abstract void setFilesToIgnore(final IgnoredFileBean... ignoredFiles);

  public abstract IgnoredFileBean[] getFilesToIgnore();

  public boolean isIgnoredFile(@Nonnull VirtualFile file) {
    return isIgnoredFile(VcsUtil.getFilePath(file));
  }

  public abstract boolean isIgnoredFile(@Nonnull FilePath file);

  public abstract boolean isContainedInLocallyDeleted(final FilePath filePath);

  @Nullable
  public abstract String getSwitchedBranch(VirtualFile file);

  public abstract String getDefaultListName();

  @Deprecated
  public abstract void letGo();

  public abstract String isFreezed();

  public abstract boolean isFreezedWithNotification(@Nullable String modalTitle);


  public abstract List<VirtualFile> getModifiedWithoutEditing();

  @Nonnull
  public abstract ThreeState haveChangesUnder(@Nonnull VirtualFile vf);

  public boolean areChangeListsEnabled() {
    return true;
  }

  public boolean isInUpdate() {
    return false;
  }
}
