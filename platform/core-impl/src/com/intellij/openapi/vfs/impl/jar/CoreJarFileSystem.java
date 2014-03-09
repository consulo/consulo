/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class CoreJarFileSystem extends DeprecatedVirtualFileSystem implements ArchiveFileSystem {
  private final Map<String, CoreJarHandler> myHandlers = new HashMap<String, CoreJarHandler>();

  @NotNull
  @Override
  public String getProtocol() {
    return StandardFileSystems.JAR_PROTOCOL;
  }

  @Override
  public VirtualFile findFileByPath(@NotNull @NonNls String path) {
    int separatorIndex = path.indexOf("!/");
    if (separatorIndex < 0) {
      throw new IllegalArgumentException("path in JarFileSystem must contain a separator");
    }
    String localPath = path.substring(0, separatorIndex);
    String pathInJar = path.substring(separatorIndex+2);
    CoreJarHandler handler = getHandler(localPath);
    if (handler == null)
      return null;
    return handler.findFileByPath(pathInJar);
  }

  @Nullable
  protected CoreJarHandler getHandler(String localPath) {
    CoreJarHandler handler = myHandlers.get(localPath);
    if (handler == null) {
      handler = new CoreJarHandler(this, localPath);
      myHandlers.put(localPath, handler);
    }
    return handler;
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
    return findFileByPath(path);
  }

  @Nullable
  @Override
  public VirtualFile getVirtualFileForArchive(@Nullable VirtualFile entryVFile) {
    return null;
  }

  @Nullable
  @Override
  public VirtualFile findByPathWithSeparator(@Nullable VirtualFile entryVFile) {
    return null;
  }

  @Nullable
  @Override
  public ArchiveFile getArchiveWrapperFile(@NotNull VirtualFile entryVFile) throws IOException {
    return null;
  }

  @Override
  public boolean isMakeCopyOfJar(File originalFile) {
    return false;
  }

  @Override
  public boolean isMakeCopyForArchive(@NotNull File originalFile) {
    return false;
  }

  @Override
  public void setNoCopyJarForPath(String pathInJar) {
  }

  @Override
  public void addNoCopyArchiveForPath(@NotNull String path) {

  }

  @Override
  public void refreshWithoutFileWatcher(boolean asynchronous) {
  }

  @Override
  public boolean exists(@NotNull VirtualFile file) {
    return false;
  }

  @NotNull
  @Override
  public String[] list(@NotNull VirtualFile file) {
    return new String[0];
  }

  @Override
  public boolean isDirectory(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  public long getTimeStamp(@NotNull VirtualFile file) {
    return 0;
  }

  @Override
  public void setTimeStamp(@NotNull VirtualFile file, long timeStamp) throws IOException {
  }

  @Override
  public boolean isWritable(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  public void setWritable(@NotNull VirtualFile file, boolean writableFlag) throws IOException {
  }

  @Override
  public boolean isSymLink(@NotNull VirtualFile file) {
    return false;
  }

  @Nullable
  @Override
  public String resolveSymLink(@NotNull VirtualFile file) {
    return null;
  }

  @Override
  public boolean isSpecialFile(@NotNull VirtualFile file) {
    return false;
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray(@NotNull VirtualFile file) throws IOException {
    return new byte[0];
  }

  @NotNull
  @Override
  public InputStream getInputStream(@NotNull VirtualFile file) throws IOException {
    return null;
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(@NotNull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
    return null;
  }

  @Override
  public long getLength(@NotNull VirtualFile file) {
    return 0;
  }

  @Nullable
  @Override
  public VirtualFile getLocalVirtualFileFor(@Nullable VirtualFile entryVFile) {
    return null;
  }

  @Nullable
  @Override
  public VirtualFile findLocalVirtualFileByPath(@NotNull String path) {
    return null;
  }

  @Override
  public void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {
  }

  @Override
  public void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException {
  }

  @Override
  public void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException {
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException {
    return null;
  }

  @NotNull
  @Override
  public VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException {
    return null;
  }

  @Override
  public VirtualFile copyFile(Object requestor,
                                 @NotNull VirtualFile virtualFile,
                                 @NotNull VirtualFile newParent,
                                 @NotNull String copyName) throws IOException {
    return null;
  }
}
