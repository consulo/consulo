/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.fileSystem;

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
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public VirtualFile findFileByIoFile(@Nonnull File file) {
    return file.exists() ? new CoreLocalVirtualFile(this, file) : null;
  }

  @Nullable
  @Override
  public VirtualFile refreshAndFindFileByIoFile(@Nonnull File file) {
    return findFileByIoFile(file);
  }

  @Override
  public void refreshIoFiles(@Nonnull Iterable<File> files) {
  }

  @Override
  public void refreshIoFiles(@Nonnull Iterable<File> files, boolean async, boolean recursive, @javax.annotation.Nullable Runnable onFinish) {
  }

  @Override
  public void refreshFiles(@Nonnull Iterable<VirtualFile> files) {
  }

  @Override
  public void refreshFiles(@Nonnull Iterable<VirtualFile> files, boolean async, boolean recursive, @javax.annotation.Nullable Runnable onFinish) {
  }

  @Nonnull
  @Override
  public Set<WatchRequest> addRootsToWatch(@Nonnull Collection<String> rootPaths, boolean watchRecursively) {
    return Collections.emptySet();
  }

  @Override
  public void removeWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests) {
  }

  @Override
  public Set<WatchRequest> replaceWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests,
                                               @javax.annotation.Nullable Collection<String> recursiveRoots,
                                               @Nullable Collection<String> flatRoots) {
    return Collections.emptySet();
  }

  @Override
  public void registerAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler) {
  }

  @Override
  public void unregisterAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler) {
  }

  @Override
  public boolean processCachedFilesInSubtree(@Nonnull VirtualFile file, @Nonnull Processor<VirtualFile> processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCaseSensitive() {
    return SystemInfo.isFileSystemCaseSensitive;
  }

  @Nullable
  @Override
  public VirtualFile findFileByPathIfCached(@Nonnull @NonNls String path) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  protected String extractRootPath(@Nonnull String path) {
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
  public VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String copyName)
    throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public InputStream getInputStream(@Nonnull VirtualFile file) throws IOException {
    return file.getInputStream();
  }

  @Nonnull
  @Override
  public OutputStream getOutputStream(@Nonnull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLength(@Nonnull VirtualFile file) {
    return 0;
  }

  @Override
  public boolean exists(@Nonnull VirtualFile file) {
    return file.exists();
  }

  @Nonnull
  @Override
  public String[] list(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDirectory(@Nonnull VirtualFile file) {
    return file.isDirectory();
  }

  @Override
  public long getTimeStamp(@Nonnull VirtualFile file) {
    return 0;
  }

  @Override
  public void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) throws IOException {
  }

  @Override
  public boolean isWritable(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
    File file = new File(parent.getPath(), dir);
    FileUtil.createDirectory(file);
    return findFileByIoFile(file);
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
    assert file instanceof CoreLocalVirtualFile;

    FileUtil.delete(new File(file.getPath()));
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
  }

  @javax.annotation.Nullable
  @Override
  public FileAttributes getAttributes(@Nonnull VirtualFile file) {
    String path = normalize(file.getPath());
    if (path == null) return null;
    if (file.getParent() == null && path.startsWith("//")) {
      return FAKE_ROOT_ATTRIBUTES;  // fake Windows roots
    }
    return FileSystemUtil.getAttributes(FileUtil.toSystemDependentName(path));
  }

  @Override
  @Nullable
  protected String normalize(@Nonnull String path) {
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

  @Nonnull
  @Override
  public String getProtocol() {
    return PROTOCOL;
  }

  @javax.annotation.Nullable
  @Override
  public VirtualFile findFileByPath(@Nonnull @NonNls String path) {
    return findFileByIoFile(new File(path));
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Nullable
  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return findFileByPath(path);
  }
}
