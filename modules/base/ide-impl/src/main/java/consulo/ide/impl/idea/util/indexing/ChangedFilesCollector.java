/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.util.indexing;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.application.util.registry.Registry;
import consulo.content.ContentIterator;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexableFileSet;
import consulo.localHistory.LocalHistory;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

@ExtensionImpl
final class ChangedFilesCollector extends IndexedFilesListener {
  final IntObjectMap<VirtualFile> myFilesToUpdate = ContainerUtil.createConcurrentIntObjectMap();
  private final AtomicInteger myProcessedEventIndex = new AtomicInteger();
  private final Phaser myWorkersFinishedSync = new Phaser() {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
      return false;
    }
  };

  final Executor myVfsEventsExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FileBasedIndex Vfs Event Processor");
  private final AtomicInteger myScheduledVfsEventsWorkers = new AtomicInteger();
  private final FileBasedIndexImpl myManager = (FileBasedIndexImpl)FileBasedIndex.getInstance();

  @Inject
  ChangedFilesCollector() {
  }

  @Override
  protected void buildIndicesForFileRecursively(@Nonnull VirtualFile file, boolean contentChange) {
    FileBasedIndexImpl.cleanProcessedFlag(file);
    if (!contentChange) {
      myManager.myUpdatingFiles.incrementAndGet();
    }

    super.buildIndicesForFileRecursively(file, contentChange);

    if (!contentChange) {
      if (myManager.myUpdatingFiles.decrementAndGet() == 0) {
        myManager.myFilesModCount.incrementAndGet();
      }
    }
  }

  @Override
  protected void iterateIndexableFiles(@Nonnull VirtualFile file, @Nonnull ContentIterator iterator) {
    for (IndexableFileSet set : myManager.myIndexableSets) {
      if (set.isInSet(file)) {
        set.iterateIndexableFilesIn(file, iterator);
      }
    }
  }

  void scheduleForUpdate(VirtualFile file) {
    if (!(file instanceof DeletedVirtualFileStub)) {
      IndexableFileSet setForFile = myManager.getIndexableSetForFile(file);
      if (setForFile == null) {
        return;
      }
    }
    final int fileId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
    final VirtualFile previousVirtualFile = myFilesToUpdate.put(fileId, file);

    if (previousVirtualFile instanceof DeletedVirtualFileStub && !previousVirtualFile.equals(file)) {
      assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
      ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(true);
      myFilesToUpdate.put(fileId, previousVirtualFile);
    }
  }

  void removeScheduledFileFromUpdate(VirtualFile file) {
    final int fileId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
    final VirtualFile previousVirtualFile = myFilesToUpdate.remove(fileId);

    if (previousVirtualFile instanceof DeletedVirtualFileStub) {
      assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
      ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(false);
      myFilesToUpdate.put(fileId, previousVirtualFile);
    }
  }

  void removeFileIdFromFilesScheduledForUpdate(int fileId) {
    myFilesToUpdate.remove(fileId);
  }

  Collection<VirtualFile> getAllFilesToUpdate() {
    ensureUpToDate();
    if (myFilesToUpdate.isEmpty()) {
      return Collections.emptyList();
    }
    return new ArrayList<>(myFilesToUpdate.values());
  }

  @Override
  @Nonnull
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    boolean shouldCleanup = ContainerUtil.exists(events, ChangedFilesCollector::memoryStorageCleaningNeeded);
    ChangeApplier superApplier = super.prepareChange(events);

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        if (shouldCleanup) {
          myManager.cleanupMemoryStorage(false);
        }
        superApplier.beforeVfsChange();
      }

      @Override
      public void afterVfsChange() {
        superApplier.afterVfsChange();
        if (myManager.myInitialized) ensureUpToDateAsync();
      }
    };
  }

  private static boolean memoryStorageCleaningNeeded(@Nonnull VFileEvent event) {
    Object requestor = event.getRequestor();
    return requestor instanceof FileDocumentManager || requestor instanceof PsiManager || requestor == LocalHistory.VFS_EVENT_REQUESTOR;
  }

  boolean isScheduledForUpdate(VirtualFile file) {
    return myFilesToUpdate.containsKey(Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file)));
  }

  void ensureUpToDate() {
    if (!FileBasedIndexImpl.isUpToDateCheckEnabled()) {
      return;
    }
    //assert ApplicationManager.getApplication().isReadAccessAllowed() || ShutDownTracker.isShutdownHookRunning();
    myManager.waitUntilIndicesAreInitialized();

    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      processFilesInReadAction(null);
    }
    else {
      processFilesInReadActionWithYieldingToWriteAction();
    }
  }

  void ensureUpToDateAsync() {
    if (getEventMerger().getApproximateChangesCount() >= 20 && myScheduledVfsEventsWorkers.compareAndSet(0, 1)) {
      myVfsEventsExecutor.execute(() -> {
        try {
          processFilesInReadActionWithYieldingToWriteAction();
        }
        finally {
          myScheduledVfsEventsWorkers.decrementAndGet();
        }
      });

      if (Registry.is("try.starting.dumb.mode.where.many.files.changed")) {
        Runnable startDumbMode = () -> {
          for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            DumbService dumbService = DumbService.getInstance(project);
            DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(project);

            if (task != null) {
              dumbService.queueTask(task);
            }
          }
        };

        Application app = ApplicationManager.getApplication();
        if (!app.isHeadlessEnvironment()  /*avoid synchronous ensureUpToDate to prevent deadlock*/ && app.isDispatchThread() && !LaterInvocator.isInModalContext()) {
          startDumbMode.run();
        }
        else {
          app.invokeLater(startDumbMode, IdeaModalityState.nonModal());
        }
      }
    }
  }

  private void processFilesInReadAction(Project project) {
    assert ApplicationManager.getApplication().isReadAccessAllowed(); // no vfs events -> event processing code can finish

    int publishedEventIndex = getEventMerger().getPublishedEventIndex();
    int processedEventIndex = myProcessedEventIndex.get();
    if (processedEventIndex == publishedEventIndex) {
      return;
    }

    myWorkersFinishedSync.register();
    int phase = myWorkersFinishedSync.getPhase();
    try {
      getEventMerger().processChanges(info -> ConcurrencyUtil.withLock(myManager.myWriteLock, () -> {
        try {
          ProgressManager.getInstance().executeNonCancelableSection(() -> {
            int fileId = info.getFileId();
            VirtualFile file = info.getFile();
            if (info.isTransientStateChanged()) myManager.doTransientStateChangeForFile(fileId, file);
            if (info.isBeforeContentChanged()) myManager.doInvalidateIndicesForFile(fileId, file, true);
            if (info.isContentChanged()) myManager.scheduleFileForIndexing(project, fileId, file, true);
            if (info.isFileRemoved()) myManager.doInvalidateIndicesForFile(fileId, file, false);
            if (info.isFileAdded()) myManager.scheduleFileForIndexing(project, fileId, file, false);
          });
        }
        finally {
          IndexingStamp.flushCache(info.getFileId());
        }
        return true;
      }));
    }
    finally {
      myWorkersFinishedSync.arriveAndDeregister();
    }

    myWorkersFinishedSync.awaitAdvance(phase);

    if (getEventMerger().getPublishedEventIndex() == publishedEventIndex) {
      myProcessedEventIndex.compareAndSet(processedEventIndex, publishedEventIndex);
    }
  }

  private void processFilesInReadActionWithYieldingToWriteAction() {
    while (getEventMerger().hasChanges()) {
      if (!ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> processFilesInReadAction(null))) {
        ProgressIndicatorUtils.yieldToPendingWriteActions();
      }
    }
  }
}
