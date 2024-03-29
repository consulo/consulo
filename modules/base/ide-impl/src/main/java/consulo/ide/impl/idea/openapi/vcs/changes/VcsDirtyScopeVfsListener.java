// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.util.ZipperUpdater;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Listens to file system events and notifies VcsDirtyScopeManagers responsible for changed files to mark these files dirty.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsDirtyScopeVfsListener implements AsyncFileListener, Disposable {
  @Nonnull
  private final ProjectLevelVcsManager myVcsManager;

  private boolean myForbid; // for tests only

  @Nonnull
  private final ZipperUpdater myZipperUpdater;
  private final List<FilesAndDirs> myQueue;
  private final Object myLock;
  @Nonnull
  private final Runnable myDirtReporter;

  @Inject
  public VcsDirtyScopeVfsListener(@Nonnull Project project) {
    myVcsManager = ProjectLevelVcsManager.getInstance(project);

    VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);

    myLock = new Object();
    myQueue = new ArrayList<>();
    myDirtReporter = () -> {
      ArrayList<FilesAndDirs> list;
      synchronized (myLock) {
        list = new ArrayList<>(myQueue);
        myQueue.clear();
      }

      HashSet<FilePath> dirtyFiles = new HashSet<>();
      HashSet<FilePath> dirtyDirs = new HashSet<>();
      for (FilesAndDirs filesAndDirs : list) {
        dirtyFiles.addAll(filesAndDirs.forcedNonRecursive);

        for (FilePath path : filesAndDirs.regular) {
          if (path.isDirectory()) {
            dirtyDirs.add(path);
          }
          else {
            dirtyFiles.add(path);
          }
        }
      }

      if (!dirtyFiles.isEmpty() || !dirtyDirs.isEmpty()) {
        dirtyScopeManager.filePathsDirty(dirtyFiles, dirtyDirs);
      }
    };
    myZipperUpdater = new ZipperUpdater(300, Alarm.ThreadToUse.POOLED_THREAD, this);
    Disposer.register(project, this);
    VirtualFileManager.getInstance().addAsyncFileListener(this, project);
  }

  public static VcsDirtyScopeVfsListener getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, VcsDirtyScopeVfsListener.class);
  }

  public static void install(@Nonnull Project project) {
    if (!project.isOpen()) {
      throw new RuntimeException("Already closed: " + project);
    }
    getInstance(project);
  }

  public void setForbid(boolean forbid) {
    assert ApplicationManager.getApplication().isUnitTestMode();
    myForbid = forbid;
  }

  @Override
  public void dispose() {
    synchronized (myLock) {
      myQueue.clear();
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      //noinspection TestOnlyProblems
      waitForAsyncTaskCompletion();
    }
  }

  @TestOnly
  void waitForAsyncTaskCompletion() {
    myZipperUpdater.waitForAllExecuted(10, TimeUnit.SECONDS);
  }

  @Nullable
  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    if (myForbid || !myVcsManager.hasAnyMappings()) return null;
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
        add(myVcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(((VFileMoveEvent)event).getOldPath(), isDirectory));
        add(myVcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(((VFileMoveEvent)event).getNewPath(), isDirectory));
      }
      else if (event instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent)event).isRename()) {
        // if a file was renamed, then the file is dirty and its parent directory is dirty too;
        // if a directory was renamed, all its children are recursively dirty, the parent dir is also dirty but not recursively.
        FilePath oldPath = VcsUtil.getFilePath(((VFilePropertyChangeEvent)event).getOldPath(), isDirectory);
        FilePath newPath = VcsUtil.getFilePath(((VFilePropertyChangeEvent)event).getNewPath(), isDirectory);
        // the file is dirty recursively
        add(myVcsManager, dirtyFilesAndDirs, oldPath);
        add(myVcsManager, dirtyFilesAndDirs, newPath);
        FilePath parentPath = oldPath.getParentPath();
        if (parentPath != null) {
          addAsFiles(myVcsManager, dirtyFilesAndDirs, parentPath); // directory is dirty alone
        }
      }
      else {
        add(myVcsManager, dirtyFilesAndDirs, VcsUtil.getFilePath(event.getPath(), isDirectory));
      }
    }

    return new ChangeApplier() {
      @Override
      public void afterVfsChange() {
        markDirtyOnPooled(dirtyFilesAndDirs);
      }
    };
  }

  private void markDirtyOnPooled(@Nonnull FilesAndDirs dirtyFilesAndDirs) {
    if (!dirtyFilesAndDirs.isEmpty()) {
      synchronized (myLock) {
        myQueue.add(dirtyFilesAndDirs);
      }
      myZipperUpdater.queue(myDirtReporter);
    }
  }

  /**
   * Stores VcsDirtyScopeManagers and files and directories which should be marked dirty by them.
   * Files will be marked dirty, directories will be marked recursively dirty, so if you need to mark dirty a directory, but
   * not recursively, you should add it to files.
   */
  private static class FilesAndDirs {
    @Nonnull
    HashSet<FilePath> forcedNonRecursive = new HashSet<>();
    @Nonnull
    HashSet<FilePath> regular = new HashSet<>();

    private boolean isEmpty() {
      return forcedNonRecursive.isEmpty() && regular.isEmpty();
    }
  }

  private static void add(@Nonnull ProjectLevelVcsManager vcsManager, @Nonnull FilesAndDirs filesAndDirs, @Nonnull FilePath filePath, boolean forceAddAsFiles) {
    if (vcsManager.getVcsFor(filePath) == null) return;

    if (forceAddAsFiles) {
      filesAndDirs.forcedNonRecursive.add(filePath);
    }
    else {
      filesAndDirs.regular.add(filePath);
    }
  }

  private static void add(@Nonnull ProjectLevelVcsManager vcsManager, @Nonnull FilesAndDirs filesAndDirs, @Nonnull FilePath filePath) {
    add(vcsManager, filesAndDirs, filePath, false);
  }

  private static void addAsFiles(@Nonnull ProjectLevelVcsManager vcsManager, @Nonnull FilesAndDirs filesAndDirs, @Nonnull FilePath filePath) {
    add(vcsManager, filesAndDirs, filePath, true);
  }
}
