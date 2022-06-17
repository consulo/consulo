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
package consulo.ide.impl.idea.openapi.roots.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.openapi.roots.ui.util.*;
import consulo.ide.ui.CellAppearanceEx;
import consulo.ide.ui.FileAppearanceService;
import consulo.language.file.FileTypeManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.http.HttpFileSystem;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class FileAppearanceServiceImpl extends FileAppearanceService {
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

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRenderForInvalidUrl(@Nonnull String url) {
    return it -> {
      it.setIcon(PlatformIconGroup.nodesPpinvalid());
      it.append(url, SimpleTextAttributes.ERROR_ATTRIBUTES);
    };
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRenderForVirtualFile(@Nonnull VirtualFile file) {
    if (!file.isValid()) {
      return getRenderForInvalidUrl(file.getPresentableUrl());
    }

    return it -> forVirtualFile(file).customize(it);
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRenderForIoFile(@Nonnull File file) {
    final String absolutePath = file.getAbsolutePath();
    if (!file.exists()) {
      return getRenderForInvalidUrl(absolutePath);
    }

    if (file.isDirectory()) {
      return it -> {
        it.setIcon(PlatformIconGroup.nodesFolder());
        it.append(absolutePath);
      };
    }

    return it -> {
      final String name = file.getName();
      final FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(name);
      final File parent = file.getParentFile();

      it.setIcon(fileType.getIcon());
      it.append(name);
      it.append(" (" + parent.getAbsolutePath() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    };
  }
}
