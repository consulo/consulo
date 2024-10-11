/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.versionControlSystem;

import consulo.annotation.UsedInPlugin;
import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.VcsIgnoreManager;
import consulo.versionControlSystem.internal.DiryFilesStateProcessor;
import consulo.versionControlSystem.internal.VcsFileListenerContextHelper;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public abstract class VcsVFSListener implements Disposable {
    public static final class MovedFileInfo {
        public final String myOldPath;
        public String myNewPath;
        private final VirtualFile myFile;

        public MovedFileInfo(VirtualFile file, final String newPath) {
            myOldPath = file.getPath();
            myNewPath = newPath;
            myFile = file;
        }

        @Override
        public String toString() {
            return "MovedFileInfo{myNewPath=" + myNewPath + ", myFile=" + myFile + '}';
        }

        public String getOldPath() {
            return myOldPath;
        }

        public String getNewPath() {
            return myNewPath;
        }

        public void setNewPath(String newPath) {
            myNewPath = newPath;
        }

        public VirtualFile getFile() {
            return myFile;
        }
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
                    if (processBeforeContentsChange() && isUnderMyVcs(contentChangeEvent.getFile())) {
                        contentChangedEvents.add(contentChangeEvent);
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
        public void commandFinished(final CommandEvent event) {
            if (myProject != event.getProject()) {
                return;
            }

            /*
             * Create file events cannot be filtered in afterVfsChange since VcsFileListenerContextHelper populated after actual file creation in PathsVerifier.CheckAdded.check
             * So this commandFinished is the only way to get in sync with VcsFileListenerContextHelper to check if additions need to be filtered.
             */
            List<VFileEvent> afterEvents =
                ContainerUtil.filter(myEventsToProcess, e -> !(e instanceof VFileCreateEvent) || myProcessor.allowedAddition(e));
            myEventsToProcess.clear();

            if (afterEvents.isEmpty() && !myProcessor.isAnythingToProcess()) {
                return;
            }

            processEventsInBackground(afterEvents);
        }

        /**
         * Not using modal progress here, because it could lead to some focus related assertion
         * (e.g. "showing dialogs from popup" in com.intellij.ui.popup.tree.TreePopupImpl)
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

    protected static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

    private final ProjectLevelVcsManager myVcsManager;
    private final VcsIgnoreManager myVcsIgnoreManager;

    protected final Project myProject;
    protected final AbstractVcs myVcs;
    protected final ChangeListManager myChangeListManager;
    protected final VcsShowConfirmationOption myAddOption;
    protected final VcsShowConfirmationOption myRemoveOption;

    private final List<VFileEvent> myEventsToProcess = new SmartList<>();

    protected enum VcsDeleteType {
        SILENT,
        CONFIRM,
        IGNORE
    }

    private final DiryFilesStateProcessor myProcessor;

    protected VcsVFSListener(@Nonnull Project project, @Nonnull AbstractVcs vcs) {
        myProject = project;
        myVcs = vcs;
        myChangeListManager = ChangeListManager.getInstance(project);
        myVcsIgnoreManager = VcsIgnoreManager.getInstance(project);

        myVcsManager = ProjectLevelVcsManager.getInstance(project);
        myAddOption = myVcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, vcs);
        myRemoveOption = myVcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, vcs);

        MessageBusConnection busConnection = project.getApplication().getMessageBus().connect(this);
        busConnection.subscribe(CommandListener.class, new MyCommandAdapter());

        VirtualFileManager.getInstance().addAsyncFileListener(new MyAsyncVfsListener(), this);

        myProcessor = new DiryFilesStateProcessor(myChangeListManager, VcsFileListenerContextHelper.getInstance(project)) {
            @Override
            protected boolean isFileCopyingFromTrackingSupported() {
                return VcsVFSListener.this.isFileCopyingFromTrackingSupported();
            }

            @Override
            protected boolean isRecursiveDeleteSupported() {
                return VcsVFSListener.this.isRecursiveDeleteSupported();
            }

            @Override
            protected void processMovedFile(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
                VcsVFSListener.this.processMovedFile(file, newParentPath, newName);

                super.processMovedFile(file, newParentPath, newName);
            }

            @Override
            protected boolean isEventIgnored(@Nonnull VFileEvent event) {
                return VcsVFSListener.this.isEventIgnored(event);
            }

            @Override
            protected boolean filterOutByStatus(@Nonnull FileStatus status) {
                return VcsVFSListener.this.filterOutByStatus(status);
            }

            @Override
            protected boolean shouldIgnoreDeletion(@Nonnull FileStatus status) {
                return VcsVFSListener.this.shouldIgnoreDeletion(status);
            }

            @Override
            protected boolean isUnderMyVcs(@Nullable VirtualFile file) {
                return VcsVFSListener.this.isUnderMyVcs(file);
            }

            @Override
            protected void executePendingTasksImpl() {
                executeAdd();
                executeDelete();
                executeMoveRename();
            }
        };
    }

    @Override
    public void dispose() {
    }

    protected boolean isEventIgnored(@Nonnull VFileEvent event) {
        FilePath filePath = DiryFilesStateProcessor.getEventFilePath(event);
        return !isUnderMyVcs(filePath) || myChangeListManager.isIgnoredFile(filePath);
    }

    protected boolean isUnderMyVcs(@Nullable VirtualFile file) {
        return file != null && ReadAction.compute(() -> !myProject.isDisposed() && myVcsManager.getVcsFor(file) == myVcs);
    }

    protected boolean isUnderMyVcs(@Nullable FilePath filePath) {
        return filePath != null && ReadAction.compute(() -> !myProject.isDisposed() && myVcsManager.getVcsFor(filePath) == myVcs);
    }

    protected boolean isEventAccepted(@Nonnull VFileEvent event) {
        return !event.isFromRefresh() && (event.getFileSystem() instanceof LocalFileSystem);
    }

    /**
     * Determine if the listener should not process files with the given status.
     */
    protected boolean filterOutByStatus(@Nonnull FileStatus status) {
        return status == FileStatus.IGNORED || status == FileStatus.UNKNOWN;
    }

    /**
     * @return whether {@link #beforeContentsChange} is overridden.
     */
    protected boolean processBeforeContentsChange() {
        return false;
    }

    protected void executeAdd() {
        List<VirtualFile> addedFiles = myProcessor.acquireAddedFiles();
        Map<VirtualFile, VirtualFile> copyFromMap = myProcessor.acquireCopiedFiles();
        LOG.debug("executeAdd. addedFiles: ", addedFiles);

        VcsShowConfirmationOption.Value addOption = myAddOption.getValue();
        if (addOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return;
        }

        addedFiles.removeIf(myVcsIgnoreManager::isPotentiallyIgnoredFile);
        if (addedFiles.isEmpty()) {
            return;
        }

        executeAdd(addedFiles, copyFromMap);
    }

    protected void executeAdd(@Nonnull List<VirtualFile> addedFiles, @Nonnull Map<VirtualFile, VirtualFile> copyFromMap) {
        Application application = myProject.getApplication();

        if (application.isDispatchThread()) {
            application.executeOnPooledThread(() -> performAddingWithConfirmation(addedFiles, copyFromMap));
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
    protected void performAddingWithConfirmation(
        @Nonnull List<VirtualFile> addedFiles,
        @Nonnull Map<VirtualFile, VirtualFile> copyFromMap
    ) {
        VcsShowConfirmationOption.Value addOption = myAddOption.getValue();
        LOG.debug("executeAdd. add-option: ", addOption, ", files to add: ", addedFiles);
        if (addOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return;
        }

        // TODO ?addedFiles = myProjectConfigurationFilesProcessor.filterNotProjectConfigurationFiles(addedFiles);

        List<VirtualFile> filesToProcess = selectFilesToAdd(addedFiles);
        if (filesToProcess.isEmpty()) {
            return;
        }

        performAdding(filesToProcess, copyFromMap);
    }

    @Nonnull
    protected List<VirtualFile> selectFilesToAdd(@Nonnull List<VirtualFile> addFiles) {
        return selectFilesForOption(myAddOption, addFiles, getAddTitle(), getSingleFileAddTitle(), getSingleFileAddPromptTemplate());
    }

    @Nonnull
    private List<VirtualFile> selectFilesForOption(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull List<VirtualFile> files,
        String title,
        String singleFileTitle,
        String singleFilePromptTemplate
    ) {
        VcsShowConfirmationOption.Value optionValue = option.getValue();
        if (optionValue == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return List.of();
        }

        if (optionValue == VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
            return files;
        }
        if (files.isEmpty()) {
            return List.of();
        }

        Application application = myProject.getApplication();

        AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
        SimpleReference<Collection<VirtualFile>> ref = SimpleReference.create();
        application.invokeAndWait(
            () -> ref.set(helper.selectFilesToProcess(files, title, null, singleFileTitle, singleFilePromptTemplate, option))
        );
        Collection<VirtualFile> selectedFiles = ref.get();
        return selectedFiles != null ? new ArrayList<>(selectedFiles) : List.of();
    }

    @Nonnull
    private List<FilePath> selectFilePathsForOption(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull List<FilePath> files,
        String title,
        String singleFileTitle,
        String singleFilePromptTemplate,
        @Nullable String okActionName,
        @Nullable String cancelActionName
    ) {
        VcsShowConfirmationOption.Value optionValue = option.getValue();
        if (optionValue == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return List.of();
        }

        if (optionValue == VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
            return files;
        }

        if (files.isEmpty()) {
            return List.of();
        }

        Application application = myProject.getApplication();
        AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
        SimpleReference<Collection<FilePath>> ref = SimpleReference.create();
        application.invokeAndWait(() -> ref.set(helper.selectFilePathsToProcess(
            files,
            title,
            null,
            singleFileTitle,
            singleFilePromptTemplate,
            option,
            okActionName,
            cancelActionName
        )));
        Collection<FilePath> selectedFilePaths = ref.get();
        return selectedFilePaths != null ? new ArrayList<>(selectedFilePaths) : List.of();
    }

    protected void executeDelete() {
        List<FilePath> deletedFiles = acquireDeletedFiles();

        LOG.debug("executeDelete ", deletedFiles);

        VcsShowConfirmationOption.Value removeOption = myRemoveOption.getValue();
        if (removeOption == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return;
        }

        deletedFiles.removeIf(myVcsIgnoreManager::isPotentiallyIgnoredFile);

        List<FilePath> filesToProcess = selectFilePathsToDelete(deletedFiles);
        if (filesToProcess.isEmpty()) {
            return;
        }

        performDeletion(filesToProcess);
    }

    @UsedInPlugin
    protected final List<FilePath> acquireDeletedFiles() {
        return myProcessor.acquireDeletedFiles();
    }

    /**
     * Select file paths to delete
     *
     * @param deletedFiles deleted files set
     * @return selected files or null (that is considered as empty file set)
     */
    @Nonnull
    protected List<FilePath> selectFilePathsToDelete(final List<FilePath> deletedFiles) {
        return selectFilePathsForOption(
            myRemoveOption,
            deletedFiles,
            getDeleteTitle(),
            getSingleFileDeleteTitle(),
            getSingleFileDeletePromptTemplate(),
            CommonLocalize.buttonDelete().get(),
            CommonBundle.getCancelButtonText()
        );
    }


    /**
     * This is a very expensive operation and shall be avoided whenever possible.
     *
     * @see #processBeforeContentsChange()
     */
    protected void beforeContentsChange(@Nonnull List<VFileContentChangeEvent> events) {
    }

    protected void processMovedFile(@Nonnull VirtualFile file, @Nonnull String newParentPath, @Nonnull String newName) {
    }

    private void executeMoveRename() {
        List<MovedFileInfo> movedFiles = myProcessor.acquireMovedFiles();
        LOG.debug("executeMoveRename ", movedFiles);
        if (movedFiles.isEmpty()) {
            return;
        }

        performMoveRename(movedFiles);
    }

    protected boolean isRecursiveDeleteSupported() {
        return false;
    }

    protected boolean isFileCopyingFromTrackingSupported() {
        return true;
    }

    protected boolean shouldIgnoreDeletion(@Nonnull FileStatus status) {
        return false;
    }

    protected abstract String getAddTitle();

    protected abstract String getSingleFileAddTitle();

    protected abstract String getSingleFileAddPromptTemplate();

    protected abstract void performAdding(final Collection<VirtualFile> addedFiles, final Map<VirtualFile, VirtualFile> copyFromMap);

    protected abstract String getDeleteTitle();

    protected abstract String getSingleFileDeleteTitle();

    protected abstract String getSingleFileDeletePromptTemplate();

    protected abstract void performDeletion(List<FilePath> filesToDelete);

    protected abstract void performMoveRename(List<MovedFileInfo> movedFiles);
}
