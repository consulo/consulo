/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.component.ComponentManager;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightFileTypeRegistry extends FileTypeRegistry {
  private final Map<String, FileType> myExtensionsMap = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
  private final List<FileType> myAllFileTypes = new ArrayList<>();

  @Inject
  public LightFileTypeRegistry() {
    myAllFileTypes.add(UnknownFileType.INSTANCE);
  }

  @Override
  public boolean isFileIgnored(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isFileOfType(@Nonnull VirtualFile file, @Nonnull FileType type) {
    return file.getFileType() == type;
  }

  @Nonnull
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
  public FileType getFileTypeByFileName(@Nonnull String fileName) {
    final String extension = FileUtil.getExtension(fileName);
    return getFileTypeByExtension(extension);
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull CharSequence fileName) {
    final String extension = FileUtil.getExtension(fileName.toString());
    return getFileTypeByExtension(extension);
  }

  @Nonnull
  @Override
  public FileType getFileTypeByExtension(@Nonnull String extension) {
    final FileType result = myExtensionsMap.get(extension);
    return result == null ? UnknownFileType.INSTANCE : result;
  }

  public void registerFileType(@Nonnull FileType fileType, @Nonnull String extension) {
    myAllFileTypes.add(fileType);
    for (final String ext : extension.split(";")) {
      myExtensionsMap.put(ext, fileType);
    }
  }

  @Nullable
  @Override
  public FileType findFileTypeByName(@Nonnull String fileTypeName) {
    for (FileType type : myAllFileTypes) {
      if (type.getId().equals(fileTypeName)) {
        return type;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nonnull ComponentManager project) {
    return null;
  }
}
