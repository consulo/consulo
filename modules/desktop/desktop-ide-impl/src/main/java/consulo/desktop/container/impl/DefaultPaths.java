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
package consulo.desktop.container.impl;

import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import consulo.platform.Platform;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
public abstract class DefaultPaths {
  private static class Fallback extends DefaultPaths {
    @Nonnull
    @Override
    public File getDocumentsDir() {
      File userHome = Platform.current().user().homePath().toFile();
      // some OS-es can have documents dir inside user home, for example Ubuntu
      File file = new File(userHome, "Documents");
      if (file.exists()) {
        return new File(userHome, "Documents" + File.separatorChar + ourDefaultPrefix);
      }
      return new File(userHome, ourDefaultPrefix + " Project");
    }

    @Nonnull
    @Override
    public String getLocalSettingsDir() {
      String userHome = Platform.current().user().homePath().toString();
      return userHome + File.separatorChar + ".consulo_settings" + File.separatorChar + "system";
    }

    @Nonnull
    @Override
    public String getRoamingSettingsDir() {
      String userHome = Platform.current().user().homePath().toString();
      return userHome + File.separatorChar + ".consulo_settings" + File.separatorChar + "config";
    }
  }

  private static abstract class SimpleDefaultPaths extends DefaultPaths {
    @Nonnull
    protected abstract String getDocumentsDirNoPrefix();

    @Override
    @Nonnull
    public File getDocumentsDir() {
      return new File(getDocumentsDirNoPrefix(), ourDefaultPrefix);
    }

    @Nonnull
    protected abstract String getLocalSettingsDirNoPrefix();

    @Override
    @Nonnull
    public String getLocalSettingsDir() {
      return getLocalSettingsDirNoPrefix() + File.separatorChar + ourDefaultPrefix;
    }

    @Nonnull
    protected abstract String getRoamingSettingsDirNoPrefix();

    @Nonnull
    @Override
    public String getRoamingSettingsDir() {
      return getRoamingSettingsDirNoPrefix() + File.separatorChar + ourDefaultPrefix;
    }
  }

  private static class Mac extends SimpleDefaultPaths {
    @Nonnull
    @Override
    protected String getDocumentsDirNoPrefix() {
      return Platform.current().user().homePath() + "/Documents";
    }

    @Nonnull
    @Override
    public File getExternalPlatformDirectory(@Nonnull File defaultPath) {
      return new File(
        Platform.current().user().homePath().toFile(),
        "Library/Application Support/Consulo Platform"
      );
    }

    @Nonnull
    @Override
    protected String getLocalSettingsDirNoPrefix() {
      return Platform.current().user().homePath() + "/Library/Caches";
    }

    @Nonnull
    @Override
    protected String getRoamingSettingsDirNoPrefix() {
      return Platform.current().user().homePath() + "/Library/Preferences";
    }

    @Nonnull
    @Override
    public String getRoamingPluginsDir() {
      return Platform.current().user().homePath() + "/Library/Application Support/" + ourDefaultPrefix;
    }

    @Nonnull
    @Override
    public File getLocalLogsDir() {
      return new File(Platform.current().user().homePath().toFile(),"Library/Logs/" + ourDefaultPrefix);
    }
  }

  private static class Windows extends SimpleDefaultPaths {
    @Nonnull
    @Override
    public File getExternalPlatformDirectory(@Nonnull File defaultPath) {
      return new File(Shell32Util.getFolderPath(ShlObj.CSIDL_APPDATA), "Consulo Platform");
    }

    @Nonnull
    @Override
    protected String getDocumentsDirNoPrefix() {
      return Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL);
    }

    @Nonnull
    @Override
    protected String getLocalSettingsDirNoPrefix() {
      // will return path like C:\Users\{user.name}\AppData\Local
      return Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA);
    }

    @Nonnull
    @Override
    protected String getRoamingSettingsDirNoPrefix() {
      // will return path like C:\Users\{user.name}\AppData\Roaming
      return Shell32Util.getFolderPath(ShlObj.CSIDL_APPDATA);
    }
  }

  private static final DefaultPaths ourInstance = get();
  private static final String ourDefaultPrefix = "Consulo";

  private static DefaultPaths get() {
    if (Platform.current().os().isMac()) {
      return new Mac();
    }
    else if (Platform.current().os().isWindows()) {
      return new Windows();
    }
    return new Fallback();
  }

  @Nonnull
  public static DefaultPaths getInstance() {
    return ourInstance;
  }

  /**
   * @return default directory for new projects
   */
  @Nonnull
  public abstract File getDocumentsDir();

  /**
   * @return directory for caches, etc. Removing will not broke user settings
   */
  @Nonnull
  public abstract String getLocalSettingsDir();

  @Nonnull
  public File getLocalLogsDir() {
    return new File(getLocalSettingsDir(), "logs");
  }

  /**
   * @return directory for user settings
   */
  @Nonnull
  public abstract String getRoamingSettingsDir();

  @Nonnull
  public String getRoamingPluginsDir() {
    return getRoamingSettingsDir() + File.separatorChar + "plugins";
  }

  /**
   * @return external platform directory, on each updates directory will be install in it, or inside distribution directory if null
   */
  @Nonnull
  public File getExternalPlatformDirectory(@Nonnull File defaultPath) {
    return defaultPath;
  }
}
