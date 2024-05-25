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

import consulo.container.internal.PathManagerHolder;
import consulo.util.nodep.io.FileUtilRt;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Set;

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
  protected static final String SCRATCHES_FOLDER = "scratches";

  public static ContainerPathManager get() {
    ContainerPathManager manager = PathManagerHolder.getInstance();
    if (manager == null) {
      throw new IllegalArgumentException("not initialized");
    }
    return manager;
  }

  public abstract String getHomePath();

  /**
   * @return external platform directory for mac, or platform directory inside application for other oses
   */
  public File getExternalPlatformDirectory() {
    return new File(getAppHomeDirectory(), PLATFORM_FOLDER);
  }

  /**
   * @return app home, equal IDE installation path
   */
  public abstract File getAppHomeDirectory();

  public String getBinPath() {
    return getHomePath() + File.separator + BIN_FOLDER;
  }

  public Path findIconInAppHomeDirectory() {
    File appHomeDirectory = getAppHomeDirectory();
    if (!appHomeDirectory.exists()) {
      return null;
    }

    Path[] svgIcon = new Path[1];
    Path[] pngIcon = new Path[1];
    try {

      Files.walkFileTree(appHomeDirectory.toPath(), Set.of(), 1, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String fileName = file.getFileName().toString();
          String extension = FileUtilRt.getExtension(fileName);
          if ("svg".equals(extension)) {
            svgIcon[0] = file;
          }

          if ("png".equals(extension)) {
            pngIcon[0] = file;
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (IOException ignored) {
    }

    if (svgIcon[0] != null) {
      return svgIcon[0];
    }

    return pngIcon[0];
  }

  public abstract String getConfigPath();

  public abstract String getSystemPath();

  public Path getSystemDir() {
    return Paths.get(getSystemPath());
  }

  public abstract File getDocumentsDir();

  /**
   * Looks for a file in all possible bin directories.
   *
   * @return first that exists, or {@code null} if nothing found.
   * @see #findBinFileWithException(String)
   */
  public File findBinFile(String fileName) {
    File file = new File(getBinPath(), fileName);
    if (file.isFile()) return file;
    return null;
  }

  public String getLibPath() {
    return getHomePath() + File.separator + LIB_FOLDER;
  }

  public String getScratchPath() {
    return getConfigPath() + File.separator + SCRATCHES_FOLDER;
  }

  public String getOptionsPath() {
    return getConfigPath() + File.separator + OPTIONS_FOLDER;
  }

  public File getOptionsFile(String fileName) {
    return new File(getOptionsPath(), fileName + ".xml");
  }

  public String[] getPluginsPaths() {
    return new String[]{getConfigPath() + File.separatorChar + PLUGINS_FOLDER};
  }

  public String getInstallPluginsPath() {
    String[] pluginsPaths = getPluginsPaths();
    if (pluginsPaths.length != 1) {
      throw new IllegalArgumentException("Plugins paths size is not equal one. Paths: " + Arrays.asList(pluginsPaths));
    }

    return pluginsPaths[0];
  }

  public String getTempPath() {
    return getSystemPath() + File.separator + "tmp";
  }

  public File getIndexRoot() {
    File file = new File(getSystemPath(), "index");
    file.mkdirs();
    return file;
  }

  public String getPluginTempPath() {
    return getSystemPath() + File.separator + PLUGINS_FOLDER;
  }

  public File getLogPath() {
    return new File(getSystemPath(), "logs");
  }

  public File findFileInLibDirectory(String relativePath) {
    return new File(getLibPath() + File.separator + relativePath);
  }
}
