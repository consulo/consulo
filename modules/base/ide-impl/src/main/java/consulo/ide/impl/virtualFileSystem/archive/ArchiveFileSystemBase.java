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
package consulo.ide.impl.virtualFileSystem.archive;

import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.impl.jar.JarHandler;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ArchiveFileSystem;
import consulo.ide.impl.idea.openapi.vfs.newvfs.VfsImplUtil;
import consulo.platform.Platform;
import consulo.util.collection.Sets;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePointerCapableFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Aug-16
 * <p>
 * Base class for archive file types
 */
public abstract class ArchiveFileSystemBase extends ArchiveFileSystem implements consulo.virtualFileSystem.archive.ArchiveFileSystem, VirtualFilePointerCapableFileSystem {
  private final Set<String> myNoCopyJarPaths;
  private final String myProtocol;

  protected ArchiveFileSystemBase(@Nonnull String protocol) {
    myProtocol = protocol;
    boolean noCopy = SystemProperties.getBooleanProperty("idea.jars.nocopy", !Platform.current().os().isWindows());
    myNoCopyJarPaths = noCopy ? null : Sets.newConcurrentHashSet(FileUtil.PATH_HASHING_STRATEGY);
  }

  @Nonnull
  public abstract ArchiveFile createArchiveFile(@Nonnull String filePath) throws IOException;

  @Nonnull
  @Override
  public final String getProtocol() {
    return myProtocol;
  }

  @Override
  public boolean isMakeCopyOfJar(@Nonnull File originalJar) {
    return !(myNoCopyJarPaths == null || myNoCopyJarPaths.contains(originalJar.getPath()));
  }

  @Override
  public void setNoCopyJarForPath(String pathInJar) {
    if (myNoCopyJarPaths == null || pathInJar == null) return;
    int index = pathInJar.indexOf(URLUtil.ARCHIVE_SEPARATOR);
    if (index < 0) return;
    String path = FileUtil.toSystemIndependentName(pathInJar.substring(0, index));
    myNoCopyJarPaths.add(path);
  }

  @Nonnull
  @Override
  public String extractPresentableUrl(@Nonnull String path) {
    return super.extractPresentableUrl(StringUtil.trimEnd(path, URLUtil.ARCHIVE_SEPARATOR));
  }

  @Override
  public String normalize(@Nonnull String path) {
    final int jarSeparatorIndex = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
    if (jarSeparatorIndex > 0) {
      final String root = path.substring(0, jarSeparatorIndex);
      return FileUtil.normalize(root) + path.substring(jarSeparatorIndex);
    }
    return super.normalize(path);
  }

  @Nonnull
  @Override
  public String extractRootPath(@Nonnull String path) {
    final int jarSeparatorIndex = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
    assert jarSeparatorIndex >= 0 : "Path passed to ArchiveFileSystem must have archive separator '!/': " + path;
    return path.substring(0, jarSeparatorIndex + URLUtil.ARCHIVE_SEPARATOR.length());
  }

  @Nonnull
  @Override
  protected String extractLocalPath(@Nonnull String rootPath) {
    return StringUtil.trimEnd(rootPath, URLUtil.ARCHIVE_SEPARATOR);
  }

  @Nonnull
  @Override
  protected String composeRootPath(@Nonnull String localPath) {
    return localPath + URLUtil.ARCHIVE_SEPARATOR;
  }

  @Nonnull
  @Override
  protected JarHandler getHandler(@Nonnull VirtualFile entryFile) {
    return VfsImplUtil.getHandler(this, entryFile, JarHandler::new);
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull String path) {
    return VfsImplUtil.findFileByPath(this, path);
  }

  @Override
  public VirtualFile findFileByPathIfCached(@Nonnull String path) {
    return VfsImplUtil.findFileByPathIfCached(this, path);
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return VfsImplUtil.refreshAndFindFileByPath(this, path);
  }

  @Override
  public void refresh(boolean asynchronous) {
    VfsImplUtil.refresh(this, asynchronous);
  }

  @Nullable
  @Override
  public VirtualFile getLocalVirtualFileFor(@Nullable VirtualFile entryVFile) {
    return getVirtualFileForJar(entryVFile);
  }

  @Nullable
  @Override
  public VirtualFile findLocalVirtualFileByPath(@Nonnull String path) {
    if (!path.contains(URLUtil.ARCHIVE_SEPARATOR)) {
      path += URLUtil.ARCHIVE_SEPARATOR;
    }
    return findFileByPath(path);
  }

  @Nullable
  public VirtualFile getVirtualFileForJar(@Nullable VirtualFile entryFile) {
    return entryFile == null ? null : getLocalByEntry(entryFile);
  }

  @Nullable
  public VirtualFile getJarRootForLocalFile(@Nonnull VirtualFile file) {
    return getRootByLocal(file);
  }
}
