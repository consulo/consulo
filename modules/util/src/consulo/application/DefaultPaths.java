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
package consulo.application;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.SystemProperties;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
public abstract class DefaultPaths {
  private static class Fallback extends DefaultPaths {
    @NotNull
    @Override
    public String getDocumentsDir() {
      String userHome = SystemProperties.getUserHome();
      // some OS-es can have documents dir inside user home, for example Ubuntu
      File file = new File(userHome, "Documents");
      if (file.exists()) {
        return userHome + File.separatorChar + "Documents" + File.separatorChar + ourDefaultPrefix;
      }
      return userHome + File.separatorChar + ourDefaultPrefix + " Project";
    }

    @NotNull
    @Override
    public String getLocalSettingsDir() {
      String userHome = SystemProperties.getUserHome();
      return userHome + File.separatorChar + ".consulo_settings" + File.separatorChar + "system";
    }

    @NotNull
    @Override
    public String getRoamingSettingsDir() {
      String userHome = SystemProperties.getUserHome();
      return userHome + File.separatorChar + ".consulo_settings" + File.separatorChar + "config";
    }
  }

  private static abstract class SimpleDefaultPaths extends DefaultPaths {
    @NotNull
    protected abstract String getDocumentsDirNoPrefix();

    @Override
    @NotNull
    public String getDocumentsDir() {
      return getDocumentsDirNoPrefix() + File.separatorChar + ourDefaultPrefix;
    }

    @NotNull
    protected abstract String getLocalSettingsDirNoPrefix();

    @Override
    @NotNull
    public String getLocalSettingsDir() {
      return getLocalSettingsDirNoPrefix() + File.separatorChar + ourDefaultPrefix;
    }

    @NotNull
    protected abstract String getRoamingSettingsDirNoPrefix();

    @NotNull
    @Override
    public String getRoamingSettingsDir() {
      return getRoamingSettingsDirNoPrefix() + File.separatorChar + ourDefaultPrefix;
    }
  }

  private static class Mac extends SimpleDefaultPaths {
    @NotNull
    @Override
    protected String getDocumentsDirNoPrefix() {
      return SystemProperties.getUserHome() + "/Documents";
    }

    @NotNull
    @Override
    protected String getLocalSettingsDirNoPrefix() {
      return SystemProperties.getUserHome() + "/Library/Caches";
    }

    @NotNull
    @Override
    protected String getRoamingSettingsDirNoPrefix() {
      return SystemProperties.getUserHome() + "/Library/Preferences";
    }

    @NotNull
    @Override
    public String getRoamingPluginsDir() {
      return SystemProperties.getUserHome() + "/Library/Application Support/" + ourDefaultPrefix;
    }

    @NotNull
    @Override
    public String getLocalLogsDir() {
      return SystemProperties.getUserHome() + "/Library/Logs/" + ourDefaultPrefix;
    }
  }

  private static class Windows extends SimpleDefaultPaths {
    @NotNull
    @Override
    protected String getDocumentsDirNoPrefix() {
      return Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL);
    }

    @NotNull
    @Override
    protected String getLocalSettingsDirNoPrefix() {
      // will return path like C:\Users\{user.name}\AppData\Local
      return Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA);
    }

    @NotNull
    @Override
    protected String getRoamingSettingsDirNoPrefix() {
      // will return path like C:\Users\{user.name}\AppData\Roaming
      return Shell32Util.getFolderPath(ShlObj.CSIDL_APPDATA);
    }
  }

  private static final DefaultPaths ourInstance = get();
  private static final String ourDefaultPrefix = "Consulo";

  private static DefaultPaths get() {
    if (SystemInfo.isMac) {
      return new Mac();
    }
    else if (SystemInfo.isWindows) {
      return new Windows();
    }
    return new Fallback();
  }

  @NotNull
  public static DefaultPaths getInstance() {
    return ourInstance;
  }

  /**
   * @return default directory for new projects
   */
  @NotNull
  public abstract String getDocumentsDir();

  /**
   * @return directory for caches, etc. Removing will not broke user settings
   */
  @NotNull
  public abstract String getLocalSettingsDir();

  @NotNull
  public String getLocalLogsDir() {
    return getLocalSettingsDir() + File.separatorChar + "logs";
  }

  /**
   * @return directory for user settings
   */
  @NotNull
  public abstract String getRoamingSettingsDir();

  @NotNull
  public String getRoamingPluginsDir() {
    return getRoamingSettingsDir() + File.separatorChar + "plugins";
  }
}
