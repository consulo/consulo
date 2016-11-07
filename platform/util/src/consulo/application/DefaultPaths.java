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
    protected String getUserDocumentsDir() {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String getDirectoryForProjects() {
      String userHome = SystemProperties.getUserHome();
      // some OS can have documents dir inside user home, for example Ubuntu
      File file = new File(userHome, "Documents");
      if (file.exists()) {
        return userHome + File.separatorChar + "Documents" + File.separatorChar + "Consulo";
      }
      return userHome + File.separatorChar + "Consulo Project";
    }
  }

  private static class Mac extends DefaultPaths {
    @NotNull
    @Override
    protected String getUserDocumentsDir() {
      return SystemProperties.getUserHome() + File.separatorChar + "Documents";
    }
  }

  private static class Windows extends DefaultPaths {
    @NotNull
    @Override
    protected String getUserDocumentsDir() {
      return Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL);
    }
  }

  private static final DefaultPaths ourInstance = get();

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

  @NotNull
  public String getDirectoryForProjects() {
    return getUserDocumentsDir() + File.separatorChar + "Consulo";
  }

  @NotNull
  protected abstract String getUserDocumentsDir();
}
