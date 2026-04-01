// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.progress.ProgressManager;
import consulo.container.boot.ContainerPathManager;
import consulo.content.ContentIterator;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;

import java.io.File;
import java.util.List;

abstract class IndexedFilesListener implements AsyncFileListener {
  private final VfsEventsMerger myEventMerger = new VfsEventsMerger();

  private static class ConfigHolder {
    private static final VirtualFile myConfig = LocalFileSystem.getInstance().findFileByIoFile(new File(ContainerPathManager.get().getConfigPath()));
    private static final VirtualFile myLog = LocalFileSystem.getInstance().findFileByIoFile(ContainerPathManager.get().getLogPath());
  }

  
  VfsEventsMerger getEventMerger() {
    return myEventMerger;
  }

  protected void buildIndicesForFileRecursively(VirtualFile file, boolean contentChange) {
    if (file.isDirectory()) {
      ContentIterator iterator = fileOrDir -> {
        myEventMerger.recordFileEvent(fileOrDir, contentChange);
        return true;
      };

      iterateIndexableFiles(file, iterator);
    }
    else {
      myEventMerger.recordFileEvent(file, contentChange);
    }
  }

  private static boolean invalidateIndicesForFile(VirtualFile file, boolean contentChange, VfsEventsMerger eventMerger) {
    if (isUnderConfigOrSystem(file)) {
      return false;
    }
    ProgressManager.checkCanceled();
    eventMerger.recordBeforeFileEvent(file, contentChange);
    return !file.isDirectory() || FileBasedIndexImpl.isMock(file) || ManagingFS.getInstance().wereChildrenAccessed(file);
  }

  protected abstract void iterateIndexableFiles(VirtualFile file, ContentIterator iterator);

  void invalidateIndicesRecursively(VirtualFile file, boolean contentChange, VfsEventsMerger eventMerger) {
    VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(VirtualFile file) {
        return invalidateIndicesForFile(file, contentChange, eventMerger);
      }

      @Override
      public Iterable<VirtualFile> getChildrenIterable(VirtualFile file) {
        return file instanceof NewVirtualFile ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  @Override
  
  public ChangeApplier prepareChange(List<? extends VFileEvent> events) {
    VfsEventsMerger tempMerger = new VfsEventsMerger();
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent cce) {
          invalidateIndicesRecursively(cce.getFile(), true, tempMerger);
      }
      else if (event instanceof VFileDeleteEvent de) {
          invalidateIndicesRecursively(de.getFile(), false, tempMerger);
      }
      else if (event instanceof VFilePropertyChangeEvent pce) {
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

  private void processAfterEvents(List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent cce) {
          buildIndicesForFileRecursively(cce.getFile(), true);
      }
      else if (event instanceof VFileCopyEvent ce) {
        VirtualFile copy = ce.findCreatedFile();
        if (copy != null) {
          buildIndicesForFileRecursively(copy, false);
        }
      }
      else if (event instanceof VFileCreateEvent ce) {
        VirtualFile newChild = ce.getFile();
        if (newChild != null) {
          buildIndicesForFileRecursively(newChild, false);
        }
      }
      else if (event instanceof VFileMoveEvent me) {
          buildIndicesForFileRecursively(me.getFile(), false);
      }
      else if (event instanceof VFilePropertyChangeEvent pce) {
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

  private static boolean isUnderConfigOrSystem(VirtualFile file) {
    return ConfigHolder.myConfig != null
        && VirtualFileUtil.isAncestor(ConfigHolder.myConfig, file, false)
        || ConfigHolder.myLog != null && VirtualFileUtil.isAncestor(ConfigHolder.myLog, file, false);
  }
}