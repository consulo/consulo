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
import com.intellij.openapi.vfs.newvfs.impl.VfsData;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.PathUtil;
import consulo.annotations.RequiredWriteAction;
import consulo.compiler.server.fileSystem.archive.ArchiveNewVirtualFile;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.impl.archive.ArchiveFileSystemBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:39/15.08.13
 */
public class CompilerServerManagingFSImpl extends PersistentFS {
  @Nonnull
  @Override
  public VfsData getVfsData() {
    return null;
  }

  @Override
  public void clearIdCache() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String[] listPersisted(@Nonnull VirtualFile parent) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public FSRecords.NameId[] listAll(@Nonnull VirtualFile parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getId(@Nonnull VirtualFile parent, @Nonnull String childName, @Nonnull NewVirtualFileSystem delegate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName(int id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastRecordedLength(@Nonnull VirtualFile file) {
    return 0;
  }

  @Override
  public boolean exists(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSymLink(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String resolveSymLink(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile createChildDirectory(@Nullable Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile createChildFile(@Nullable Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public OutputStream getOutputStream(@Nonnull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLength(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHidden(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getFileAttributes(int id) {
    throw new UnsupportedOperationException();
  }

  @javax.annotation.Nullable
  @Override
  public NewVirtualFile findFileByIdIfCached(int id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int storeUnlinkedContent(@Nonnull byte[] bytes) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray(int contentId) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray(@Nonnull VirtualFile file, boolean cacheContent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int acquireContent(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void releaseContent(int contentId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getCurrentContentId(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @RequiredWriteAction
  @Override
  public void processEvents(@Nonnull List<VFileEvent> events) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewVirtualFileSystem replaceWithNativeFS(@Nonnull NewVirtualFileSystem fs) {
    return null;
  }

  @Nullable
  @Override
  public DataInputStream readAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att) {
    return new DataInputStream(new ByteArrayInputStream(new byte[100]));
  }

  @Nonnull
  @Override
  public DataOutputStream writeAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att) {
    return new DataOutputStream(new ByteArrayOutputStream(100));
  }

  @Override
  public int getModificationCount(@Nonnull VirtualFile fileOrDirectory) {
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
  public boolean areChildrenLoaded(@Nonnull VirtualFile dir) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean wereChildrenAccessed(@Nonnull VirtualFile dir) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public NewVirtualFile findRoot(@Nonnull String basePath, @Nonnull NewVirtualFileSystem fs) {
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

  @Nonnull
  @Override
  public VirtualFile[] getRoots() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public VirtualFile[] getRoots(@Nonnull NewVirtualFileSystem fs) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
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
