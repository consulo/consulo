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

import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.ide.impl.idea.ide.actions.ShowFilePathAction;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.desktop.awt.ui.impl.image.DesktopImageOverIconImpl;
import consulo.desktop.util.windows.WindowsElevationUtil;
import consulo.platform.Platform;
import consulo.platform.impl.PlatformBase;
import consulo.ui.image.Image;
import consulo.util.jna.JnaLoader;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.net.URL;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
class DesktopAWTPlatformImpl extends PlatformBase {
  static class DesktopFileSystemImpl extends FileSystemImpl {
    @Nullable
    @Override
    public Image getImage(@Nonnull File file) {
      Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
      return systemIcon == null ? null : new DesktopImageOverIconImpl(systemIcon);
    }
  }

  static class DesktopUserImpl extends UserImpl {
    private final Platform myPlatform;

    DesktopUserImpl(Platform platform) {
      myPlatform = platform;
    }

    @Override
    public boolean superUser() {
      // this is correct ?
      if (myPlatform.os().isUnix() && "root".equals(System.getenv("USER"))) {
        return true;
      }
      return WindowsElevationUtil.isUnderElevation();
    }

    @Override
    public boolean darkTheme() {
      if (myPlatform.os().isWindows()) {
        return checkWindowsDarkTheme();
      }
      else if (myPlatform.os().isMacMojave()) {
        return checkMacOsDarkTheme();
      }
      return false;
    }

    private static final String ourRegistryPath = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String ourRegistryValue = "AppsUseLightTheme";

    private static boolean checkWindowsDarkTheme() {
      if (!JnaLoader.isLoaded()) {
        return false;
      }

      try {
        return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, ourRegistryPath, ourRegistryValue) &&
               Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, ourRegistryPath, ourRegistryValue) == 0;
      }
      catch (Exception ignored) {
      }
      return false;
    }

    private static boolean checkMacOsDarkTheme() {
      Foundation.NSAutoreleasePool pool = new Foundation.NSAutoreleasePool();
      try {
        // https://developer.apple.com/forums/thread/118974
        ID userDefaults = Foundation.invoke("NSUserDefaults", "standardUserDefaults");
        String appleInterfaceStyle = Foundation.toStringViaUTF8(Foundation.invoke(userDefaults, "objectForKey:", Foundation.nsString("AppleInterfaceStyle")));

        //val autoMode = SystemInfo.isMacOSCatalina &&
        //               Foundation.invoke(userDefaults, "boolForKey:", Foundation.nsString("AppleInterfaceStyleSwitchesAutomatically")).booleanValue()

        return StringUtil.notNullize(appleInterfaceStyle).toLowerCase(Locale.ROOT).contains("dark");
      }
      finally {
        pool.drain();
      }
    }
  }

  public DesktopAWTPlatformImpl() {
    super(getSystemJvmProperties());
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
  protected User createUser() {
    return new DesktopUserImpl(this);
  }

  @Nonnull
  @Override
  protected FileSystem createFS() {
    return new DesktopFileSystemImpl();
  }
}
