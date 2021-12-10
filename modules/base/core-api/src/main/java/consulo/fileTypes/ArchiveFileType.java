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
package consulo.fileTypes;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.image.Image;
import consulo.vfs.ArchiveFileSystem;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19:19/13.07.13
 */
public abstract class ArchiveFileType implements FileType {
  private final NotNullLazyValue<ArchiveFileSystem> myFileSystemLazyValue = NotNullLazyValue.createValue(() -> {
    VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(getProtocol());
    if(fileSystem == null) {
      throw new IllegalArgumentException("VirtualFileSystem with protocol: " + getProtocol() + " is not registered");
    }
    return (ArchiveFileSystem) fileSystem;
  });

  @Nonnull
  public abstract String getProtocol();

  @Nonnull
  public ArchiveFileSystem getFileSystem() {
    return myFileSystemLazyValue.getValue();
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
    return AllIcons.FileTypes.Archive;
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
