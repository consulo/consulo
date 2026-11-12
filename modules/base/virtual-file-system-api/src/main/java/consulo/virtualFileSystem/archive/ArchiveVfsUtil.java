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
package consulo.virtualFileSystem.archive;

import consulo.annotation.DeprecationInfo;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.LocalFileProvider;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.JBZipArchiveFile;
import consulo.virtualFileSystem.internal.JavaZipArchiveFile;
import consulo.virtualFileSystem.internal.PlatformArchives;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 19:39/13.07.13
 */
public class ArchiveVfsUtil {
  /**
   * The url of the root of the archive - of the local machine ({@code jar:///path/lib.jar!/}), or of another platform
   * ({@code platform-jar://<platform id>/path/lib.jar!/}) - without finding the file: nothing is read.
   *
   * @return {@code null} if the file is not an archive by its name
   */
  public static @Nullable String getArchiveRootUrl(Path archive) {
    Path fileName = archive.getFileName();
    if (fileName == null || !(FileTypeRegistry.getInstance().getFileTypeByFileName(fileName.toString()) instanceof ArchiveFileType fileType)) {
      return null;
    }
    return getArchiveRootUrl(fileType.getProtocol(), archive);
  }

  /**
   * The url of the root of the archive of the protocol - for an archive which is a directory, like {@code jrt}.
   */
  public static @Nullable String getArchiveRootUrl(String archiveProtocol, Path archive) {
    String url = VirtualFileManager.getInstance().getUrl(archive);
    if (url == null) {
      return null;
    }
    String protocol = VirtualFileManager.extractProtocol(url);
    String path = VirtualFileManager.extractPath(url);
    String archiveRootProtocol = StandardFileSystems.PLATFORM_PROTOCOL.equals(protocol)
      ? PlatformArchives.getPlatformProtocol(archiveProtocol)
      : archiveProtocol;
    return VirtualFileManager.constructUrl(archiveRootProtocol, path + URLUtil.ARCHIVE_SEPARATOR);
  }

  /**
   * A zip (a jar) - of the local machine, read natively; of another platform, read through its path.
   */
  public static ArchiveFile openZipArchiveFile(Path path) throws IOException {
    if (path.getFileSystem() == FileSystems.getDefault()) {
      return new JavaZipArchiveFile(path);
    }
    return new JBZipArchiveFile(path);
  }


  /**
   * Return mirror virtual file from archive file system if virtual file is archive
   */
  public static @Nullable VirtualFile getArchiveRootForLocalFile(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }
    FileType fileType = virtualFile.getFileType();
    if (fileType instanceof ArchiveFileType archiveFileType) {
      VirtualFileSystem fileSystem = PlatformArchives.getArchiveFileSystem(archiveFileType.getFileSystem(), virtualFile.getFileSystem());
      return fileSystem instanceof LocalFileProvider localFileProvider
        ? localFileProvider.findLocalVirtualFileByPath(virtualFile.getPath())
        : null;
    }
    return null;
  }

  /**
   * Return original file from local system, when file is mirror in archive file system
   */
  public static @Nullable VirtualFile getVirtualFileForArchive(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }

    if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
      return ((ArchiveFileSystem)virtualFile.getFileSystem()).getLocalVirtualFileFor(virtualFile);
    }
    return null;
  }

  @Deprecated(forRemoval = true)
  @DeprecationInfo(value = "Use #getArchiveRootForLocalFile()")
  public static @Nullable VirtualFile getJarRootForLocalFile(@Nullable VirtualFile virtualFile) {
    return getArchiveRootForLocalFile(virtualFile);
  }

  @Deprecated(forRemoval = true)
  @DeprecationInfo(value = "Use #getVirtualFileForArchive()")
  public static @Nullable VirtualFile getVirtualFileForJar(@Nullable VirtualFile virtualFile) {
    return getVirtualFileForArchive(virtualFile);
  }
}
