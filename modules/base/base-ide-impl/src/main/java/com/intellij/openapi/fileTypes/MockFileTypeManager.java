/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class MockFileTypeManager extends FileTypeManager {

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull @NonNls String fileName) {
    return MockLanguageFileType.INSTANCE;
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull @NonNls CharSequence fileName) {
    return MockLanguageFileType.INSTANCE;
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFile(@Nonnull VirtualFile file) {
    return MockLanguageFileType.INSTANCE;
  }

  @Nonnull
  @Override
  public FileType getFileTypeByExtension(@NonNls @Nonnull String extension) {
    return MockLanguageFileType.INSTANCE;
  }

  @Nonnull
  @Override
  public FileType[] getRegisteredFileTypes() {
    return new FileType[] {MockLanguageFileType.INSTANCE};
  }

  @Override
  public boolean isFileIgnored(@NonNls @Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public void registerFileType(@Nonnull FileType type, @Nonnull List<? extends FileNameMatcher> defaultAssociations) {

  }

  @Override
  public boolean isFileIgnored(@NonNls @Nonnull String name) {
    return false;
  }

  @Nonnull
  @Override
  public String[] getAssociatedExtensions(@Nonnull FileType type) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @Override
  public List<FileNameMatcher> getAssociations(@Nonnull FileType type) {
    return Collections.emptyList();
  }

  @Override
  public void addFileTypeListener(@Nonnull FileTypeListener listener) {
  }

  @Override
  public void removeFileTypeListener(@Nonnull FileTypeListener listener) {
  }

  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file) {
    return file.getFileType();
  }

  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nonnull Project project) {
    return getKnownFileTypeOrAssociate(file);
  }

  @Nonnull
  @Override
  public Set<String> getIgnoredFiles() {
    return Set.of();
  }

  @Override
  public void setIgnoredFiles(@Nonnull Set<String> list) {
  }

  @Override
  public void associate(@Nonnull FileType type, @Nonnull FileNameMatcher matcher) {
  }

  @Override
  public void removeAssociation(@Nonnull FileType type, @Nonnull FileNameMatcher matcher) {
  }

  @Nonnull
  @Override
  public FileType getStdFileType(@Nonnull @NonNls String fileTypeName) {
    return MockLanguageFileType.INSTANCE;
  }

  public boolean isFileOfType(VirtualFile file, FileType type) {
    return false;
  }

  @Nullable
  @Override
  public FileType findFileTypeByName(String fileTypeName) {
    return null;
  }
}
