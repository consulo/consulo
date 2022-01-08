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
package com.intellij.openapi.vcs.update;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RefreshVFsSynchronously {
  private RefreshVFsSynchronously() {
  }

  public static void updateAllChanged(@Nonnull final UpdatedFiles updatedFiles) {
    FilesToRefreshCollector callback = new FilesToRefreshCollector();
    UpdateFilesHelper.iterateFileGroupFilesDeletedOnServerFirst(updatedFiles, callback);

    refreshDeletedOrReplaced(callback.getToRefreshDeletedOrReplaced());
    refreshFiles(callback.getToRefresh());
  }

  @Nonnull
  public static Collection<VirtualFile> refreshFiles(@Nonnull Collection<File> files) {
    Collection<VirtualFile> filesToRefresh = ContainerUtil.newHashSet();
    for (File file : files) {
      VirtualFile vf = findFirstValidVirtualParent(file);
      if (vf != null) {
        filesToRefresh.add(vf);
      }
    }
    VfsUtil.markDirtyAndRefresh(false, false, false, ArrayUtil.toObjectArray(filesToRefresh, VirtualFile.class));
    return filesToRefresh;
  }

  private static void refreshDeletedOrReplaced(@Nonnull Collection<File> deletedOrReplaced) {
    Collection<VirtualFile> filesToRefresh = ContainerUtil.newHashSet();
    for (File file : deletedOrReplaced) {
      File parent = file.getParentFile();
      VirtualFile vf = findFirstValidVirtualParent(parent);
      if (vf != null) {
        filesToRefresh.add(vf);
      }
    }
    VfsUtil.markDirtyAndRefresh(false, true, false, ArrayUtil.toObjectArray(filesToRefresh, VirtualFile.class));
  }

  @javax.annotation.Nullable
  private static VirtualFile findFirstValidVirtualParent(@javax.annotation.Nullable File file) {
    LocalFileSystem lfs = LocalFileSystem.getInstance();
    VirtualFile vf = null;
    while (file != null && (vf == null || !vf.isValid())) {
      vf = lfs.findFileByIoFile(file);
      file = file.getParentFile();
    }
    return vf == null || !vf.isValid() ? null : vf;
  }

  public static void updateChangesForRollback(final List<Change> changes) {
    updateChangesImpl(changes, RollbackChangeWrapper.ourInstance);
  }

  public static void updateChanges(final Collection<Change> changes) {
    updateChangesImpl(changes, DirectChangeWrapper.ourInstance);
  }

  private static void updateChangesImpl(final Collection<Change> changes, final ChangeWrapper wrapper) {
    Collection<File> deletedOrReplaced = ContainerUtil.newHashSet();
    Collection<File> toRefresh = ContainerUtil.newHashSet();
    for (Change change : changes) {
      if ((! wrapper.beforeNull(change)) && (wrapper.movedOrRenamedOrReplaced(change) || (wrapper.afterNull(change)))) {
        deletedOrReplaced.add(wrapper.getBeforeFile(change));
      } else if (!wrapper.beforeNull(change)) {
        toRefresh.add(wrapper.getBeforeFile(change));
      }
      if ((! wrapper.afterNull(change)) &&
          (wrapper.beforeNull(change) || (! Comparing.equal(change.getAfterRevision().getFile(), change.getBeforeRevision().getFile())))
              ) {
        toRefresh.add(wrapper.getAfterFile(change));
      }
    }

    refreshFiles(toRefresh);
    refreshDeletedOrReplaced(deletedOrReplaced);
  }

  private static class RollbackChangeWrapper implements ChangeWrapper {
    private static final RollbackChangeWrapper ourInstance = new RollbackChangeWrapper();

    @Override
    public boolean beforeNull(Change change) {
      return change.getAfterRevision() == null;
    }

    @Override
    public boolean afterNull(Change change) {
      return change.getBeforeRevision() == null;
    }

    @Override
    public File getBeforeFile(Change change) {
      return beforeNull(change) ? null : change.getAfterRevision().getFile().getIOFile();
    }

    @Override
    public File getAfterFile(Change change) {
      return afterNull(change) ? null : change.getBeforeRevision().getFile().getIOFile();
    }

    @Override
    public boolean movedOrRenamedOrReplaced(Change change) {
      return change.isMoved() || change.isRenamed() || change.isIsReplaced();
    }
  }

  private static class DirectChangeWrapper implements ChangeWrapper {
    private static final DirectChangeWrapper ourInstance = new DirectChangeWrapper();

    @Override
    public boolean beforeNull(Change change) {
      return change.getBeforeRevision() == null;
    }

    @Override
    public boolean afterNull(Change change) {
      return change.getAfterRevision() == null;
    }

    @Override
    @javax.annotation.Nullable
    public File getBeforeFile(Change change) {
      return beforeNull(change) ? null : change.getBeforeRevision().getFile().getIOFile();
    }

    @Override
    @javax.annotation.Nullable
    public File getAfterFile(Change change) {
      return afterNull(change) ? null : change.getAfterRevision().getFile().getIOFile();
    }

    @Override
    public boolean movedOrRenamedOrReplaced(Change change) {
      return change.isMoved() || change.isRenamed() || change.isIsReplaced();
    }
  }

  private interface ChangeWrapper {
    boolean beforeNull(final Change change);
    boolean afterNull(final Change change);
    @javax.annotation.Nullable
    File getBeforeFile(final Change change);
    @javax.annotation.Nullable
    File getAfterFile(final Change change);
    boolean movedOrRenamedOrReplaced(final Change change);
  }

  private static class FilesToRefreshCollector implements UpdateFilesHelper.Callback {
    private final Collection<File> myToRefresh = new HashSet<File>();
    private final Collection<File> myToRefreshDeletedOrReplaced = new HashSet<File>();

    @Override
    public void onFile(String filePath, String groupId) {
      final File file = new File(filePath);
      if (FileGroup.REMOVED_FROM_REPOSITORY_ID.equals(groupId) || FileGroup.MERGED_WITH_TREE_CONFLICT.endsWith(groupId)) {
        myToRefreshDeletedOrReplaced.add(file);
      }
      else {
        myToRefresh.add(file);
      }
    }

    @Nonnull
    public Collection<File> getToRefresh() {
      return myToRefresh;
    }

    @Nonnull
    public Collection<File> getToRefreshDeletedOrReplaced() {
      return myToRefreshDeletedOrReplaced;
    }
  }

}
