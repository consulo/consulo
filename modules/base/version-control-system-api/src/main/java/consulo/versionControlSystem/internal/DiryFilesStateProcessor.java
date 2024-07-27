// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.versionControlSystem.internal;

import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsVFSListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class DiryFilesStateProcessor {
  private static final Logger LOG = Logger.getInstance(DiryFilesStateProcessor.class);

  private final Set<VirtualFile> myAddedFiles = new SmartHashSet<>();
  private final Map<VirtualFile, VirtualFile> myCopyFromMap = new HashMap<>(); // copy -> original
  private final Set<FilePath> myDeletedFiles = new SmartHashSet<>();
  private final Set<VcsVFSListener.MovedFileInfo> myMovedFiles = new SmartHashSet<>();

  private final ReentrantReadWriteLock PROCESSING_LOCK = new ReentrantReadWriteLock();

  private final ChangeListManager myChangeListManager;
  private final VcsFileListenerContextHelper myVcsFileListenerContextHelper;

  protected DiryFilesStateProcessor(ChangeListManager changeListManager,
                                    VcsFileListenerContextHelper vcsFileListenerContextHelper) {
    myChangeListManager = changeListManager;
    myVcsFileListenerContextHelper = vcsFileListenerContextHelper;
  }

  protected abstract boolean isFileCopyingFromTrackingSupported();

  protected abstract boolean isRecursiveDeleteSupported();

  protected abstract boolean isEventIgnored(@Nonnull VFileEvent event);

  protected abstract boolean filterOutByStatus(@Nonnull FileStatus status);

  protected abstract boolean shouldIgnoreDeletion(@Nonnull FileStatus status);

  protected abstract boolean isUnderMyVcs(@Nullable VirtualFile file);

  protected abstract void executePendingTasksImpl();

  @Nonnull
  public List<VirtualFile> acquireAddedFiles() {
    return acquireListUnderLock(myAddedFiles);
  }

  @Nonnull
  public List<VcsVFSListener.MovedFileInfo> acquireMovedFiles() {
    return acquireListUnderLock(myMovedFiles);
  }

  @Nonnull
  public List<FilePath> acquireDeletedFiles() {
    return ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      List<FilePath> deletedFiles = new ArrayList<>(myDeletedFiles);
      myDeletedFiles.clear();
      return deletedFiles;
    });
  }

  /**
   * @return get a list of files under lock and clear the given collection of files
   */
  @Nonnull
  private <T> List<T> acquireListUnderLock(@Nonnull Collection<? extends T> files) {
    return ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      List<T> copiedFiles = new ArrayList<>(files);
      files.clear();
      return copiedFiles;
    });
  }

  /**
   * @return get a map of copied files under lock and clear the given map
   */
  @Nonnull
  public Map<VirtualFile, VirtualFile> acquireCopiedFiles() {
    return ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      Map<VirtualFile, VirtualFile> copyFromMap = new HashMap<>(myCopyFromMap);
      myCopyFromMap.clear();
      return copyFromMap;
    });
  }

  public void clearAllPendingTasks() {
    ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      myAddedFiles.clear();
      myCopyFromMap.clear();
      myDeletedFiles.clear();
      myMovedFiles.clear();
    });
  }

  /**
   * Called under {@link #PROCESSING_LOCK} - avoid slow operations.
   */
  private void checkMovedAddedSourceBack() {
    if (myAddedFiles.isEmpty() || myMovedFiles.isEmpty()) return;

    Map<String, VirtualFile> addedPaths = new HashMap<>(myAddedFiles.size());
    for (VirtualFile file : myAddedFiles) {
      addedPaths.put(file.getPath(), file);
    }

    for (Iterator<VcsVFSListener.MovedFileInfo> iterator = myMovedFiles.iterator(); iterator.hasNext(); ) {
      VcsVFSListener.MovedFileInfo movedFile = iterator.next();
      VirtualFile oldAdded = addedPaths.get(movedFile.myOldPath);
      if (oldAdded != null) {
        iterator.remove();
        myAddedFiles.remove(oldAdded);
        myAddedFiles.add(movedFile.getFile());
        if (isFileCopyingFromTrackingSupported()) {
          myCopyFromMap.put(oldAdded, movedFile.getFile());
        }
      }
    }
  }

  /**
   * If a file is scheduled for deletion, and at the same time for copying or addition, don't delete it.
   * It happens during Overwrite command or undo of overwrite.
   * <p>
   * Called under {@link #PROCESSING_LOCK} - avoid slow operations.
   */
  private void doNotDeleteAddedCopiedOrMovedFiles() {
    if (myDeletedFiles.isEmpty()) return;

    Set<String> copiedAddedMoved = new HashSet<>();
    for (VirtualFile file : myCopyFromMap.keySet()) {
      copiedAddedMoved.add(file.getPath());
    }
    for (VirtualFile file : myAddedFiles) {
      copiedAddedMoved.add(file.getPath());
    }
    for (VcsVFSListener.MovedFileInfo movedFileInfo : myMovedFiles) {
      copiedAddedMoved.add(movedFileInfo.myNewPath);
    }

    myDeletedFiles.removeIf(path -> copiedAddedMoved.contains(path.getPath()));
  }

  public boolean isAnythingToProcess() {
    return ConcurrencyUtil.withLock(PROCESSING_LOCK.readLock(), () -> !myAddedFiles.isEmpty() ||
      !myDeletedFiles.isEmpty() ||
      !myMovedFiles.isEmpty());
  }

  public void executePendingTasks() {
    ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      doNotDeleteAddedCopiedOrMovedFiles();
      checkMovedAddedSourceBack();
    });

    executePendingTasksImpl();
  }

  private void processFileCreated(@Nonnull VFileCreateEvent event) {
    if (LOG.isDebugEnabled()) LOG.debug("fileCreated: ", event.getFile());
    if (!event.isDirectory()) {
      VirtualFile file = event.getFile();
      if (file == null) return;

      LOG.debug("Adding [", file, "] to added files");
      ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> myAddedFiles.add(file));
    }
  }

  private void processFileMoved(@Nonnull VFileMoveEvent event) {
    VirtualFile file = event.getFile();
    VirtualFile oldParent = event.getOldParent();
    if (!isUnderMyVcs(oldParent)) {
      ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> myAddedFiles.add(file));
    }
  }

  private void processFileCopied(@Nonnull VFileCopyEvent event) {
    VirtualFile newFile = event.getNewParent().findChild(event.getNewChildName());
    if (newFile == null || myChangeListManager.isIgnoredFile(newFile)) return;
    VirtualFile originalFile = event.getFile();
    if (isFileCopyingFromTrackingSupported() && isUnderMyVcs(originalFile)) {
      ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
        myAddedFiles.add(newFile);
        myCopyFromMap.put(newFile, originalFile);
      });
    }
    else {
      ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> myAddedFiles.add(newFile));
    }
  }

  private void processBeforeDeletedFile(@Nonnull VFileDeleteEvent event) {
    processBeforeDeletedFile(event.getFile());
  }

  private void processBeforeDeletedFile(@Nonnull VirtualFile file) {
    if (file.isDirectory() && file instanceof NewVirtualFile && !isRecursiveDeleteSupported()) {
      for (VirtualFile child : ((NewVirtualFile)file).getCachedChildren()) {
        ProgressManager.checkCanceled();
        FileStatus status = myChangeListManager.getStatus(child);
        if (!filterOutByStatus(status)) {
          processBeforeDeletedFile(child);
        }
      }
    }
    else {
      FileStatus status = myChangeListManager.getStatus(file);
      if (filterOutByStatus(status) || shouldIgnoreDeletion(status)) return;

      FilePath filePath = VcsUtil.getFilePath(file);
      ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> myDeletedFiles.add(filePath));
    }
  }

  protected void processMovedFile(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
    FileStatus status = myChangeListManager.getStatus(file);
    LOG.debug("Checking moved file ", file, "; status=", status);

    String newPath = newParentPath + "/" + newName;
    ConcurrencyUtil.withLock(PROCESSING_LOCK.writeLock(), () -> {
      if (!filterOutByStatus(status)) {
        VcsVFSListener.MovedFileInfo existingMovedFile = ContainerUtil.find(myMovedFiles, info -> Comparing.equal(info.getFile(), file));
        if (existingMovedFile != null) {
          LOG.debug("Reusing existing moved file [" + file + "] with new path [" + newPath + "]");
          existingMovedFile.myNewPath = newPath;
        }
        else {
          LOG.debug("Registered moved file ", file);
          myMovedFiles.add(new VcsVFSListener.MovedFileInfo(file, newPath));
        }
      }
      else {
        // If a file is moved on top of another file (overwrite), the VFS at first removes the original file,
        // and then performs the "clean" move.
        // But we don't need to handle this deletion by the VCS: it is not a real deletion, but just a trick to implement the overwrite.
        // This situation is already handled in doNotDeleteAddedCopiedOrMovedFiles(), but that method is called at the end of the command,
        // so it is not suitable for moving unversioned files: if an unversioned file is moved, it won't be recorded,
        // won't affect doNotDeleteAddedCopiedOrMovedFiles(), and therefore won't save the file from deletion.
        // Thus here goes a special handle for unversioned files overwrite-move.
        myDeletedFiles.remove(VcsUtil.getFilePath(newPath, file.isDirectory()));
      }
    });
  }

  private void processBeforeFileMovement(@Nonnull VFileMoveEvent event) {
    VirtualFile file = event.getFile();
    if (isUnderMyVcs(event.getNewParent())) {
      LOG.debug("beforeFileMovement ", event, " into same vcs");
      addFileToMove(file, event.getNewParent().getPath(), file.getName());
    }
    else {
      LOG.debug("beforeFileMovement ", event, " into different vcs");
      processBeforeDeletedFile(file);
    }
  }

  private void processBeforePropertyChange(@Nonnull VFilePropertyChangeEvent event) {
    if (event.isRename()) {
      LOG.debug("before file rename ", event);
      String newName = (String)event.getNewValue();
      VirtualFile file = event.getFile();
      VirtualFile parent = file.getParent();
      if (parent != null) {
        addFileToMove(file, parent.getPath(), newName);
      }
    }
  }

  private void addFileToMove(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
    if (file.isDirectory() && !file.is(VFileProperty.SYMLINK)) {
      @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = file.getChildren();
      if (children != null) {
        for (VirtualFile child : children) {
          ProgressManager.checkCanceled();
          addFileToMove(child, newParentPath + "/" + newName, child.getName());
        }
      }
    }
    else {
      processMovedFile(file, newParentPath, newName);
    }
  }

  public void processBeforeEvents(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (isEventIgnored(event)) continue;

      if (event instanceof VFileDeleteEvent && allowedDeletion(event)) {
        processBeforeDeletedFile((VFileDeleteEvent)event);
      }
      else if (event instanceof VFileMoveEvent) {
        processBeforeFileMovement((VFileMoveEvent)event);
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        processBeforePropertyChange((VFilePropertyChangeEvent)event);
      }
    }
  }

  public void processAfterEvents(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      ProgressManager.checkCanceled();
      if (isEventIgnored(event)) continue;

      if (event instanceof VFileCreateEvent) {
        processFileCreated((VFileCreateEvent)event);
      }
      else if (event instanceof VFileCopyEvent) {
        processFileCopied((VFileCopyEvent)event);
      }
      else if (event instanceof VFileMoveEvent) {
        processFileMoved((VFileMoveEvent)event);
      }
    }
  }

  public boolean allowedDeletion(@Nonnull VFileEvent event) {
    if (myVcsFileListenerContextHelper.isDeletionContextEmpty()) return true;

    return !myVcsFileListenerContextHelper.isDeletionIgnored(getEventFilePath(event));
  }

  public boolean allowedAddition(@Nonnull VFileEvent event) {
    if (myVcsFileListenerContextHelper.isAdditionContextEmpty()) return true;

    return !myVcsFileListenerContextHelper.isAdditionIgnored(getEventFilePath(event));
  }

  @Nonnull
  public static FilePath getEventFilePath(@Nonnull VFileEvent event) {
    if (event instanceof VFileCreateEvent createEvent) {
      return VcsUtil.getFilePath(event.getPath(), createEvent.isDirectory());
    }

    VirtualFile file = event.getFile();
    if (file != null) {
      // Do not use file.getPath(), as it is slower.
      return VcsUtil.getFilePath(event.getPath(), file.isDirectory());
    }
    else {
      LOG.error("VFileEvent should have VirtualFile: " + event);
      return VcsUtil.getFilePath(event.getPath());
    }
  }
}
