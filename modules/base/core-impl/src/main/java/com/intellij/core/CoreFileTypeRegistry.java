/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.core;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.util.collection.Maps;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
@Deprecated
public class CoreFileTypeRegistry extends FileTypeRegistry {
  private final Map<String, FileType> myExtensionsMap = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
  private final List<FileType> myAllFileTypes = new ArrayList<FileType>();

  public CoreFileTypeRegistry() {
    myAllFileTypes.add(UnknownFileType.INSTANCE);
  }

  @Override
  public boolean isFileIgnored(@NonNls @Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isFileOfType(@Nonnull VirtualFile file, @Nonnull FileType type) {
    return file.getFileType() == type;
  }

  @Override
  public FileType[] getRegisteredFileTypes() {
    return myAllFileTypes.toArray(new FileType[myAllFileTypes.size()]);
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFile(@Nonnull VirtualFile file) {
    return getFileTypeByFileName(file.getName());
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull @NonNls String fileName) {
    final String extension = FileUtilRt.getExtension(fileName);
    return getFileTypeByExtension(extension);
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull @NonNls CharSequence fileName) {
    final String extension = FileUtilRt.getExtension(fileName.toString());
    return getFileTypeByExtension(extension);
  }

  @Nonnull
  @Override
  public FileType getFileTypeByExtension(@NonNls @Nonnull String extension) {
    final FileType result = myExtensionsMap.get(extension);
    return result == null ? UnknownFileType.INSTANCE : result;
  }

  public void registerFileType(@Nonnull FileType fileType, @Nonnull @NonNls String extension) {
    myAllFileTypes.add(fileType);
    for (final String ext : extension.split(";")) {
      myExtensionsMap.put(ext, fileType);
    }
  }


  @Nullable
  @Override
  public FileType findFileTypeByName(String fileTypeName) {
    for (FileType type : myAllFileTypes) {
      if (type.getName().equals(fileTypeName)) {
        return type;
      }
    }
    return null;
  }
}
