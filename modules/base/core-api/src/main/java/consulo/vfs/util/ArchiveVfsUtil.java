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
package consulo.vfs.util;

import consulo.fileTypes.ArchiveFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.impl.archive.ArchiveEntry;
import consulo.vfs.impl.archive.ArchiveFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.annotation.DeprecationInfo;

import java.io.*;
import java.util.Iterator;

/**
 * @author VISTALL
 * @since 19:39/13.07.13
 */
public class ArchiveVfsUtil {

  /**
   * Return mirror virtual file from archive file system if virtual file is archive
   */
  @Nullable
  public static VirtualFile getArchiveRootForLocalFile(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }
    final FileType fileType = virtualFile.getFileType();
    if (fileType instanceof ArchiveFileType) {
      return ((ArchiveFileType)fileType).getFileSystem().findLocalVirtualFileByPath(virtualFile.getPath());
    }
    return null;
  }

  /**
   * Return original file from local system, when file is mirror in archive file system
   */
  @Nullable
  public static VirtualFile getVirtualFileForArchive(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }

    if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
      return ((ArchiveFileSystem)virtualFile.getFileSystem()).getLocalVirtualFileFor(virtualFile);
    }
    return null;
  }

  @Deprecated
  @DeprecationInfo(value = "Use #getArchiveRootForLocalFile()", until = "2.0")
  @Nullable
  public static VirtualFile getJarRootForLocalFile(@Nullable VirtualFile virtualFile) {
    return getArchiveRootForLocalFile(virtualFile);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #getVirtualFileForArchive()", until = "2.0")
  @Nullable
  public static VirtualFile getVirtualFileForJar(@Nullable VirtualFile virtualFile) {
    return getVirtualFileForArchive(virtualFile);
  }

  public static void extract(final @Nonnull ArchiveFile zipFile, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter)
          throws IOException {
    extract(zipFile, outputDir, filenameFilter, true);
  }

  public static void extract(final @Nonnull ArchiveFile zipFile,
                             @Nonnull File outputDir,
                             @Nullable FilenameFilter filenameFilter,
                             boolean overwrite) throws IOException {
    final Iterator<? extends ArchiveEntry> entries = zipFile.entries();
    while (entries.hasNext()) {
      ArchiveEntry entry = entries.next();
      final File file = new File(outputDir, entry.getName());
      if (filenameFilter == null || filenameFilter.accept(file.getParentFile(), file.getName())) {
        extractEntry(entry, zipFile.getInputStream(entry), outputDir, overwrite);
      }
    }
  }

  public static void extractEntry(ArchiveEntry entry, final InputStream inputStream, File outputDir) throws IOException {
    extractEntry(entry, inputStream, outputDir, true);
  }

  public static void extractEntry(ArchiveEntry entry, final InputStream inputStream, File outputDir, boolean overwrite) throws IOException {
    final boolean isDirectory = entry.isDirectory();
    final String relativeName = entry.getName();
    final File file = new File(outputDir, relativeName);
    if (file.exists() && !overwrite) return;

    FileUtil.createParentDirs(file);
    if (isDirectory) {
      file.mkdir();
    }
    else {
      final BufferedInputStream is = new BufferedInputStream(inputStream);
      final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        FileUtil.copy(is, os);
      }
      finally {
        os.close();
        is.close();
      }
    }
  }
}
