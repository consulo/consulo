/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.indexing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class IndexedFilesListener extends VirtualFileAdapter implements BulkFileListener {
  private final ManagingFS myManagingFS = ManagingFS.getInstance();
  @Nullable private final String myConfigPath;
  @Nullable private final String myLogPath;

  public IndexedFilesListener() {
    myConfigPath = calcConfigPath(PathManager.getConfigPath());
    myLogPath = calcConfigPath(PathManager.getLogPath());
  }

  @Override
  public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
    buildIndicesForFileRecursively(event.getFile(), false);
  }

  @Override
  public void fileCreated(@Nonnull final VirtualFileEvent event) {
    buildIndicesForFileRecursively(event.getFile(), false);
  }

  @Override
  public void fileCopied(@Nonnull final VirtualFileCopyEvent event) {
    buildIndicesForFileRecursively(event.getFile(), false);
  }

  @Override
  public void beforeFileDeletion(@Nonnull final VirtualFileEvent event) {
    invalidateIndicesRecursively(event.getFile(), false);
  }

  @Override
  public void beforeContentsChange(@Nonnull final VirtualFileEvent event) {
    invalidateIndicesRecursively(event.getFile(), true);
  }

  @Override
  public void contentsChanged(@Nonnull final VirtualFileEvent event) {
    buildIndicesForFileRecursively(event.getFile(), true);
  }

  @Override
  public void beforePropertyChange(@Nonnull final VirtualFilePropertyEvent event) {
    String propertyName = event.getPropertyName();

    if (propertyName.equals(VirtualFile.PROP_NAME)) {
      // indexes may depend on file name
      // name change may lead to filetype change so the file might become not indexable
      // in general case have to 'unindex' the file and index it again if needed after the name has been changed
      invalidateIndicesRecursively(event.getFile(), false);
    } else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
      invalidateIndicesRecursively(event.getFile(), true);
    }
  }

  @Override
  public void propertyChanged(@Nonnull final VirtualFilePropertyEvent event) {
    String propertyName = event.getPropertyName();
    if (propertyName.equals(VirtualFile.PROP_NAME)) {
      // indexes may depend on file name
      buildIndicesForFileRecursively(event.getFile(), false);
    } else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
      buildIndicesForFileRecursively(event.getFile(), true);
    }
  }

  protected void buildIndicesForFileRecursively(@Nonnull final VirtualFile file, final boolean contentChange) {
    if (file.isDirectory()) {
      final ContentIterator iterator = fileOrDir -> {
        buildIndicesForFile(fileOrDir, contentChange);
        return true;
      };

      iterateIndexableFiles(file, iterator);
    }
    else {
      buildIndicesForFile(file, contentChange);
    }
  }

  protected boolean invalidateIndicesForFile(VirtualFile file, boolean contentChange) {
    if (isUnderConfigOrSystem(file)) {
      return false;
    }
    if (file.isDirectory()) {
      doInvalidateIndicesForFile(file, contentChange);
      if (!FileBasedIndexImpl.isMock(file) && !myManagingFS.wereChildrenAccessed(file)) {
        return false;
      }
    }
    else {
      doInvalidateIndicesForFile(file, contentChange);
    }
    return true;
  }

  protected abstract void iterateIndexableFiles(VirtualFile file, ContentIterator iterator);
  protected abstract void buildIndicesForFile(VirtualFile file, boolean contentChange);
  protected abstract void doInvalidateIndicesForFile(VirtualFile file, boolean contentChange);

  protected void invalidateIndicesRecursively(@Nonnull final VirtualFile file, final boolean contentChange) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        return invalidateIndicesForFile(file, contentChange);
      }

      @Override
      public Iterable<VirtualFile> getChildrenIterable(@Nonnull VirtualFile file) {
        return file instanceof NewVirtualFile ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  @Override
  public void before(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      BulkVirtualFileListenerAdapter.fireBefore(this, event);
    }
  }

  @Override
  public void after(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      BulkVirtualFileListenerAdapter.fireAfter(this, event);
    }
  }

  @Nullable
  private static String calcConfigPath(@Nonnull String path) {
    try {
      final String _path = FileUtil.toSystemIndependentName(new File(path).getCanonicalPath());
      return _path.endsWith("/") ? _path : _path + "/";
    }
    catch (IOException e) {
      FileBasedIndexImpl.LOG.info(e);
      return null;
    }
  }

  private boolean isUnderConfigOrSystem(@Nonnull VirtualFile file) {
    final String filePath = file.getPath();
    return myConfigPath != null && FileUtil.startsWith(filePath, myConfigPath) ||
           myLogPath != null && FileUtil.startsWith(filePath, myLogPath);
  }
}