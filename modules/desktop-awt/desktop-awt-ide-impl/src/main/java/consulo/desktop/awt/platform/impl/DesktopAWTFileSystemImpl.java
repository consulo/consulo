/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.platform.impl;

import consulo.desktop.awt.ui.impl.image.DesktopImageOverIconImpl;
import consulo.platform.Platform;
import consulo.platform.impl.PlatformFileSystemImpl;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.Map;

/**
* @author VISTALL
* @since 25/04/2023
*/
class DesktopAWTFileSystemImpl extends PlatformFileSystemImpl {
  public DesktopAWTFileSystemImpl(Platform platform, Map<String, String> jvmProperties) {
    super(platform, jvmProperties);
  }

  @Nullable
  @Override
  public Image getImage(@Nonnull File file) {
    Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
    return systemIcon == null ? null : new DesktopImageOverIconImpl(systemIcon);
  }
}
