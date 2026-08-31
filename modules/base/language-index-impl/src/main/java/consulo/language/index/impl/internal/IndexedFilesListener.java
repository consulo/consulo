// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.progress.ProgressManager;
import consulo.content.ContentIterator;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;

import java.util.List;

abstract class IndexedFilesListener implements AsyncFileListener {
  private final VfsEventsMerger myEventMerger = new VfsEventsMerger();

  VfsEventsMerger getEventMerger() {
    return myEventMerger;
  }

  public void scheduleForIndexingRecursively(VirtualFile file, boolean onlyContentDependent) {
    FileBasedIndexImpl.cleanProcessedFlag(file);
    if (file.isDirectory()) {
      ContentIterator iterator = fileOrDir -> {
        myEventMerger.recordFileEvent(fileOrDir, onlyContentDependent);
        return true;
      };

      iterateIndexableFiles(file, iterator);
    }
    else {
      myEventMerger.recordFileEvent(file, onlyContentDependent);
    }
  }

  private static boolean collectFiles(VirtualFile file, IntObjectMap<VirtualFile> id2File) {
    ProgressManager.checkCanceled();
    if (file instanceof VirtualFileWithId virtualFileWithId) {
      id2File.put(virtualFileWithId.getId(), file);
    }
    return !file.isDirectory() || FileBasedIndexImpl.isMock(file) || ManagingFS.getInstance().wereChildrenAccessed(file);
  }

  protected abstract void iterateIndexableFiles(VirtualFile file, ContentIterator iterator);

  public void collectFilesRecursively(VirtualFile file, IntObjectMap<VirtualFile> id2File) {
    VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(VirtualFile file) {
        return collectFiles(file, id2File);
      }

      @Override
      public Iterable<VirtualFile> getChildrenIterable(VirtualFile file) {
        return file instanceof NewVirtualFile ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  @Override
  public ChangeApplier prepareChange(List<? extends VFileEvent> events) {
    IntObjectMap<VirtualFile> deletedFiles = IntMaps.newIntObjectHashMap();
    for (VFileEvent event : events) {
      if (event instanceof VFileDeleteEvent deleteEvent) {
        collectFilesRecursively(deleteEvent.getFile(), deletedFiles);
      }
    }

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        for (VirtualFile file : deletedFiles.values()) {
          myEventMerger.recordFileRemovedEvent(file);
        }
      }

      @Override
      public void afterVfsChange() {
        processAfterEvents(events);
      }
    };
  }

  private void processAfterEvents(List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      VirtualFile fileToIndex = null;
      boolean onlyContentDependent = true;

      if (event instanceof VFileContentChangeEvent) {
        fileToIndex = event.getFile();
      }
      else if (event instanceof VFileCopyEvent ce) {
        VirtualFile copy = ce.findCreatedFile();
        if (copy != null) {
          fileToIndex = copy;
          onlyContentDependent = false;
        }
      }
      else if (event instanceof VFileCreateEvent) {
        VirtualFile newChild = event.getFile();
        if (newChild != null) {
          fileToIndex = newChild;
          onlyContentDependent = false;
        }
      }
      else if (event instanceof VFileMoveEvent) {
        fileToIndex = event.getFile();
        onlyContentDependent = false;
      }
      else if (event instanceof VFilePropertyChangeEvent pce) {
        String propertyName = pce.getPropertyName();
        if (propertyName.equals(VirtualFile.PROP_NAME)) {
          // indexes may depend on file name
          fileToIndex = pce.getFile();
          onlyContentDependent = false;
        }
        else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
          fileToIndex = pce.getFile();
        }
      }

      if (fileToIndex != null) {
        scheduleForIndexingRecursively(fileToIndex, onlyContentDependent);
      }
    }
  }
}
