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
package com.intellij.util;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import consulo.vfs.ArchiveFileSystem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PathUtil {
  private PathUtil() {
  }

  @Nullable
  public static String getLocalPath(@Nullable VirtualFile file) {
    if (file == null || !file.isValid()) {
      return null;
    }
    if (file.getFileSystem().getProtocol().equals(ArchiveFileSystem.ARCHIVE_SEPARATOR) && file.getParent() != null) {
      return null;
    }
    return getLocalPath(file.getPath());
  }

  @Nonnull
  public static String getLocalPath(@Nonnull String path) {
    return FileUtil.toSystemDependentName(StringUtil.trimEnd(path, ArchiveFileSystem.ARCHIVE_SEPARATOR));
  }

  @Nonnull
  public static VirtualFile getLocalFile(@Nonnull VirtualFile file) {
    if (!file.isValid()) {
      return file;
    }
    if (file.getFileSystem() instanceof LocalFileProvider) {
      final VirtualFile localFile = ((LocalFileProvider)file.getFileSystem()).getLocalVirtualFileFor(file);
      if (localFile != null) {
        return localFile;
      }
    }
    return file;
  }

  @Nonnull
  public static String getJarPathForClass(@Nonnull Class aClass) {
    final String pathForClass = PathManager.getJarPathForClass(aClass);
    assert pathForClass != null : aClass;
    return pathForClass;
  }

  @Nonnull
  public static String toPresentableUrl(@Nonnull String url) {
    return getLocalPath(VirtualFileManager.extractPath(url));
  }

  public static String getCanonicalPath(@NonNls String path) {
    return FileUtil.toCanonicalPath(path);
  }

  @Nonnull
  public static String getFileName(@Nonnull String path) {
    return PathUtilRt.getFileName(path);
  }

  @Nullable
  public static String getFileExtension(@Nonnull String name) {
    int index = name.lastIndexOf('.');
    if (index < 0) return null;
    return name.substring(index + 1);
  }

  @Nonnull
  public static String getParentPath(@Nonnull String path) {
    return PathUtilRt.getParentPath(path);
  }

  @Nonnull
  public static String suggestFileName(@Nonnull String text) {
    return PathUtilRt.suggestFileName(text);
  }

  @Nonnull
  public static String suggestFileName(@Nonnull String text, final boolean allowDots, final boolean allowSpaces) {
    return PathUtilRt.suggestFileName(text, allowDots, allowSpaces);
  }

  public static boolean isValidFileName(@Nonnull String fileName) {
    return PathUtilRt.isValidFileName(fileName, true);
  }

  public static boolean isValidFileName(@Nonnull String fileName, boolean strict) {
    return PathUtilRt.isValidFileName(fileName, strict);
  }

  @Contract("null -> null; !null -> !null")
  public static String toSystemIndependentName(@Nullable String path) {
    return path == null ? null : FileUtilRt.toSystemIndependentName(path);
  }


  @Contract("null -> null; !null -> !null")
  public static String toSystemDependentName(@Nullable String path) {
    return path == null ? null : FileUtilRt.toSystemDependentName(path);
  }

  @Nonnull
  public static String makeFileName(@Nonnull String name, @Nullable String extension) {
    return name + (StringUtil.isEmpty(extension) ? "" : "." + extension);
  }
}
