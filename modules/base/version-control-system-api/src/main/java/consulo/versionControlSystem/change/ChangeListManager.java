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
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class ChangeListManager implements ChangeListModification {
  @Nonnull
  public static ChangeListManager getInstance(Project project) {
    return project.getInstance(ChangeListManager.class);
  }

  public abstract void scheduleUpdate();

  public abstract void scheduleUpdate(boolean updateUnversionedFiles);

  /**
   * Invoke callback when current CLM refresh is completed, without any visible progress.
   * <p/>
   * WARNING: This callback WILL NOT wait for async unchanged files update if VCS is using a custom {@link VcsManagedFilesHolder}.
   * These can be listened via {@link ChangeListListener#unchangedFileStatusChanged(boolean)} or on a per-VCS basis.
   */
  public void invokeAfterUpdate(boolean callbackOnAwt, @Nonnull Runnable afterUpdate) {
    InvokeAfterUpdateMode mode = callbackOnAwt ? InvokeAfterUpdateMode.SILENT : InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED;
    invokeAfterUpdate(afterUpdate, mode, null, null);
  }

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
  public abstract LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment, @Nullable ChangeListData data);

  public LocalChangeList addChangeList(@Nonnull final String name, @Nullable final String comment) {
    return addChangeList(name, comment, null);
  }

  public abstract void setDefaultChangeList(@Nonnull String name);

  public abstract void setDefaultChangeList(@Nonnull LocalChangeList list);

  /**
   * @param automatic true is changelist switch operation was not triggered by user (and, for example, will be reverted soon)
   *                  4ex: This flag disables automatic empty changelist deletion.
   */
  public abstract void setDefaultChangeList(@Nonnull LocalChangeList list, boolean automatic);

  public abstract void removeChangeList(final String name);

  public abstract void removeChangeList(final LocalChangeList list);

  public abstract void moveChangesTo(@Nonnull LocalChangeList list, Change... changes);

  public abstract void moveChangesTo(@Nonnull LocalChangeList list, @Nonnull List<Change> changes);

  // added - since ChangeListManager wouldn't pass internal lists, only copies
  public abstract boolean setReadOnly(final String name, final boolean value);

  public abstract boolean editName(@Nonnull String fromName, @Nonnull String toName);

  @Nullable
  public abstract String editComment(@Nonnull String fromName, final String newComment);

  public abstract int getChangeListsNumber();

  /**
   * @deprecated Use {@link #getChangeLists()} instead.
   */
  @Nonnull
  @Deprecated
  public List<LocalChangeList> getChangeListsCopy() {
    return getChangeLists();
  }

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
  public abstract FileStatus getStatus(@Nonnull FilePath file);

  @Nonnull
  public abstract FileStatus getStatus(@Nonnull VirtualFile file);

  @Nonnull
  public abstract Collection<Change> getChangesIn(VirtualFile dir);

  @Nonnull
  public abstract Collection<Change> getChangesIn(FilePath path);

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

  @Deprecated
  public void addFilesToIgnore(final IgnoredFileBean... ignoredFiles) {
  }

  @Deprecated
  public void addDirectoryToIgnoreImplicitly(@Nonnull String path) {
  }

  @Deprecated
  public void setFilesToIgnore(final IgnoredFileBean... ignoredFiles) {
  }

  @Deprecated
  public IgnoredFileBean[] getFilesToIgnore() {
    return new IgnoredFileBean[0];
  }

  public abstract boolean isIgnoredFile(@Nonnull VirtualFile file);

  public abstract boolean isIgnoredFile(@Nonnull FilePath file);

  @Nullable
  public abstract String getSwitchedBranch(VirtualFile file);

  public abstract String getDefaultListName();

  @Deprecated
  public abstract void letGo();

  public abstract String isFreezed();

  public abstract boolean isFreezedWithNotification(@Nullable String modalTitle);

  @Nonnull
  public abstract List<FilePath> getUnversionedFilesPaths();

  /**
   * @deprecated use {@link #getUnversionedFilesPaths}
   */
  @Deprecated
  @Nonnull
  public List<VirtualFile> getUnversionedFiles() {
    return ContainerUtil.mapNotNull(getUnversionedFilesPaths(), FilePath::getVirtualFile);
  }

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
