/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.boot;

import consulo.container.boot.internal.PathManagerHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2019-12-06
 */
public abstract class ContainerPathManager {
  private static final String PLATFORM_FOLDER = "platform";
  private static final String LIB_FOLDER = "lib";
  private static final String PLUGINS_FOLDER = "plugins";
  private static final String BIN_FOLDER = "bin";
  private static final String OPTIONS_FOLDER = "options";
  private static final String SCRATCHES_FOLDER = "scratches";

  @Nonnull
  public static ContainerPathManager get() {
    ContainerPathManager manager = PathManagerHolder.getInstance();
    if (manager == null) {
      throw new IllegalArgumentException("not initialized");
    }
    return manager;
  }

  @Nonnull
  public abstract String getHomePath();

  /**
   * @return external platform directory for mac, or platform directory inside application for other oses
   */
  @Nonnull
  public File getExternalPlatformDirectory() {
    return new File(getAppHomeDirectory(), PLATFORM_FOLDER);
  }

  /**
   * @return app home, equal IDE installation path
   */
  @Nonnull
  public abstract File getAppHomeDirectory();

  @Nonnull
  public String getBinPath() {
    return getHomePath() + File.separator + BIN_FOLDER;
  }

  @Nonnull
  public abstract String getConfigPath();

  @Nonnull
  public abstract String getSystemPath();

  @Nonnull
  public abstract File getDocumentsDir();

  /**
   * Looks for a file in all possible bin directories.
   *
   * @return first that exists, or {@code null} if nothing found.
   * @see #findBinFileWithException(String)
   */
  @Nullable
  public File findBinFile(@Nonnull String fileName) {
    File file = new File(getBinPath(), fileName);
    if (file.isFile()) return file;
    return null;
  }

  @Nonnull
  public String getLibPath() {
    return getHomePath() + File.separator + LIB_FOLDER;
  }

  @Nonnull
  public String getScratchPath() {
    return getConfigPath();
  }

  @Nonnull
  public String getOptionsPath() {
    return getConfigPath() + File.separator + OPTIONS_FOLDER;
  }

  @Nonnull
  public File getOptionsFile(@Nonnull String fileName) {
    return new File(getOptionsPath(), fileName + ".xml");
  }

  @Nonnull
  public String[] getPluginsPaths() {
    return new String[]{getConfigPath() + File.separatorChar + PLUGINS_FOLDER};
  }

  @Nonnull
  public String getInstallPluginsPath() {
    String[] pluginsPaths = getPluginsPaths();
    if (pluginsPaths.length != 1) {
      throw new IllegalArgumentException("Plugins paths size is not equal one. Paths: " + Arrays.asList(pluginsPaths));
    }

    return pluginsPaths[0];
  }

  @Nonnull
  public String getTempPath() {
    return getSystemPath() + File.separator + "tmp";
  }

  @Nonnull
  public File getIndexRoot() {
    File file = new File(getSystemPath(), "index");
    file.mkdirs();
    return file;
  }

  @Nonnull
  public String getPluginTempPath() {
    return getSystemPath() + File.separator + PLUGINS_FOLDER;
  }

  @Nonnull
  public File getLogPath() {
    return new File(getSystemPath(), "logs");
  }

  @Nonnull
  public File findFileInLibDirectory(@Nonnull String relativePath) {
    return new File(getLibPath() + File.separator + relativePath);
  }
}
