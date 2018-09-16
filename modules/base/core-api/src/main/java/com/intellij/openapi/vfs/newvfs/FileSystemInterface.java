/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileSystemInterface {
  // default values for missing files (same as in corresponding java.io.File methods)
  long DEFAULT_LENGTH = 0;
  long DEFAULT_TIMESTAMP = 0;

  boolean exists(@Nonnull VirtualFile file);

  @Nonnull
  String[] list(@Nonnull VirtualFile file);

  boolean isDirectory(@Nonnull VirtualFile file);

  long getTimeStamp(@Nonnull VirtualFile file);
  void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) throws IOException;

  boolean isWritable(@Nonnull VirtualFile file);
  void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException;

  boolean isSymLink(@Nonnull VirtualFile file);
  @Nullable
  String resolveSymLink(@Nonnull VirtualFile file);

  VirtualFile createChildDirectory(@Nullable Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException;
  VirtualFile createChildFile(@Nullable Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException;

  void deleteFile(final Object requestor, @Nonnull VirtualFile file) throws IOException;
  void moveFile(final Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException;
  void renameFile(final Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException;
  VirtualFile copyFile(final Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String copyName) throws IOException;

  @Nonnull
  byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException;

  /**
   * Does NOT strip the BOM from the beginning of the stream, unlike the {@link com.intellij.openapi.vfs.VirtualFile#getInputStream()}
   */
  @Nonnull
  InputStream getInputStream(@Nonnull VirtualFile file) throws IOException;

  /**
   * Does NOT add the BOM to the beginning of the stream, unlike the {@link com.intellij.openapi.vfs.VirtualFile#getOutputStream(Object)}
   */
  @Nonnull
  OutputStream getOutputStream(@Nonnull VirtualFile file, final Object requestor, final long modStamp, final long timeStamp) throws IOException;

  long getLength(@Nonnull VirtualFile file);
}
