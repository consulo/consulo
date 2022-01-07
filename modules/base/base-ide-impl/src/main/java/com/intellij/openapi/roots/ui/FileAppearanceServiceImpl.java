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
package com.intellij.openapi.roots.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.roots.ui.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.ex.http.HttpFileSystem;
import com.intellij.ui.SimpleColoredComponent;
import consulo.vfs.ArchiveFileSystem;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.io.File;

@Singleton
public class FileAppearanceServiceImpl extends FileAppearanceService {
  private static CellAppearanceEx EMPTY = new CellAppearanceEx() {
    @Override
    public void customize(@Nonnull SimpleColoredComponent component) { }

    @Nonnull
    @Override
    public String getText() { return ""; }
  };

  @Nonnull
  @Override
  public CellAppearanceEx empty() {
    return EMPTY;
  }

  @Nonnull
  @Override
  public CellAppearanceEx forVirtualFile(@Nonnull final VirtualFile file) {
    if (!file.isValid()) {
      return forInvalidUrl(file.getPresentableUrl());
    }

    final VirtualFileSystem fileSystem = file.getFileSystem();
    if (fileSystem instanceof ArchiveFileSystem) {
      return new JarSubfileCellAppearance(file);
    }
    if (fileSystem instanceof HttpFileSystem) {
      return new HttpUrlCellAppearance(file);
    }
    if (file.isDirectory()) {
      return SimpleTextCellAppearance.regular(file.getPresentableUrl(), AllIcons.Nodes.Folder);
    }
    return new ValidFileCellAppearance(file);
  }

  @Nonnull
  @Override
  public CellAppearanceEx forIoFile(@Nonnull final File file) {
    final String absolutePath = file.getAbsolutePath();
    if (!file.exists()) {
      return forInvalidUrl(absolutePath);
    }

    if (file.isDirectory()) {
      return SimpleTextCellAppearance.regular(absolutePath, AllIcons.Nodes.Folder);
    }

    final String name = file.getName();
    final FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(name);
    final File parent = file.getParentFile();
    final CompositeAppearance appearance = CompositeAppearance.textComment(name, parent.getAbsolutePath());
    appearance.setIcon(fileType.getIcon());
    return appearance;
  }

  @Override
  @Nonnull
  public CellAppearanceEx forInvalidUrl(@Nonnull final String text) {
    return SimpleTextCellAppearance.invalid(text, AllIcons.Nodes.PpInvalid);
  }
}
