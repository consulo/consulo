/*
 * Copyright 2013-2014 must-be.org
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
package org.consulo.compiler.server.fileSystem;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import com.intellij.util.Processor;
import com.intellij.util.io.fs.IFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * @author VISTALL
 * @since 1:44/11.08.13
 */
public class CompilerServerLocalFileSystemImpl extends LocalFileSystem {
  private static final FileAttributes FAKE_ROOT_ATTRIBUTES =
    new FileAttributes(true, false, false, false, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, false);

  @Nullable
  @Override
  public VirtualFile findFileByIoFile(@NotNull File file) {
    return file.exists() ? new CoreLocalVirtualFile(this, file) : null;
  }

  @Nullable
  @Override
  public VirtualFile findFileByIoFile(@NotNull IFile file) {
    return findFileByPath(file.getAbsolutePath());
  }

  @Nullable
  @Override
  public VirtualFile refreshAndFindFileByIoFile(@NotNull File file) {
    return findFileByIoFile(file);
  }

  @Nullable
  @Override
  public VirtualFile refreshAndFindFileByIoFile(@NotNull IFile ioFile) {
    return findFileByIoFile(ioFile);
  }

  @Override
  public void refreshIoFiles(@NotNull Iterable<File> files) {
  }

  @Override
  public void refreshIoFiles(@NotNull Iterable<File> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
  }

  @Override
  public void refreshFiles(@NotNull Iterable<VirtualFile> files) {
  }

  @Override
  public void refreshFiles(@NotNull Iterable<VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
  }

  @NotNull
  @Override
  public Set<WatchRequest> addRootsToWatch(@NotNull Collection<String> rootPaths, boolean watchRecursively) {
    return Collections.emptySet();
  }

  @Override
  public void removeWatchedRoots(@NotNull Collection<WatchRequest> watchRequests) {
  }

  @Override
  public Set<WatchRequest> replaceWatchedRoots(@NotNull Collection<WatchRequest> watchRequests,
                                               @Nullable Collection<String> recursiveRoots,
                                               @Nullable Collection<String> flatRoots) {
    return Collections.emptySet();
  }

  @Override
  public void registerAuxiliaryFileOperationsHandler(@NotNull LocalFileOperationsHandler handler) {
  }

  @Override
  public void unregisterAuxiliaryFileOperationsHandler(@NotNull LocalFileOperationsHandler handler) {
  }

  @Override
  public boolean processCachedFilesInSubtree(@NotNull VirtualFile file, @NotNull Processor<VirtualFile> processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCaseSensitive() {
    return SystemInfo.isFileSystemCaseSensitive;
  }

  @Nullable
  @Override
  public VirtualFile findFileByPathIfCached(@NotNull @NonNls String path) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  protected String extractRootPath(@NotNull String path) {
    if (path.isEmpty()) {
      try {
        return extractRootPath(new File("").getCanonicalPath());
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (SystemInfo.isWindows) {
      if (path.length() >= 2 && path.charAt(1) == ':') {
        // Drive letter
        return path.substring(0, 2).toUpperCase(Locale.US);
      }

      if (path.startsWith("//") || path.startsWith("\\\\")) {
        // UNC. Must skip exactly two path elements like [\\ServerName\ShareName]\pathOnShare\file.txt
        // Root path is in square brackets here.

        int slashCount = 0;
        int idx;
        for (idx = 2; idx < path.length() && slashCount < 2; idx++) {
          final char c = path.charAt(idx);
          if (c == '\\' || c == '/') {
            slashCount++;
            idx--;
          }
        }

        return path.substring(0, idx);
      }

      return "";
    }

    return StringUtil.startsWithChar(path, '/') ? "/" : "";
  }

  @Override
  public int getRank() {
    return 0;
  }

  @Override
  public VirtualFile copyFile(Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent, @NotNull String copyName)
    throws IOException {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray(@NotNull VirtualFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public InputStream getInputStream(@NotNull VirtualFile file) throws IOException {
    return file.getInputStream();
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(@NotNull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLength(@NotNull VirtualFile file) {
    return 0;
  }

  @Override
  public boolean exists(@NotNull VirtualFile file) {
    return file.exists();
  }

  @NotNull
  @Override
  public String[] list(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDirectory(@NotNull VirtualFile file) {
    return file.isDirectory();
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
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile parent, @NotNull String dir) throws IOException {
    File file = new File(parent.getPath(), dir);
    FileUtil.createDirectory(file);
    return findFileByIoFile(file);
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @NotNull VirtualFile parent, @NotNull String file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(Object requestor, @NotNull VirtualFile file) throws IOException {
    assert file instanceof CoreLocalVirtualFile;

    FileUtil.delete(new File(file.getPath()));
  }

  @Override
  public void moveFile(Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void renameFile(Object requestor, @NotNull VirtualFile file, @NotNull String newName) throws IOException {
  }

  @Nullable
  @Override
  public FileAttributes getAttributes(@NotNull VirtualFile file) {
    String path = normalize(file.getPath());
    if (path == null) return null;
    if (file.getParent() == null && path.startsWith("//")) {
      return FAKE_ROOT_ATTRIBUTES;  // fake Windows roots
    }
    return FileSystemUtil.getAttributes(FileUtil.toSystemDependentName(path));
  }

  @Override
  @Nullable
  protected String normalize(@NotNull String path) {
    if (path.isEmpty()) {
      try {
        path = new File("").getCanonicalPath();
      }
      catch (IOException e) {
        return path;
      }
    }
    else if (SystemInfo.isWindows) {
      if (path.charAt(0) == '/' && !path.startsWith("//")) {
        path = path.substring(1);  // hack over new File(path).toURI().toURL().getFile()
      }

      if (path.contains("~")) {
        try {
          path = new File(FileUtil.toSystemDependentName(path)).getCanonicalPath();
        }
        catch (IOException e) {
          return null;
        }
      }
    }

    File file = new File(path);
    if (!isAbsoluteFileOrDriveLetter(file)) {
      path = file.getAbsolutePath();
    }

    return FileUtil.normalize(path);
  }

  private static boolean isAbsoluteFileOrDriveLetter(File file) {
    String path = file.getPath();
    if (SystemInfo.isWindows && path.length() == 2 && path.charAt(1) == ':') {
      // just drive letter.
      // return true, despite the fact that technically it's not an absolute path
      return true;
    }
    return file.isAbsolute();
  }

  @NotNull
  @Override
  public String getProtocol() {
    return PROTOCOL;
  }

  @Nullable
  @Override
  public VirtualFile findFileByPath(@NotNull @NonNls String path) {
    return findFileByIoFile(new File(path));
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Nullable
  @Override
  public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
    return findFileByPath(path);
  }
}
