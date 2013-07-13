/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.vfs.impl.archive;

import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 18:54/13.07.13
 */
public interface ArchiveHandler {
  @Nullable
  FileAttributes getAttributes(@NotNull final VirtualFile file);

  @NotNull
  File getOriginalFile();

  @NotNull
  String[] list(VirtualFile file);

  boolean isDirectory(VirtualFile file);

  long getTimeStamp(VirtualFile file);

  long getLength(VirtualFile file);

  byte[] contentsToByteArray(VirtualFile file) throws IOException;

  InputStream getInputStream(VirtualFile file) throws IOException;

  boolean exists(VirtualFile fileOrDirectory);

  VirtualFile markDirty();

  void refreshLocalFileForJar();

  ArchiveFile getArchiveFile();

  File getMirrorFile(File file);
}
