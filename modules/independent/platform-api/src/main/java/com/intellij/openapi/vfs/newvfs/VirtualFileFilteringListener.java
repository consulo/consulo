/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.vfs.*;
import javax.annotation.Nonnull;

public class VirtualFileFilteringListener implements VirtualFileListener {
  private final VirtualFileListener myDelegate;
  private final VirtualFileSystem myFilter;

  public VirtualFileFilteringListener(@Nonnull VirtualFileListener delegate, @Nonnull VirtualFileSystem filter) {
    myDelegate = delegate;
    myFilter = filter;
  }

  private boolean isGood(VirtualFileEvent event) {
    return event.getFile().getFileSystem() == myFilter;
  }

  @Override
  public void beforeContentsChange(@Nonnull final VirtualFileEvent event) {
    if (isGood(event)) {
      myDelegate.beforeContentsChange(event);
    }
  }

  @Override
  public void beforeFileDeletion(@Nonnull final VirtualFileEvent event) {
    if (isGood(event)) {
      myDelegate.beforeFileDeletion(event);
    }
  }

  @Override
  public void beforeFileMovement(@Nonnull final VirtualFileMoveEvent event) {
    if (isGood(event)) {
      myDelegate.beforeFileMovement(event);
    }
  }

  @Override
  public void beforePropertyChange(@Nonnull final VirtualFilePropertyEvent event) {
    if (isGood(event)) {
      myDelegate.beforePropertyChange(event);
    }
  }

  @Override
  public void contentsChanged(@Nonnull final VirtualFileEvent event) {
    if (isGood(event)) {
      myDelegate.contentsChanged(event);
    }
  }

  @Override
  public void fileCopied(@Nonnull final VirtualFileCopyEvent event) {
    if (isGood(event)) {
      myDelegate.fileCopied(event);
    }
  }

  @Override
  public void fileCreated(@Nonnull final VirtualFileEvent event) {
    if (isGood(event)) {
      myDelegate.fileCreated(event);
    }
  }

  @Override
  public void fileDeleted(@Nonnull final VirtualFileEvent event) {
    if (isGood(event)) {
      myDelegate.fileDeleted(event);
    }
  }

  @Override
  public void fileMoved(@Nonnull final VirtualFileMoveEvent event) {
    if (isGood(event)) {
      myDelegate.fileMoved(event);
    }
  }

  @Override
  public void propertyChanged(@Nonnull final VirtualFilePropertyEvent event) {
    if (isGood(event)) {
      myDelegate.propertyChanged(event);
    }
  }
}