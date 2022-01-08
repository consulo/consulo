/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.vfs;

import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import javax.annotation.Nonnull;

import java.io.IOException;


public class VcsFileSystem extends DeprecatedVirtualFileSystem {

  public static final String COULD_NOT_IMPLEMENT_MESSAGE = VcsBundle.message("exception.text.internal.errror.could.not.implement.method");
  private static final String PROTOCOL = "vcs";

  public static VcsFileSystem getInstance() {
    return (VcsFileSystem)VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
  }

  @Override
  @Nonnull
  public String getProtocol() {
    return PROTOCOL;
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull String path) {
    return null;
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return null;
  }

  @Override
  public void fireContentsChanged(Object requestor, @Nonnull VirtualFile file, long oldModificationStamp) {
    super.fireContentsChanged(requestor, file, oldModificationStamp);
  }

  @Override
  protected void fireBeforeFileDeletion(Object requestor, @Nonnull VirtualFile file) {
    super.fireBeforeFileDeletion(requestor, file);
  }

  @Override
  protected void fireFileDeleted(Object requestor, @Nonnull VirtualFile file, @Nonnull String fileName, VirtualFile parent) {
    super.fireFileDeleted(requestor, file, fileName, parent);
  }

  @Override
  protected void fireBeforeContentsChange(Object requestor, @Nonnull VirtualFile file) {
    super.fireBeforeContentsChange(requestor, file);
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile vFile) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }

  @Override
  public VirtualFile copyFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent, @Nonnull final String copyName) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull String newName) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String fileName) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }

  @Override
  @Nonnull
  public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String dirName) throws IOException {
    throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
  }
}
