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
package consulo.desktop.awt.platform.impl;

import consulo.desktop.startup.DesktopPlatformUserImpl;
import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.ide.impl.idea.ide.actions.ShowFilePathAction;
import consulo.platform.PlatformFileSystem;
import consulo.platform.PlatformUser;
import consulo.platform.impl.PlatformBase;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
class DesktopAWTPlatformImpl extends PlatformBase {
  public DesktopAWTPlatformImpl() {
    super(LOCAL, LOCAL, getSystemJvmProperties());
  }

  @Override
  public void openInBrowser(@Nonnull URL url) {
    BrowserUtil.browse(url);
  }

  @Override
  public void openFileInFileManager(@Nonnull File file) {
    ShowFilePathAction.openFile(file);
  }

  @Override
  public void openDirectoryInFileManager(@Nonnull File file) {
    ShowFilePathAction.openDirectory(file);
  }

  @Nonnull
  @Override
  protected PlatformUser createUser(Map<String, String> jvmProperties) {
    return new DesktopPlatformUserImpl(this, jvmProperties);
  }

  @Nonnull
  @Override
  protected PlatformFileSystem createFS(Map<String, String> jvmProperties) {
    return new DesktopAWTFileSystemImpl(this, jvmProperties);
  }
}
