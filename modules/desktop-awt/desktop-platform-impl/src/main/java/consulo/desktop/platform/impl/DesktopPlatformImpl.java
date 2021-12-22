/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.platform.impl;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.win32.WindowsElevationUtil;
import consulo.platform.impl.PlatformBase;
import consulo.ui.desktop.internal.image.DesktopImageOverIconImpl;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
class DesktopPlatformImpl extends PlatformBase {
  static class DesktopFileSystemImpl extends FileSystemImpl {
    @Nullable
    @Override
    public Image getImage(@Nonnull File file) {
      Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
      return systemIcon == null ? null : new DesktopImageOverIconImpl(systemIcon);
    }
  }

  static class DesktopUserImpl extends UserImpl {
    @Override
    public boolean superUser() {
      // this is correct ?
      if (SystemInfo.isUnix && "root".equals(System.getenv("USER"))) {
        return true;
      }
      return WindowsElevationUtil.isUnderElevation();
    }
  }

  public DesktopPlatformImpl() {
  }

  @Nonnull
  @Override
  protected User createUser() {
    return new DesktopUserImpl();
  }

  @Nonnull
  @Override
  protected FileSystem createFS() {
    return new DesktopFileSystemImpl();
  }
}
