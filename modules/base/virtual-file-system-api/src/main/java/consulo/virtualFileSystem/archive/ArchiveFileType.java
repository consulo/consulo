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

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 19:19/13.07.13
 */
public abstract class ArchiveFileType implements FileType {
  private final Supplier<ArchiveFileSystem> myFileSystemLazyValue;

  protected ArchiveFileType(VirtualFileManager virtualFileManager) {
    myFileSystemLazyValue = LazyValue.notNull(() -> {
      VirtualFileSystem fileSystem = virtualFileManager.getFileSystem(getProtocol());
      if (fileSystem == null) {
        throw new IllegalArgumentException("VirtualFileSystem with protocol: " + getProtocol() + " is not registered");
      }
      return (ArchiveFileSystem)fileSystem;
    });
  }

  @Nonnull
  public abstract String getProtocol();

  @Nonnull
  public ArchiveFileSystem getFileSystem() {
    return myFileSystemLazyValue.get();
  }

  @Override
  @Nonnull
  public LocalizeValue getDescription() {
    return IdeLocalize.filetypeDescriptionArchiveFiles();
  }

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return "";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.filetypesArchive();
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
