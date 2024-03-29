/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import org.intellij.lang.annotations.MagicConstant;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class VfsEventsMerger {
  private static final boolean DEBUG = false;
  //static final boolean DEBUG = (true);

  void recordFileEvent(@Nonnull VirtualFile file, boolean contentChange) {
    if (DEBUG) System.out.println("Request build indices for file:" + file.getPath() + ", contentChange:" + contentChange);
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, contentChange ? FILE_CONTENT_CHANGED : FILE_ADDED);
  }

  void recordBeforeFileEvent(@Nonnull VirtualFile file, boolean contentChanged) {
    if (DEBUG) System.out.println("Request invalidate indices for file:" + file.getPath() + ", contentChange:" + contentChanged);
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, contentChanged ? BEFORE_FILE_CONTENT_CHANGED : FILE_REMOVED);
  }

  void recordTransientStateChangeEvent(@Nonnull VirtualFile file) {
    if (DEBUG) System.out.println("Transient state changed for file:" + file.getPath());
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, FILE_TRANSIENT_STATE_CHANGED);
  }

  private final AtomicInteger myPublishedEventIndex = new AtomicInteger();

  int getPublishedEventIndex() {
    return myPublishedEventIndex.get();
  }

  // NB: this code is executed not only during vfs events dispatch (in write action) but also during requestReindex (in read action)
  private void updateChange(int fileId, @Nonnull VirtualFile file, @EventMask short mask) {
    while (true) {
      ChangeInfo existingChangeInfo = myChangeInfos.get(fileId);
      ChangeInfo newChangeInfo = new ChangeInfo(file, mask, existingChangeInfo);
      if (myChangeInfos.put(fileId, newChangeInfo) == existingChangeInfo) {
        myPublishedEventIndex.incrementAndGet();
        break;
      }
    }
  }

  void applyMergedEvents(@Nonnull VfsEventsMerger merger) {
    for (ChangeInfo info : merger.myChangeInfos.values()) {
      updateChange(info.getFileId(), info.file, info.eventMask);
    }
  }

  @FunctionalInterface
  public interface VfsEventProcessor {
    boolean process(@Nonnull ChangeInfo changeInfo);
  }

  // 1. Method can be invoked in several threads
  // 2. Method processes snapshot of available events at the time of the invokation, it does mean that if events are produced concurrently
  // with the processing then set of events will be not empty
  // 3. Method regularly checks for cancellations (thus can finish with PCEs) but event processor should process the change info atomically
  // (without PCE)
  boolean processChanges(@Nonnull VfsEventProcessor eventProcessor) {
    if (!myChangeInfos.isEmpty()) {
      int[] fileIds = myChangeInfos.keys(); // snapshot of the keys
      for (int fileId : fileIds) {
        ProgressManager.checkCanceled();
        ChangeInfo info = myChangeInfos.remove(fileId);
        if (info == null) continue;

        try {
          if (DEBUG) System.out.println("Processing " + info);
          if (!eventProcessor.process(info)) return false;
        }
        catch (ProcessCanceledException pce) { // todo remove
          FileBasedIndexImpl.LOG.error(pce);
          assert false;
        }
      }
    }
    return true;
  }

  boolean hasChanges() {
    return !myChangeInfos.isEmpty();
  }

  int getApproximateChangesCount() {
    return myChangeInfos.size();
  }

  @Nonnull
  Stream<VirtualFile> getChangedFiles() {
    return myChangeInfos.values().stream().map(ChangeInfo::getFile);
  }

  private final ConcurrentIntObjectMap<ChangeInfo> myChangeInfos = ContainerUtil.createConcurrentIntObjectMap();

  private static final short FILE_ADDED = 1;
  private static final short FILE_REMOVED = 2;
  private static final short FILE_CONTENT_CHANGED = 4;
  private static final short BEFORE_FILE_CONTENT_CHANGED = 8;
  private static final short FILE_TRANSIENT_STATE_CHANGED = 16;

  @MagicConstant(flags = {FILE_ADDED, FILE_REMOVED, FILE_CONTENT_CHANGED, BEFORE_FILE_CONTENT_CHANGED, FILE_TRANSIENT_STATE_CHANGED})
  @interface EventMask {
  }

  static class ChangeInfo {
    private final VirtualFile file;

    @EventMask
    private final short eventMask;

    ChangeInfo(@Nonnull VirtualFile file, @EventMask short eventMask, @Nullable ChangeInfo previous) {
      this.file = file;
      this.eventMask = mergeEventMask(previous == null ? 0 : previous.eventMask, eventMask);
    }

    @EventMask
    private static short mergeEventMask(@EventMask short existingOperation, @EventMask short newOperation) {
      if (newOperation == FILE_REMOVED) {
        return FILE_REMOVED;
      }
      return (short)(existingOperation | newOperation);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("file: ").append(file.getPath()).append("\n").append("operation: ");
      if ((eventMask & FILE_TRANSIENT_STATE_CHANGED) != 0) builder.append("TRANSIENT_STATE_CHANGE ");
      if ((eventMask & BEFORE_FILE_CONTENT_CHANGED) != 0) builder.append("UPDATE-REMOVE ");
      if ((eventMask & FILE_CONTENT_CHANGED) != 0) builder.append("UPDATE ");
      if ((eventMask & FILE_REMOVED) != 0) builder.append("REMOVE ");
      if ((eventMask & FILE_ADDED) != 0) builder.append("ADD ");
      return builder.toString().trim();
    }

    boolean isBeforeContentChanged() {
      return (eventMask & BEFORE_FILE_CONTENT_CHANGED) != 0;
    }

    boolean isContentChanged() {
      return (eventMask & FILE_CONTENT_CHANGED) != 0;
    }

    boolean isFileRemoved() {
      return (eventMask & FILE_REMOVED) != 0;
    }

    boolean isFileAdded() {
      return (eventMask & FILE_ADDED) != 0;
    }

    boolean isTransientStateChanged() {
      return (eventMask & FILE_TRANSIENT_STATE_CHANGED) != 0;
    }

    @Nonnull
    VirtualFile getFile() {
      return file;
    }

    int getFileId() {
      int fileId = FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file);
      if (fileId < 0) fileId = -fileId;
      return fileId;
    }
  }
}
