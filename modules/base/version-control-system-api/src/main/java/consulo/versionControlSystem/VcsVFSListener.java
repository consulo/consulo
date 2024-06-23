// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.versionControlSystem;

import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.internal.ProjectConfigurationFilesProcessorImpl;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static consulo.util.concurrent.ConcurrencyUtil.withLock;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public abstract class VcsVFSListener implements Disposable {
  protected static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

  private final ProjectLevelVcsManager myVcsManager;
  private final VcsIgnoreManager myVcsIgnoreManager;
  private final VcsFileListenerContextHelper myVcsFileListenerContextHelper;

  protected static class MovedFileInfo {
    @Nonnull
    public final String myOldPath;
    @Nonnull
    public String myNewPath;
    @Nonnull
    private final VirtualFile myFile;

    MovedFileInfo(@Nonnull VirtualFile file, @Nonnull String newPath) {
      myOldPath = file.getPath();
      myNewPath = newPath;
      myFile = file;
    }

    @Override
    public String toString() {
      return String.format("MovedFileInfo{[%s] -> [%s]}", myOldPath, myNewPath);  //NON-NLS
    }

    public boolean isCaseSensitive() {
      return myFile.isCaseSensitive();
    }

    public @Nonnull FilePath getOldPath() {
      return VcsUtil.getFilePath(myOldPath, myFile.isDirectory());
    }

    public @Nonnull FilePath getNewPath() {
      return VcsUtil.getFilePath(myNewPath, myFile.isDirectory());
    }
  }

  protected final Project myProject;
  protected final AbstractVcs myVcs;
  protected final ChangeListManager myChangeListManager;
  protected final VcsShowConfirmationOption myAddOption;
  protected final VcsShowConfirmationOption myRemoveOption;
  protected final StateProcessor myProcessor = new StateProcessor();
  private final ProjectConfigurationFilesProcessorImpl myProjectConfigurationFilesProcessor;
  protected final ExternallyAddedFilesProcessorImpl myExternalFilesProcessor;
  private final IgnoreFilesProcessorImpl myIgnoreFilesProcessor;
  private final List<VFileEvent> myEventsToProcess = new SmartList<>();

  protected final class StateProcessor {
    private final Set<VirtualFile> myAddedFiles = new SmartHashSet<>();
    private final Map<VirtualFile, VirtualFile> myCopyFromMap = new HashMap<>(); // copy -> original
    private final Set<FilePath> myDeletedFiles = new SmartHashSet<>();
    private final Set<MovedFileInfo> myMovedFiles = new SmartHashSet<>();

    private final ReentrantReadWriteLock PROCESSING_LOCK = new ReentrantReadWriteLock();

    @Nonnull
    public List<VirtualFile> acquireAddedFiles() {
      return acquireListUnderLock(myAddedFiles);
    }

    @Nonnull
    public List<MovedFileInfo> acquireMovedFiles() {
      return acquireListUnderLock(myMovedFiles);
    }

    @Nonnull
    public List<FilePath> acquireDeletedFiles() {
      return withLock(PROCESSING_LOCK.writeLock(), () -> {
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
      return withLock(PROCESSING_LOCK.writeLock(), () -> {
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
      return withLock(PROCESSING_LOCK.writeLock(), () -> {
        Map<VirtualFile, VirtualFile> copyFromMap = new HashMap<>(myCopyFromMap);
        myCopyFromMap.clear();
        return copyFromMap;
      });
    }

    private void clearAllPendingTasks() {
      withLock(PROCESSING_LOCK.writeLock(), () -> {
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

      for (Iterator<MovedFileInfo> iterator = myMovedFiles.iterator(); iterator.hasNext(); ) {
        MovedFileInfo movedFile = iterator.next();
        VirtualFile oldAdded = addedPaths.get(movedFile.myOldPath);
        if (oldAdded != null) {
          iterator.remove();
          myAddedFiles.remove(oldAdded);
          myAddedFiles.add(movedFile.myFile);
          if (isFileCopyingFromTrackingSupported()) {
            myCopyFromMap.put(oldAdded, movedFile.myFile);
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
      for (MovedFileInfo movedFileInfo : myMovedFiles) {
        copiedAddedMoved.add(movedFileInfo.myNewPath);
      }

      myDeletedFiles.removeIf(path -> copiedAddedMoved.contains(path.getPath()));
    }

    private boolean isAnythingToProcess() {
      return withLock(PROCESSING_LOCK.readLock(), () -> !myAddedFiles.isEmpty() ||
        !myDeletedFiles.isEmpty() ||
        !myMovedFiles.isEmpty());
    }


    private void executePendingTasks() {
      withLock(PROCESSING_LOCK.writeLock(), () -> {
        doNotDeleteAddedCopiedOrMovedFiles();
        checkMovedAddedSourceBack();
      });

      executeAdd();
      executeDelete();
      executeMoveRename();
    }

    private void processFileCreated(@Nonnull VFileCreateEvent event) {
      if (LOG.isDebugEnabled()) LOG.debug("fileCreated: ", event.getFile());
      if (!event.isDirectory()) {
        VirtualFile file = event.getFile();
        if (file == null) return;

        LOG.debug("Adding [", file, "] to added files");
        withLock(PROCESSING_LOCK.writeLock(), () -> {
          myAddedFiles.add(file);
        });
      }
    }

    private void processFileMoved(@Nonnull VFileMoveEvent event) {
      VirtualFile file = event.getFile();
      VirtualFile oldParent = event.getOldParent();
      if (!isUnderMyVcs(oldParent)) {
        withLock(PROCESSING_LOCK.writeLock(), () -> {
          myAddedFiles.add(file);
        });
      }
    }

    private void processFileCopied(@Nonnull VFileCopyEvent event) {
      VirtualFile newFile = event.getNewParent().findChild(event.getNewChildName());
      if (newFile == null || myChangeListManager.isIgnoredFile(newFile)) return;
      VirtualFile originalFile = event.getFile();
      if (isFileCopyingFromTrackingSupported() && isUnderMyVcs(originalFile)) {
        withLock(PROCESSING_LOCK.writeLock(), () -> {
          myAddedFiles.add(newFile);
          myCopyFromMap.put(newFile, originalFile);
        });
      }
      else {
        withLock(PROCESSING_LOCK.writeLock(), () -> {
          myAddedFiles.add(newFile);
        });
      }
    }

    @RequiredUIAccess
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
        withLock(PROCESSING_LOCK.writeLock(), () -> {
          myDeletedFiles.add(filePath);
        });
      }
    }

    @RequiredUIAccess
    private void processMovedFile(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
      FileStatus status = myChangeListManager.getStatus(file);
      LOG.debug("Checking moved file ", file, "; status=", status);

      String newPath = newParentPath + "/" + newName;
      withLock(PROCESSING_LOCK.writeLock(), () -> {
        if (!filterOutByStatus(status)) {
          MovedFileInfo existingMovedFile = ContainerUtil.find(myMovedFiles, info -> Comparing.equal(info.myFile, file));
          if (existingMovedFile != null) {
            LOG.debug("Reusing existing moved file [" + file + "] with new path [" + newPath + "]");
            existingMovedFile.myNewPath = newPath;
          }
          else {
            LOG.debug("Registered moved file ", file);
            myMovedFiles.add(new MovedFileInfo(file, newPath));
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

    @RequiredUIAccess
    private void processBeforeFileMovement(@Nonnull VFileMoveEvent event) {
      VirtualFile file = event.getFile();
      if (isUnderMyVcs(event.getNewParent())) {
        LOG.debug("beforeFileMovement ", event, " into same vcs");
        addFileToMove(file, event.getNewParent().getPath(), file.getName());
      }
      else {
        LOG.debug("beforeFileMovement ", event, " into different vcs");
        myProcessor.processBeforeDeletedFile(file);
      }
    }

    @RequiredUIAccess
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

    @RequiredUIAccess
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
        VcsVFSListener.this.processMovedFile(file, newParentPath, newName);
        myProcessor.processMovedFile(file, newParentPath, newName);
      }
    }

    @RequiredUIAccess
    private void processBeforeEvents(@Nonnull List<? extends VFileEvent> events) {
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


    private void processAfterEvents(@Nonnull List<? extends VFileEvent> events) {
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
  }

  /**
   * @see #installListeners()
   */
  protected VcsVFSListener(@Nonnull AbstractVcs vcs) {
    myProject = vcs.getProject();
    myVcs = vcs;
    myChangeListManager = ChangeListManager.getInstance(myProject);
    myVcsIgnoreManager = VcsIgnoreManager.getInstance(myProject);

    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    myAddOption = myVcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, vcs);
    myRemoveOption = myVcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, vcs);

    myVcsFileListenerContextHelper = VcsFileListenerContextHelper.getInstance(myProject);

    myProjectConfigurationFilesProcessor = createProjectConfigurationFilesProcessor();
    myExternalFilesProcessor = createExternalFilesProcessor();
    myIgnoreFilesProcessor = createIgnoreFilesProcessor();
  }

  /**
   * @deprecated Use {@link #VcsVFSListener(AbstractVcs)} followed by {@link #installListeners()}
   */
  @Deprecated(forRemoval = true)
  protected VcsVFSListener(@Nonnull Project project, @Nonnull AbstractVcs vcs) {
    this(vcs);
    installListeners();
  }

  protected void installListeners() {
    VirtualFileManager.getInstance().addAsyncFileListener(new MyAsyncVfsListener(), this);
    myProject.getMessageBus().connect(this).subscribe(CommandListener.class, new MyCommandAdapter());

    myProjectConfigurationFilesProcessor.install();
    myExternalFilesProcessor.install();
    myIgnoreFilesProcessor.install();
  }

  @Override
  public void dispose() {
  }

  protected boolean isEventAccepted(@Nonnull VFileEvent event) {
    return !event.isFromRefresh() && (event.getFileSystem() instanceof LocalFileSystem);
  }

  protected boolean isEventIgnored(@Nonnull VFileEvent event) {
    FilePath filePath = getEventFilePath(event);
    return !isUnderMyVcs(filePath) || myChangeListManager.isIgnoredFile(filePath);
  }

  protected boolean isUnderMyVcs(@Nullable VirtualFile file) {
    return file != null && ReadAction.compute(() -> !myProject.isDisposed() && myVcsManager.getVcsFor(file) == myVcs);
  }

  protected boolean isUnderMyVcs(@Nullable FilePath filePath) {
    return filePath != null && ReadAction.compute(() -> !myProject.isDisposed() && myVcsManager.getVcsFor(filePath) == myVcs);
  }

  @Nonnull
  private static FilePath getEventFilePath(@Nonnull VFileEvent event) {
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

  private boolean allowedDeletion(@Nonnull VFileEvent event) {
    if (myVcsFileListenerContextHelper.isDeletionContextEmpty()) return true;

    return !myVcsFileListenerContextHelper.isDeletionIgnored(getEventFilePath(event));
  }

  private boolean allowedAddition(@Nonnull VFileEvent event) {
    if (myVcsFileListenerContextHelper.isAdditionContextEmpty()) return true;

    return !myVcsFileListenerContextHelper.isAdditionIgnored(getEventFilePath(event));
  }

  protected void executeAdd() {
    List<VirtualFile> addedFiles = myProcessor.acquireAddedFiles();
    Map<VirtualFile, VirtualFile> copyFromMap = myProcessor.acquireCopiedFiles();
    LOG.debug("executeAdd. addedFiles: ", addedFiles);

    VcsShowConfirmationOption.Value addOption = myAddOption.getValue();
    if (addOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) return;

    addedFiles.removeIf(myVcsIgnoreManager::isPotentiallyIgnoredFile);
    if (addedFiles.isEmpty()) return;

    executeAdd(addedFiles, copyFromMap);
  }

  protected void executeAdd(@Nonnull List<VirtualFile> addedFiles, @Nonnull Map<VirtualFile, VirtualFile> copyFromMap) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      // backward compatibility with plugins
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        performAddingWithConfirmation(addedFiles, copyFromMap);
      });
    }
    else {
      performAddingWithConfirmation(addedFiles, copyFromMap);
    }
  }

  /**
   * Execute add that performs adding from specific collections
   *
   * @param addedFiles  the added files
   * @param copyFromMap the copied files
   */
  protected void performAddingWithConfirmation(@Nonnull List<VirtualFile> addedFiles, @Nonnull Map<VirtualFile, VirtualFile> copyFromMap) {
    VcsShowConfirmationOption.Value addOption = myAddOption.getValue();
    LOG.debug("executeAdd. add-option: ", addOption, ", files to add: ", addedFiles);
    if (addOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) return;

    addedFiles = myProjectConfigurationFilesProcessor.filterNotProjectConfigurationFiles(addedFiles);

    List<VirtualFile> filesToProcess = selectFilesToAdd(addedFiles);
    if (filesToProcess.isEmpty()) return;

    performAdding(filesToProcess, copyFromMap);
  }

  private void executeMoveRename() {
    List<MovedFileInfo> movedFiles = myProcessor.acquireMovedFiles();
    LOG.debug("executeMoveRename ", movedFiles);
    if (movedFiles.isEmpty()) return;

    performMoveRename(movedFiles);
  }

  protected void executeDelete() {
    List<FilePath> deletedFiles = myProcessor.acquireDeletedFiles();
    LOG.debug("executeDelete ", deletedFiles);

    VcsShowConfirmationOption.Value removeOption = myRemoveOption.getValue();
    if (removeOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) return;

    deletedFiles.removeIf(myVcsIgnoreManager::isPotentiallyIgnoredFile);

    List<FilePath> filesToProcess = selectFilePathsToDelete(deletedFiles);
    if (filesToProcess.isEmpty()) return;

    performDeletion(filesToProcess);
  }

  protected void saveUnsavedVcsIgnoreFiles() {
    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    Set<String> ignoreFileNames = VcsUtil.getVcsIgnoreFileNames(myProject);

    for (Document document : fileDocumentManager.getUnsavedDocuments()) {
      VirtualFile file = fileDocumentManager.getFile(document);
      if (file != null && ignoreFileNames.contains(file.getName())) {
        ApplicationManager.getApplication().invokeAndWait(() -> fileDocumentManager.saveDocument(document));
      }
    }
  }

  /**
   * Select file paths to delete
   *
   * @param deletedFiles deleted files set
   * @return selected files or empty if {@link VcsShowConfirmationOption.Value#DO_NOTHING_SILENTLY}
   */
  protected @Nonnull List<FilePath> selectFilePathsToDelete(@Nonnull List<FilePath> deletedFiles) {
    return selectFilePathsForOption(myRemoveOption, deletedFiles, getDeleteTitle(), getSingleFileDeleteTitle(),
                                    getSingleFileDeletePromptTemplate(),
                                    CommonBundle.message("button.delete"), CommonBundle.getCancelButtonText());
  }

  /**
   * Same as {@link #selectFilePathsToDelete} but for add operation
   *
   * @param addFiles added files set
   * @return selected files or empty if {@link VcsShowConfirmationOption.Value#DO_NOTHING_SILENTLY}
   */
  protected @Nonnull List<FilePath> selectFilePathsToAdd(@Nonnull List<FilePath> addFiles) {
    return selectFilePathsForOption(myAddOption, addFiles, getAddTitle(), getSingleFileAddTitle(), getSingleFileAddPromptTemplate(),
                                    CommonBundle.getAddButtonText(), CommonBundle.getCancelButtonText());
  }

  protected @Nonnull List<VirtualFile> selectFilesToAdd(@Nonnull List<VirtualFile> addFiles) {
    return selectFilesForOption(myAddOption, addFiles, getAddTitle(), getSingleFileAddTitle(), getSingleFileAddPromptTemplate());
  }

  private @Nonnull List<FilePath> selectFilePathsForOption(@Nonnull VcsShowConfirmationOption option,
                                                           @Nonnull List<FilePath> files,
                                                           String title,
                                                           String singleFileTitle,
                                                           String singleFilePromptTemplate,
                                                           @Nullable String okActionName,
                                                           @Nullable String cancelActionName) {
    VcsShowConfirmationOption.Value optionValue = option.getValue();
    if (optionValue == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
      return emptyList();
    }
    if (optionValue == VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
      return files;
    }
    if (files.isEmpty()) {
      return emptyList();
    }

    AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
    Ref<Collection<FilePath>> ref = Ref.create();
    ApplicationManager.getApplication()
                      .invokeAndWait(() -> ref.set(helper.selectFilePathsToProcess(files,
                                                                                   title,
                                                                                   null,
                                                                                   singleFileTitle,
                                                                                   singleFilePromptTemplate,
                                                                                   option,
                                                                                   okActionName,
                                                                                   cancelActionName)));
    Collection<FilePath> selectedFilePaths = ref.get();
    return selectedFilePaths != null ? new ArrayList<>(selectedFilePaths) : emptyList();
  }

  private @Nonnull List<VirtualFile> selectFilesForOption(@Nonnull VcsShowConfirmationOption option,
                                                          @Nonnull List<VirtualFile> files,
                                                          String title,
                                                          String singleFileTitle,
                                                          String singleFilePromptTemplate) {
    VcsShowConfirmationOption.Value optionValue = option.getValue();
    if (optionValue == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
      return emptyList();
    }
    if (optionValue == VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
      return files;
    }
    if (files.isEmpty()) {
      return emptyList();
    }

    AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
    Ref<Collection<VirtualFile>> ref = Ref.create();
    ApplicationManager.getApplication()
                      .invokeAndWait(() -> ref.set(helper.selectFilesToProcess(files, title, null, singleFileTitle,
                                                                               singleFilePromptTemplate, option)));
    Collection<VirtualFile> selectedFiles = ref.get();
    return selectedFiles != null ? new ArrayList<>(selectedFiles) : emptyList();
  }

  /**
   * @return whether {@link #beforeContentsChange} is overridden.
   */
  protected boolean processBeforeContentsChange() {
    return false;
  }

  /**
   * This is a very expensive operation and shall be avoided whenever possible.
   *
   * @see #processBeforeContentsChange()
   */
  @RequiredUIAccess
  protected void beforeContentsChange(@Nonnull List<VFileContentChangeEvent> events) {
  }

  @RequiredUIAccess
  protected void processMovedFile(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
  }

  /**
   * Determine if the listener should not process files with the given status.
   */
  protected boolean filterOutByStatus(@Nonnull FileStatus status) {
    return status == FileStatus.IGNORED || status == FileStatus.UNKNOWN;
  }

  protected boolean shouldIgnoreDeletion(@Nonnull FileStatus status) {
    return false;
  }

  @Nonnull
  protected abstract String getAddTitle();

  @Nonnull
  protected abstract String getSingleFileAddTitle();

  @Nonnull
  protected abstract String getSingleFileAddPromptTemplate();

  @Nonnull
  protected abstract String getDeleteTitle();

  protected abstract String getSingleFileDeleteTitle();

  protected abstract String getSingleFileDeletePromptTemplate();

  protected abstract void performAdding(@Nonnull Collection<VirtualFile> addedFiles, @Nonnull Map<VirtualFile, VirtualFile> copyFromMap);

  protected abstract void performDeletion(@Nonnull List<FilePath> filesToDelete);

  protected abstract void performMoveRename(@Nonnull List<MovedFileInfo> movedFiles);

  protected boolean isRecursiveDeleteSupported() {
    return false;
  }

  protected boolean isFileCopyingFromTrackingSupported() {
    return true;
  }

  @SuppressWarnings("unchecked")
  private ExternallyAddedFilesProcessorImpl createExternalFilesProcessor() {
    return new ExternallyAddedFilesProcessorImpl(myProject,
                                                 this,
                                                 myVcs,
                                                 (files) -> {
                                                   performAdding((Collection<VirtualFile>)files, emptyMap());
                                                 });
  }

  @SuppressWarnings("unchecked")
  private ProjectConfigurationFilesProcessorImpl createProjectConfigurationFilesProcessor() {
    return new ProjectConfigurationFilesProcessorImpl(myProject,
                                                      this,
                                                      myVcs,
                                                      (files) -> {
                                                        performAdding((Collection<VirtualFile>)files, emptyMap());
                                                      });
  }

  private IgnoreFilesProcessorImpl createIgnoreFilesProcessor() {
    return new IgnoreFilesProcessorImpl(myProject, this, myVcs);
  }

  private class MyAsyncVfsListener implements AsyncFileListener {

    private static boolean isBeforeEvent(@Nonnull VFileEvent event) {
      return event instanceof VFileDeleteEvent
        || event instanceof VFileMoveEvent
        || event instanceof VFilePropertyChangeEvent;
    }

    private static boolean isAfterEvent(@Nonnull VFileEvent event) {
      return event instanceof VFileCreateEvent
        || event instanceof VFileCopyEvent
        || event instanceof VFileMoveEvent;
    }

    @Nullable
    @Override
    public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
      List<VFileContentChangeEvent> contentChangedEvents = new ArrayList<>();
      List<VFileEvent> beforeEvents = new ArrayList<>();
      List<VFileEvent> afterEvents = new ArrayList<>();
      for (VFileEvent event : events) {
        ProgressManager.checkCanceled();
        if (event instanceof VFileContentChangeEvent contentChangeEvent) {
          if (processBeforeContentsChange()) {
            VirtualFile file = contentChangeEvent.getFile();
            if (isUnderMyVcs(file)) {
              contentChangedEvents.add(contentChangeEvent);
            }
          }
        }
        else if (isEventAccepted(event)) {
          if (isBeforeEvent(event)) {
            beforeEvents.add(event);
          }
          if (isAfterEvent(event)) {
            afterEvents.add(event);
          }
        }
      }
      if (contentChangedEvents.isEmpty() && beforeEvents.isEmpty() && afterEvents.isEmpty()) {
        return null;
      }
      return new ChangeApplier() {
        @Override
        public void beforeVfsChange() {
          beforeContentsChange(contentChangedEvents);

          myProcessor.processBeforeEvents(beforeEvents);
        }

        @Override
        public void afterVfsChange() {
          myEventsToProcess.addAll(afterEvents);
        }
      };
    }
  }

  private class MyCommandAdapter implements CommandListener {

    @Override
    public void commandFinished(@Nonnull CommandEvent event) {
      if (myProject != event.getProject()) return;

      /*
       * Create file events cannot be filtered in afterVfsChange since VcsFileListenerContextHelper populated after actual file creation in PathsVerifier.CheckAdded.check
       * So this commandFinished is the only way to get in sync with VcsFileListenerContextHelper to check if additions need to be filtered.
       */
      List<VFileEvent> afterEvents = ContainerUtil.filter(myEventsToProcess, e -> !(e instanceof VFileCreateEvent) || allowedAddition(e));
      myEventsToProcess.clear();

      if (afterEvents.isEmpty() && !myProcessor.isAnythingToProcess()) return;

      processEventsInBackground(afterEvents);
    }

    /**
     * Not using modal progress here, because it could lead to some focus related assertion (e.g. "showing dialogs from popup" in com.intellij.ui.popup.tree.TreePopupImpl)
     * Assume, that it is a safe to do all processing in background even if "Add to VCS" dialog may appear during such processing.
     */
    private void processEventsInBackground(List<? extends VFileEvent> events) {
      new Task.Backgroundable(myProject, VcsBundle.message("progress.title.version.control.processing.changed.files"), true) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          try {
            indicator.checkCanceled();
            myProcessor.processAfterEvents(events);
            myProcessor.executePendingTasks();
          }
          catch (ProcessCanceledException e) {
            myProcessor.clearAllPendingTasks();
          }
        }
      }.queue();
    }
  }
}

