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

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.PathUtil;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.impl.archive.ArchiveFileSystemBase;
import consulo.compiler.server.fileSystem.archive.ArchiveNewVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredWriteAction;

import java.io.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:39/15.08.13
 */
public class CompilerServerManagingFSImpl extends PersistentFS {
  @Override
  public void clearIdCache() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public String[] listPersisted(@NotNull VirtualFile parent) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public FSRecords.NameId[] listAll(@NotNull VirtualFile parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getId(@NotNull VirtualFile parent, @NotNull String childName, @NotNull NewVirtualFileSystem delegate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName(int id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastRecordedLength(@NotNull VirtualFile file) {
    return 0;
  }

  @Override
  public boolean exists(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTimeStamp(@NotNull VirtualFile file, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWritable(@NotNull VirtualFile file, boolean writableFlag) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSymLink(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String resolveSymLink(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile createChildDirectory(@Nullable Object requestor, @NotNull VirtualFile parent, @NotNull String dir) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile createChildFile(@Nullable Object requestor, @NotNull VirtualFile parent, @NotNull String file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(Object requestor, @NotNull VirtualFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void moveFile(Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void renameFile(Object requestor, @NotNull VirtualFile file, @NotNull String newName) throws IOException {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(@NotNull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLength(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHidden(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getFileAttributes(int id) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public NewVirtualFile findFileByIdIfCached(int id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int storeUnlinkedContent(@NotNull byte[] bytes) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray(int contentId) throws IOException {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray(@NotNull VirtualFile file, boolean cacheContent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int acquireContent(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void releaseContent(int contentId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getCurrentContentId(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @RequiredWriteAction
  @Override
  public void processEvents(@NotNull List<VFileEvent> events) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public DataInputStream readAttribute(@NotNull VirtualFile file, @NotNull FileAttribute att) {
    return new DataInputStream(new ByteArrayInputStream(new byte[100]));
  }

  @NotNull
  @Override
  public DataOutputStream writeAttribute(@NotNull VirtualFile file, @NotNull FileAttribute att) {
    return new DataOutputStream(new ByteArrayOutputStream(100));
  }

  @Override
  public int getModificationCount(@NotNull VirtualFile fileOrDirectory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getCheapFileSystemModificationCount() {
    return 0;
  }

  @Override
  public int getModificationCount() {
    return 0;
  }

  @Override
  public int getStructureModificationCount() {
    return 0;
  }

  @Override
  public int getFilesystemModificationCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getCreationTimestamp() {
    return 0;
  }

  @Override
  public boolean areChildrenLoaded(@NotNull VirtualFile dir) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean wereChildrenAccessed(@NotNull VirtualFile dir) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public NewVirtualFile findRoot(@NotNull String basePath, @NotNull NewVirtualFileSystem fs) {
    String p = PathUtil.toPresentableUrl(basePath);
    VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(p);

    if (fileByPath != null) {
      if (fs instanceof ArchiveFileSystem) {
        return new ArchiveNewVirtualFile(fileByPath, (ArchiveFileSystemBase) fs);
      }
      return new CompilerServerNewVirtualFileImpl(fileByPath, fs);
    }
    return null;
  }

  @NotNull
  @Override
  public VirtualFile[] getRoots() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public VirtualFile[] getRoots(@NotNull NewVirtualFileSystem fs) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public VirtualFile[] getLocalRoots() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public VirtualFile findFileById(int id) {
    throw new UnsupportedOperationException();
  }
}
