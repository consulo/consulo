// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import consulo.container.boot.ContainerPathManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

abstract class IndexedFilesListener implements AsyncFileListener {
  private final VfsEventsMerger myEventMerger = new VfsEventsMerger();

  private static class ConfigHolder {
    private static final VirtualFile myConfig = LocalFileSystem.getInstance().findFileByIoFile(new File(ContainerPathManager.get().getConfigPath()));
    private static final VirtualFile myLog = LocalFileSystem.getInstance().findFileByIoFile(ContainerPathManager.get().getLogPath());
  }

  @Nonnull
  VfsEventsMerger getEventMerger() {
    return myEventMerger;
  }

  protected void buildIndicesForFileRecursively(@Nonnull final VirtualFile file, final boolean contentChange) {
    if (file.isDirectory()) {
      final ContentIterator iterator = fileOrDir -> {
        myEventMerger.recordFileEvent(fileOrDir, contentChange);
        return true;
      };

      iterateIndexableFiles(file, iterator);
    }
    else {
      myEventMerger.recordFileEvent(file, contentChange);
    }
  }

  private static boolean invalidateIndicesForFile(@Nonnull VirtualFile file, boolean contentChange, @Nonnull VfsEventsMerger eventMerger) {
    if (isUnderConfigOrSystem(file)) {
      return false;
    }
    ProgressManager.checkCanceled();
    eventMerger.recordBeforeFileEvent(file, contentChange);
    return !file.isDirectory() || FileBasedIndexImpl.isMock(file) || ManagingFS.getInstance().wereChildrenAccessed(file);
  }

  protected abstract void iterateIndexableFiles(@Nonnull VirtualFile file, @Nonnull ContentIterator iterator);

  void invalidateIndicesRecursively(@Nonnull VirtualFile file, boolean contentChange, @Nonnull VfsEventsMerger eventMerger) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        return invalidateIndicesForFile(file, contentChange, eventMerger);
      }

      @Override
      public Iterable<VirtualFile> getChildrenIterable(@Nonnull VirtualFile file) {
        return file instanceof NewVirtualFile ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  @Override
  @Nonnull
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    VfsEventsMerger tempMerger = new VfsEventsMerger();
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent) {
        invalidateIndicesRecursively(((VFileContentChangeEvent)event).getFile(), true, tempMerger);
      }
      else if (event instanceof VFileDeleteEvent) {
        invalidateIndicesRecursively(((VFileDeleteEvent)event).getFile(), false, tempMerger);
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        final VFilePropertyChangeEvent pce = (VFilePropertyChangeEvent)event;
        String propertyName = pce.getPropertyName();
        if (propertyName.equals(VirtualFile.PROP_NAME)) {
          // indexes may depend on file name
          // name change may lead to filetype change so the file might become not indexable
          // in general case have to 'unindex' the file and index it again if needed after the name has been changed
          invalidateIndicesRecursively(pce.getFile(), false, tempMerger);
        }
        else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
          invalidateIndicesRecursively(pce.getFile(), true, tempMerger);
        }
      }
    }
    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        myEventMerger.applyMergedEvents(tempMerger);
      }

      @Override
      public void afterVfsChange() {
        processAfterEvents(events);
      }
    };
  }

  private void processAfterEvents(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent) {
        buildIndicesForFileRecursively(((VFileContentChangeEvent)event).getFile(), true);
      }
      else if (event instanceof VFileCopyEvent) {
        final VFileCopyEvent ce = (VFileCopyEvent)event;
        final VirtualFile copy = ce.getNewParent().findChild(ce.getNewChildName());
        if (copy != null) {
          buildIndicesForFileRecursively(copy, false);
        }
      }
      else if (event instanceof VFileCreateEvent) {
        final VirtualFile newChild = event.getFile();
        if (newChild != null) {
          buildIndicesForFileRecursively(newChild, false);
        }
      }
      else if (event instanceof VFileMoveEvent) {
        buildIndicesForFileRecursively(((VFileMoveEvent)event).getFile(), false);
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        final VFilePropertyChangeEvent pce = (VFilePropertyChangeEvent)event;
        String propertyName = pce.getPropertyName();
        if (propertyName.equals(VirtualFile.PROP_NAME)) {
          // indexes may depend on file name
          buildIndicesForFileRecursively(pce.getFile(), false);
        }
        else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
          buildIndicesForFileRecursively(pce.getFile(), true);
        }
      }
    }
  }

  private static boolean isUnderConfigOrSystem(@Nonnull VirtualFile file) {
    return ConfigHolder.myConfig != null && VfsUtilCore.isAncestor(ConfigHolder.myConfig, file, false) || ConfigHolder.myLog != null && VfsUtilCore.isAncestor(ConfigHolder.myLog, file, false);
  }
}