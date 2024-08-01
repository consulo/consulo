// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Objects;

/**
 * Listens to file system events and notifies VcsDirtyScopeManagers responsible for changed files to mark these files dirty.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class VcsDirtyScopeVfsListener implements AsyncVfsEventsListener, Disposable {
  @Nonnull
  private final Project myProject;

  private boolean myForbid; // for tests only

  @Inject
  public VcsDirtyScopeVfsListener(@Nonnull Project project) {
    myProject = project;
    AsyncVfsEventsPostProcessor.getInstance().addListener(this, this);
  }

  public static VcsDirtyScopeVfsListener getInstance(@Nonnull Project project) {
    return project.getInstance(VcsDirtyScopeVfsListener.class);
  }

  public static void install(@Nonnull Project project) {
    getInstance(project);
  }

  @TestOnly
  public void setForbid(boolean forbid) {
    myForbid = forbid;
  }

  @Override
  public void dispose() {
  }

  @TestOnly
  public void waitForAsyncTaskCompletion() {
    // AsyncVfsEventsPostProcessorImpl.waitEventsProcessed();
  }

  @Override
  public void filesChanged(@Nonnull List<? extends VFileEvent> events) {
    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    if (myForbid || !vcsManager.hasActiveVcss()) return;

    final FilesAndDirs dirtyFilesAndDirs = new FilesAndDirs();
    // collect files and directories - sources of events
    for (VFileEvent event : events) {
      ProgressManager.checkCanceled();

      final boolean isDirectory;
      if (event instanceof VFileCreateEvent) {
        if (!((VFileCreateEvent)event).getParent().isInLocalFileSystem()) {
          continue;
        }
        isDirectory = ((VFileCreateEvent)event).isDirectory();
      }
      else {
        final VirtualFile file = Objects.requireNonNull(event.getFile(), "All events but VFileCreateEvent have @NotNull getFile()");
        if (!file.isInLocalFileSystem()) {
          continue;
        }
        isDirectory = file.isDirectory();
      }

      if (event instanceof VFileMoveEvent) {
        add(vcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(((VFileMoveEvent)event).getOldPath(), isDirectory));
        add(vcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(((VFileMoveEvent)event).getNewPath(), isDirectory));
      }
      else if (event instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent)event).isRename()) {
        // if a file was renamed, then the file is dirty and its parent directory is dirty too;
        // if a directory was renamed, all its children are recursively dirty, the parent dir is also dirty but not recursively.
        FilePath oldPath = VcsUtil.getFilePath(((VFilePropertyChangeEvent)event).getOldPath(), isDirectory);
        FilePath newPath = VcsUtil.getFilePath(((VFilePropertyChangeEvent)event).getNewPath(), isDirectory);
        // the file is dirty recursively, its old directory is dirty alone
        addWithParentDirectory(vcsManager, dirtyFilesAndDirs, oldPath);
        add(vcsManager, dirtyFilesAndDirs, newPath);
      }
      else {
        add(vcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(event.getPath(), isDirectory));
      }
    }

    VcsDirtyScopeManagerImpl dirtyScopeManager = VcsDirtyScopeManagerImpl.getInstanceImpl(myProject);
    dirtyScopeManager.fileVcsPathsDirty(dirtyFilesAndDirs.files.asMap(), dirtyFilesAndDirs.dirs.asMap());
  }

  /**
   * Stores VcsDirtyScopeManagers and files and directories which should be marked dirty by them.
   * Files will be marked dirty, directories will be marked recursively dirty, so if you need to mark dirty a directory, but
   * not recursively, you should add it to files.
   */
  private static class FilesAndDirs {
    @Nonnull
    VcsDirtyScopeMap files = new VcsDirtyScopeMap();
    @Nonnull
    VcsDirtyScopeMap dirs = new VcsDirtyScopeMap();
  }

  private static void add(@Nonnull ProjectLevelVcsManager vcsManager,
                          @Nonnull FilesAndDirs filesAndDirs,
                          @Nonnull FilePath filePath,
                          boolean withParentDirectory) {
    VcsRoot vcsRoot = vcsManager.getVcsRootObjectFor(filePath);
    AbstractVcs vcs = vcsRoot != null ? vcsRoot.getVcs() : null;
    if (vcsRoot == null || vcs == null) return;

    if (filePath.isDirectory()) {
      filesAndDirs.dirs.add(vcsRoot, filePath);
    }
    else {
      filesAndDirs.files.add(vcsRoot, filePath);
    }

    if (withParentDirectory && vcs.areDirectoriesVersionedItems()) {
      FilePath parentPath = filePath.getParentPath();
      if (parentPath != null && vcsManager.getVcsFor(parentPath) == vcs) {
        filesAndDirs.files.add(vcsRoot, parentPath);
      }
    }
  }

  private static void add(@Nonnull ProjectLevelVcsManager vcsManager,
                          @Nonnull FilesAndDirs filesAndDirs,
                          @Nonnull FilePath filePath) {
    add(vcsManager, filesAndDirs, filePath, false);
  }

  private static void addWithParentDirectory(@Nonnull ProjectLevelVcsManager vcsManager,
                                             @Nonnull FilesAndDirs filesAndDirs,
                                             @Nonnull FilePath filePath) {
    add(vcsManager, filesAndDirs, filePath, true);
  }
}
