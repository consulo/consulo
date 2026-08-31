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
package consulo.language.index.impl.internal;

import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class VfsEventsMerger {
  private static final boolean DEBUG = false;
  //static final boolean DEBUG = (true);

  public void recordFileEvent(VirtualFile file, boolean contentChange) {
    if (DEBUG) System.out.println("Request build indices for file:" + file.getPath() + ", contentChange:" + contentChange);
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, contentChange ? FILE_CONTENT_CHANGED : FILE_ADDED);
  }

  public void recordFileRemovedEvent(VirtualFile file) {
    if (DEBUG) System.out.println("Request invalidate indices for file:" + file.getPath() + ", deletion");
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, FILE_REMOVED);
  }

  public void recordTransientStateChangeEvent(VirtualFile file) {
    if (DEBUG) System.out.println("Transient state changed for file:" + file.getPath());
    updateChange(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file), file, FILE_TRANSIENT_STATE_CHANGED);
  }

  private final AtomicInteger myPublishedEventIndex = new AtomicInteger();

  int getPublishedEventIndex() {
    return myPublishedEventIndex.get();
  }

  // NB: this code is executed not only during vfs events dispatch (in write action) but also during requestReindex (in read action)
  private void updateChange(int fileId, VirtualFile file, @EventMask short mask) {
    while (true) { // CAS-like loop:
      ChangeInfo existingChangeInfo = myChangeInfos.get(fileId);
      if (existingChangeInfo != null && existingChangeInfo.eventMask == mask) {
        return; // nothing to update
      }

      ChangeInfo newChangeInfo = new ChangeInfo(file, mask, existingChangeInfo);
      if (existingChangeInfo == null) { // .replace() impl doesn't support oldValue=null, hence the branch:
        if (myChangeInfos.putIfAbsent(fileId, newChangeInfo) == null) {
          break;
        }
      }
      else {
        if (myChangeInfos.replace(fileId, existingChangeInfo, newChangeInfo)) {
          break;
        }
      }
    }
    myPublishedEventIndex.incrementAndGet();
  }

  @FunctionalInterface
  public interface VfsEventProcessor {
    /** Prepares a change before it is removed from the merger: this phase MAY be cancellable */
    default void prepare(ChangeInfo changeInfo) {
    }

    /** Applies a change in some way: this method MUST NOT be cancellable */
    boolean process(ChangeInfo changeInfo);

    /** this is a helper method that designates the end of the events batch, can be used for optimizations */
    default void endBatch() {
    }
  }

  /**
   * 1. Method can be invoked in several threads.
   * 2. Method processes the snapshot of available events at the time of the invocation: it means that if events are produced
   *    concurrently with their processing, then the set of events _could_ be non-empty after the method terminates.
   * 3. Method itself regularly checks for cancellations (thus _can_ finish with PCEs), but event {@code eventProcessor.process()}
   *    should process the change info atomically (i.e. without PCE)
   */
  public boolean processChanges(VfsEventProcessor eventProcessor) {
    if (!myChangeInfos.isEmpty()) {
      int[] fileIds = myChangeInfos.keySet().toIntArray(); // snapshot of the keys
      for (int fileId : fileIds) {
        ProgressManager.checkCanceled();

        ChangeInfo info = myChangeInfos.get(fileId);
        if (info == null) continue;

        // Keep the change in myChangeInfos while the cancellable preparation is running.
        eventProcessor.prepare(info);
        // If a newer event is recorded concurrently -> the identity check fails and the newer change stays queued.
        if (!myChangeInfos.remove(fileId, info)) continue;

        try {
          if (DEBUG) System.out.println("Processing " + info);
          if (!eventProcessor.process(info)) {
            eventProcessor.endBatch();
            return false;
          }
        }
        catch (ProcessCanceledException pce) {
          // it should be no PCE here -- eventProcessor.process()/.endBatch() should
          // be 'atomic': a change is either processed, or not, so throwing PCE from inside
          // the processor is an error
          FileBasedIndexImpl.LOG.error(new RuntimeException(pce));
          assert false;
        }
      }
      // endBatch() is a logical end of _successful_ batch => shouldn't be in 'finally'
      eventProcessor.endBatch();
    }
    return true;
  }

  public boolean hasChanges() {
    return !myChangeInfos.isEmpty();
  }

  public int getApproximateChangesCount() {
    return myChangeInfos.size();
  }

  
  Stream<VirtualFile> getChangedFiles() {
    return myChangeInfos.values().stream().map(ChangeInfo::getFile);
  }

  private final ConcurrentIntObjectMap<ChangeInfo> myChangeInfos = IntMaps.newConcurrentIntObjectHashMap();

  private static final short FILE_ADDED = 1;
  private static final short FILE_REMOVED = 2;
  private static final short FILE_CONTENT_CHANGED = 4;
  private static final short FILE_TRANSIENT_STATE_CHANGED = 8;

  @MagicConstant(flags = {FILE_ADDED, FILE_REMOVED, FILE_CONTENT_CHANGED, FILE_TRANSIENT_STATE_CHANGED})
  @interface EventMask {
  }

  public static final class ChangeInfo {
    private final VirtualFile file;

    @EventMask
    private final short eventMask;

    ChangeInfo(VirtualFile file, @EventMask short eventMask, @Nullable ChangeInfo previous) {
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
      if ((eventMask & FILE_CONTENT_CHANGED) != 0) builder.append("UPDATE ");
      if ((eventMask & FILE_REMOVED) != 0) builder.append("REMOVE ");
      if ((eventMask & FILE_ADDED) != 0) builder.append("ADD ");
      return builder.toString().trim();
    }

    public boolean isContentChanged() {
      return (eventMask & FILE_CONTENT_CHANGED) != 0;
    }

    public boolean isFileRemoved() {
      return (eventMask & FILE_REMOVED) != 0;
    }

    public boolean isFileAdded() {
      return (eventMask & FILE_ADDED) != 0;
    }

    public boolean isTransientStateChanged() {
      return (eventMask & FILE_TRANSIENT_STATE_CHANGED) != 0;
    }

    public VirtualFile getFile() {
      return file;
    }

    public int getFileId() {
      int fileId = FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file);
      if (fileId < 0) fileId = -fileId;
      return fileId;
    }
  }
}
