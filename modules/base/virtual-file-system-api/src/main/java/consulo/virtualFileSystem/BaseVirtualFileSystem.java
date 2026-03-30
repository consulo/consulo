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
package consulo.virtualFileSystem;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.proxy.EventDispatcher;
import consulo.virtualFileSystem.event.*;

import org.jspecify.annotations.Nullable;
import java.io.IOException;

/**
 * @author max
 */
public abstract class BaseVirtualFileSystem implements VirtualFileSystem {
  private final EventDispatcher<VirtualFileListener> myEventDispatcher = EventDispatcher.create(VirtualFileListener.class);

  protected void startEventPropagation() {
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      return;
    }

    application.getMessageBus().connect().subscribe(BulkFileListener.class, new BulkVirtualFileListenerAdapter(myEventDispatcher.getMulticaster(), this));
  }

  @Override
  public void addVirtualFileListener(VirtualFileListener listener) {
    myEventDispatcher.addListener(listener);
  }

  /**
   * Removes listener form the file system.
   *
   * @param listener the listener
   */
  @Override
  public void removeVirtualFileListener(VirtualFileListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  @RequiredWriteAction
  protected void firePropertyChanged(
    @Nullable Object requestor,
    VirtualFile file,
    String propertyName,
    Object oldValue,
    Object newValue
  ) {
    assertWriteAccessAllowed();
    VirtualFilePropertyEvent event = new VirtualFilePropertyEvent(requestor, file, propertyName, oldValue, newValue);
    myEventDispatcher.getMulticaster().propertyChanged(event);
  }

  @RequiredWriteAction
  protected void fireContentsChanged(@Nullable Object requestor, VirtualFile file, long oldModificationStamp) {
    assertWriteAccessAllowed();
    VirtualFileEvent event = new VirtualFileEvent(requestor, file, file.getParent(), oldModificationStamp, file.getModificationStamp());
    myEventDispatcher.getMulticaster().contentsChanged(event);
  }

  @RequiredWriteAction
  protected void fireFileCreated(@Nullable Object requestor, VirtualFile file) {
    assertWriteAccessAllowed();
    VirtualFileEvent event = new VirtualFileEvent(requestor, file, file.getName(), file.getParent());
    myEventDispatcher.getMulticaster().fileCreated(event);
  }

  @RequiredWriteAction
  protected void fireFileDeleted(@Nullable Object requestor, VirtualFile file, String fileName, VirtualFile parent) {
    assertWriteAccessAllowed();
    VirtualFileEvent event = new VirtualFileEvent(requestor, file, fileName, parent);
    myEventDispatcher.getMulticaster().fileDeleted(event);
  }

  @RequiredWriteAction
  protected void fireFileMoved(@Nullable Object requestor, VirtualFile file, VirtualFile oldParent) {
    assertWriteAccessAllowed();
    VirtualFileMoveEvent event = new VirtualFileMoveEvent(requestor, file, oldParent, file.getParent());
    myEventDispatcher.getMulticaster().fileMoved(event);
  }

  @RequiredWriteAction
  protected void fireFileCopied(@Nullable Object requestor, VirtualFile originalFile, VirtualFile createdFile) {
    assertWriteAccessAllowed();
    VirtualFileCopyEvent event = new VirtualFileCopyEvent(requestor, originalFile, createdFile);
    try {
      myEventDispatcher.getMulticaster().fileCopied(event);
    }
    catch (AbstractMethodError e) { //compatibility with 6.0
      myEventDispatcher.getMulticaster().fileCreated(event);
    }
  }

  @RequiredWriteAction
  protected void fireBeforePropertyChange(
    @Nullable Object requestor,
    VirtualFile file,
    String propertyName,
    Object oldValue,
    Object newValue
  ) {
    assertWriteAccessAllowed();
    VirtualFilePropertyEvent event = new VirtualFilePropertyEvent(requestor, file, propertyName, oldValue, newValue);
    myEventDispatcher.getMulticaster().beforePropertyChange(event);
  }

  @RequiredWriteAction
  protected void fireBeforeContentsChange(@Nullable Object requestor, VirtualFile file) {
    assertWriteAccessAllowed();
    VirtualFileEvent event = new VirtualFileEvent(requestor, file, file.getName(), file.getParent());
    myEventDispatcher.getMulticaster().beforeContentsChange(event);
  }

  @RequiredWriteAction
  protected void fireBeforeFileDeletion(@Nullable Object requestor, VirtualFile file) {
    assertWriteAccessAllowed();
    VirtualFileEvent event = new VirtualFileEvent(requestor, file, file.getName(), file.getParent());
    myEventDispatcher.getMulticaster().beforeFileDeletion(event);
  }

  @RequiredWriteAction
  protected void fireBeforeFileMovement(@Nullable Object requestor, VirtualFile file, VirtualFile newParent) {
    assertWriteAccessAllowed();
    VirtualFileMoveEvent event = new VirtualFileMoveEvent(requestor, file, file.getParent(), newParent);
    myEventDispatcher.getMulticaster().beforeFileMovement(event);
  }

  @RequiredWriteAction
  protected void assertWriteAccessAllowed() {
    Application.get().assertWriteAccessAllowed();
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  @RequiredWriteAction
  public void deleteFile(@Nullable Object requestor, VirtualFile vFile) throws IOException {
    throw new UnsupportedOperationException("deleteFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public void moveFile(@Nullable Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException("move() not supported");
  }

  @Override
  @RequiredWriteAction
  public void renameFile(@Nullable Object requestor, VirtualFile vFile, String newName) throws IOException {
    throw new UnsupportedOperationException("renameFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile vDir, String fileName) throws IOException {
    throw new UnsupportedOperationException("createChildFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile vDir, String dirName) throws IOException {
    throw new UnsupportedOperationException("createChildDirectory() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile copyFile(@Nullable Object requestor, VirtualFile virtualFile, VirtualFile newParent, String copyName)
      throws IOException {
    throw new UnsupportedOperationException("copyFile() not supported");
  }
}