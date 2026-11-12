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
package consulo.versionControlSystem.update;

import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RefreshVFsSynchronously {
  private RefreshVFsSynchronously() {
  }

  public static void updateAllChanged(UpdatedFiles updatedFiles) {
    FilesToRefreshCollector callback = new FilesToRefreshCollector();
    UpdateFilesHelper.iterateFileGroupFilesDeletedOnServerFirst(updatedFiles, callback);

    refreshDeletedOrReplaced(callback.getToRefreshDeletedOrReplaced());
    refreshFiles(callback.getToRefresh());
  }

  
  public static Collection<VirtualFile> refreshFiles(Collection<Path> files) {
    Collection<VirtualFile> filesToRefresh = new HashSet<>();
    for (Path file : files) {
      VirtualFile vf = findFirstValidVirtualParent(file);
      if (vf != null) {
        filesToRefresh.add(vf);
      }
    }
    VirtualFileUtil.markDirtyAndRefresh(false, false, false, ArrayUtil.toObjectArray(filesToRefresh, VirtualFile.class));
    return filesToRefresh;
  }

  private static void refreshDeletedOrReplaced(Collection<Path> deletedOrReplaced) {
    Collection<VirtualFile> filesToRefresh = new HashSet<>();
    for (Path file : deletedOrReplaced) {
      Path parent = file.getParent();
      VirtualFile vf = findFirstValidVirtualParent(parent);
      if (vf != null) {
        filesToRefresh.add(vf);
      }
    }
    VirtualFileUtil.markDirtyAndRefresh(false, true, false, ArrayUtil.toObjectArray(filesToRefresh, VirtualFile.class));
  }

  /**
   * The path is of the machine of the file - the local one, or a remote one.
   */
  private static @Nullable VirtualFile findFirstValidVirtualParent(@Nullable Path file) {
    VirtualFileManager manager = VirtualFileManager.getInstance();
    VirtualFile vf = null;
    while (file != null && (vf == null || !vf.isValid())) {
      vf = manager.findFileByNioPath(file);
      file = file.getParent();
    }
    return vf == null || !vf.isValid() ? null : vf;
  }

  public static void updateChangesForRollback(List<Change> changes) {
    updateChangesImpl(changes, RollbackChangeWrapper.ourInstance);
  }

  public static void updateChanges(Collection<Change> changes) {
    updateChangesImpl(changes, DirectChangeWrapper.ourInstance);
  }

  private static void updateChangesImpl(Collection<Change> changes, ChangeWrapper wrapper) {
    Collection<Path> deletedOrReplaced = new HashSet<>();
    Collection<Path> toRefresh = new HashSet<>();
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

    // a path not of a file system has nothing to refresh
    toRefresh.remove(null);
    deletedOrReplaced.remove(null);
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
    public @Nullable Path getBeforeFile(Change change) {
      return beforeNull(change) ? null : change.getAfterRevision().getFile().getNioPath();
    }

    @Override
    public @Nullable Path getAfterFile(Change change) {
      return afterNull(change) ? null : change.getBeforeRevision().getFile().getNioPath();
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
    public @Nullable Path getBeforeFile(Change change) {
      return beforeNull(change) ? null : change.getBeforeRevision().getFile().getNioPath();
    }

    @Override
    public @Nullable Path getAfterFile(Change change) {
      return afterNull(change) ? null : change.getAfterRevision().getFile().getNioPath();
    }

    @Override
    public boolean movedOrRenamedOrReplaced(Change change) {
      return change.isMoved() || change.isRenamed() || change.isIsReplaced();
    }
  }

  private interface ChangeWrapper {
    boolean beforeNull(Change change);
    boolean afterNull(Change change);
    @Nullable Path getBeforeFile(Change change);
    @Nullable Path getAfterFile(Change change);
    boolean movedOrRenamedOrReplaced(Change change);
  }

  private static class FilesToRefreshCollector implements UpdateFilesHelper.Callback {
    private final Collection<Path> myToRefresh = new HashSet<>();
    private final Collection<Path> myToRefreshDeletedOrReplaced = new HashSet<>();

    @Override
    public void onFile(String filePath, String groupId) {
      Path file = Path.of(filePath);
      if (FileGroup.REMOVED_FROM_REPOSITORY_ID.equals(groupId) || FileGroup.MERGED_WITH_TREE_CONFLICT.endsWith(groupId)) {
        myToRefreshDeletedOrReplaced.add(file);
      }
      else {
        myToRefresh.add(file);
      }
    }

    
    public Collection<Path> getToRefresh() {
      return myToRefresh;
    }

    
    public Collection<Path> getToRefreshDeletedOrReplaced() {
      return myToRefreshDeletedOrReplaced;
    }
  }

}
