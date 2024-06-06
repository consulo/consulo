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
package consulo.desktop.container.impl;

import consulo.application.ApplicationProperties;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static consulo.util.lang.SystemProperties.getUserHome;

/**
 * @author VISTALL
 * @since 2019-12-07
 */
public class DesktopContainerPathManager extends ContainerPathManager {
  public static final String[] PROPERTIES_FILE = {
    "consulo.properties.file",
    "idea.properties.file"
  };

  public static final String[] PROPERTY_SYSTEM_PATH = {
    "consulo.system.path",
    "idea.system.path"
  };

  public static final String[] PROPERTY_SCRATCH_PATH = {
    "consulo.scratch.path",
    "idea.scratch.path",
  };

  public static final String[] PROPERTY_CONFIG_PATH = {
    "consulo.config.path",
    "idea.config.path"
  };

  public static final String[] PROPERTY_HOME_PATH = {
    "consulo.home.path",
    "idea.home.path"
  };

  public static final String[] PROPERTY_LOG_PATH = {
    "consulo.log.path",
    "idea.log.path"
  };

  public static final String[] CONSULO_PLUGINS_PATHS = {
    "consulo.plugins.paths",
    "idea.plugins.path"
  };

  public static final String CONSULO_INSTALL_PLUGINS_PATH = "consulo.install.plugins.path";
  public static final String CONSULO_APP_HOME_PATH = "consulo.app.home.path";
  public static final String CONSULO_NO_EXTERNAL_PLATFORM = "consulo.no.external.platform";

  private static final String PLATFORM_FOLDER = "platform";
  private static final String LIB_FOLDER = "lib";
  private static final String PLUGINS_FOLDER = "plugins";
  private static final String BIN_FOLDER = "bin";
  private static final String OPTIONS_FOLDER = "options";

  private static String ourHomePath;
  private static String ourSystemPath;
  private static String ourConfigPath;
  private static String ourScratchPath;
  private static File ourLogPath;

  private static String ourInstallPluginsPath;
  private static String[] ourPluginsPaths;

  /**
   * @return home path of platform (in most cases path is $APP_HOME_PATH$/platform/$HOME_PATH$)
   */
  @Override
  @Nonnull
  public String getHomePath() {
    if (ourHomePath != null) return ourHomePath;

    String propertyValue = selectPropertyValue(PROPERTY_HOME_PATH);
    if (propertyValue != null) {
      ourHomePath = getAbsolutePath(propertyValue);
      if (!new File(ourHomePath).isDirectory()) {
        throw new RuntimeException("Invalid home path '" + ourHomePath + "'");
      }
    }
    else {
      if (ourHomePath == null) {
        throw new RuntimeException("Could not find installation home path.");
      }
    }

    if (Platform.current().os().isWindows()) {
      try {
        ourHomePath = new File(ourHomePath).getCanonicalPath();
      }
      catch (IOException ignored) {
      }
    }

    return ourHomePath;
  }

  /**
   * @return external platform directory for mac, or platform directory inside application for other oses
   */
  @Override
  @Nonnull
  public File getExternalPlatformDirectory() {
    File defaultPath = new File(getAppHomeDirectory(), PLATFORM_FOLDER);

    // force platform inside distribution directory
    if (Boolean.getBoolean(CONSULO_NO_EXTERNAL_PLATFORM) || ApplicationProperties.isInSandbox()) {
      return defaultPath;
    }
    return DefaultPaths.getInstance().getExternalPlatformDirectory(defaultPath);
  }

  /**
   * @return app home, equal IDE installation path
   */
  @Override
  @Nonnull
  public File getAppHomeDirectory() {
    String appHomePath = System.getProperty(CONSULO_APP_HOME_PATH);
    if (appHomePath != null) {
      return new File(getAbsolutePath(trimPathQuotes(appHomePath)));
    }

    File homeDir = new File(getHomePath());

    // 'platform' directory
    File parentFile = homeDir.getParentFile();
    if (!parentFile.getName().equals(PLATFORM_FOLDER)) {
      throw new IllegalArgumentException("Parent dir is not platform: " + parentFile.getName());
    }

    return parentFile.getParentFile();
  }

  @Override
  @Nonnull
  public String getBinPath() {
    return getHomePath() + File.separator + BIN_FOLDER;
  }

  @Override
  @Nonnull
  public String getLibPath() {
    return getHomePath() + File.separator + LIB_FOLDER;
  }

  // config paths
  @Override
  @Nonnull
  public String getConfigPath() {
    if (ourConfigPath != null) return ourConfigPath;

    String propertyValue = selectPropertyValue(PROPERTY_CONFIG_PATH);
    if (propertyValue != null) {
      ourConfigPath = getAbsolutePath(trimPathQuotes(propertyValue));
    }
    else {
      ourConfigPath = DefaultPaths.getInstance().getRoamingSettingsDir();
    }

    if (ApplicationProperties.isInSandbox()) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Config Path: " + ourConfigPath);
    }

    return ourConfigPath;
  }

  @Override
  @Nonnull
  public String getScratchPath() {
    if (ourScratchPath != null) return ourScratchPath;

    String propertyValue = selectPropertyValue(PROPERTY_SCRATCH_PATH);
    if (propertyValue != null) {
      ourScratchPath = getAbsolutePath(trimPathQuotes(propertyValue));
    }
    else {
      ourScratchPath = getConfigPath() + "/" + ContainerPathManager.SCRATCHES_FOLDER;
    }

    return ourScratchPath;
  }

  public void ensureConfigFolderExists() {
    checkAndCreate(getConfigPath(), true);
  }

  @Override
  @Nonnull
  public String getOptionsPath() {
    return getConfigPath() + File.separator + OPTIONS_FOLDER;
  }

  @Override
  @Nonnull
  public File getOptionsFile(@Nonnull String fileName) {
    return new File(getOptionsPath(), fileName + ".xml");
  }

  @Override
  @Nonnull
  public String getInstallPluginsPath() {
    if (ourInstallPluginsPath != null) {
      return ourInstallPluginsPath;
    }

    String property = System.getProperty(CONSULO_INSTALL_PLUGINS_PATH);
    if (property != null) {
      ourInstallPluginsPath = getAbsolutePath(trimPathQuotes(property));
    }
    else {
      String[] pluginsPaths = getPluginsPaths();
      if (pluginsPaths.length != 1) {
        throw new IllegalArgumentException("Plugins paths size is not equal one. Paths: " + Arrays.asList(pluginsPaths));
      }

      ourInstallPluginsPath = pluginsPaths[0];
    }

    if (ApplicationProperties.isInSandbox()) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Install Plugins Path: " + ourInstallPluginsPath);
    }
    return ourInstallPluginsPath;
  }

  @Override
  @Nonnull
  public String[] getPluginsPaths() {
    if (ourPluginsPaths != null) return ourPluginsPaths;

    String propertyValue = selectPropertyValue(CONSULO_PLUGINS_PATHS);
    if (propertyValue != null) {
      if (propertyValue.contains(File.pathSeparator)) {
        String[] splittedPaths = propertyValue.split(File.pathSeparator);
        for (int i = 0; i < splittedPaths.length; i++) {
          String splitValue = splittedPaths[i];

          splittedPaths[i] = getAbsolutePath(trimPathQuotes(splitValue));
        }

        ourPluginsPaths = splittedPaths;
      }
      else {
        ourPluginsPaths = new String[]{getAbsolutePath(trimPathQuotes(propertyValue))};
      }
    }
    else if (selectPropertyValue(PROPERTY_CONFIG_PATH) != null) {
      // if config path overridden, use another logic for plugins
      ourPluginsPaths = new String[]{getConfigPath() + File.separatorChar + "plugins"};
    }
    else {
      ourPluginsPaths = new String[]{DefaultPaths.getInstance().getRoamingPluginsDir()};
    }

    if (ApplicationProperties.isInSandbox()) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Plugins Paths: " + Arrays.asList(ourPluginsPaths));
    }
    return ourPluginsPaths;
  }

  // runtime paths

  @Override
  @Nonnull
  public String getSystemPath() {
    if (ourSystemPath != null) return ourSystemPath;

    String propertyValue = selectPropertyValue(PROPERTY_SYSTEM_PATH);

    if (propertyValue != null) {
      ourSystemPath = getAbsolutePath(trimPathQuotes(propertyValue));
    }
    else {
      ourSystemPath = DefaultPaths.getInstance().getLocalSettingsDir();
    }

    if (ApplicationProperties.isInSandbox()) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("System Path: " + ourSystemPath);
    }

    checkAndCreate(ourSystemPath, true);
    return ourSystemPath;
  }

  @Override
  public Path getSystemDir() {
    Path path = Paths.get(ContainerPathManager.get().getSystemPath());
    try {
      return path.toRealPath();
    }
    catch (IOException e) {
      Logger.getInstance(DesktopContainerPathManager.class).error(e);
    }
    return path;
  }

  @Nonnull
  @Override
  public File getDocumentsDir() {
    return DefaultPaths.getInstance().getDocumentsDir();
  }

  @Override
  @Nonnull
  public String getTempPath() {
    return getSystemPath() + File.separator + "tmp";
  }

  @Override
  @Nonnull
  public File getIndexRoot() {
    String indexRoot = System.getProperty("index_root_path", getSystemPath() + "/index");
    checkAndCreate(indexRoot, true);
    return new File(indexRoot);
  }

  @Override
  @Nonnull
  public File getLogPath() {
    if (ourLogPath != null) return ourLogPath;

    String propertyValue = selectPropertyValue(PROPERTY_LOG_PATH);
    if (propertyValue != null) {
      ourLogPath = getAbsoluteFile(trimPathQuotes(propertyValue));
    }
    else if (selectPropertyValue(PROPERTY_SYSTEM_PATH) != null) {
      // if system path overridden, use another logic for logs
      ourLogPath = new File(getSystemPath(), "logs");
    }
    else {
      ourLogPath = DefaultPaths.getInstance().getLocalLogsDir();
    }

    return ourLogPath;
  }

  @Override
  @Nonnull
  public String getPluginTempPath() {
    return getSystemPath() + File.separator + PLUGINS_FOLDER;
  }

  public void loadProperties() {
    List<String> paths = new ArrayList<>();
    String propertyValue = selectPropertyValue(PROPERTIES_FILE);
    if (propertyValue != null) {
      paths.add(propertyValue);
    }
    paths.add(new File(getAppHomeDirectory(), "consulo.properties").getPath());
    paths.add(getUserHome() + "/consulo.properties");

    File propFile = FileUtil.findFirstThatExist(ArrayUtil.toStringArray(paths));

    if (propFile == null) {
      return;
    }

    try (InputStream fis = new BufferedInputStream(new FileInputStream(propFile))) {
      final PropertyResourceBundle bundle = new PropertyResourceBundle(fis);
      final Enumeration keys = bundle.getKeys();
      String home = (String)bundle.handleGetObject("idea.home");
      if (home != null && ourHomePath == null) {
        ourHomePath = getAbsolutePath(substituteVars(home));
      }
      final Properties sysProperties = System.getProperties();
      while (keys.hasMoreElements()) {
        String key = (String)keys.nextElement();
        if (sysProperties.getProperty(key, null) == null) { // load the property from the property file only if it is not defined yet
          final String value = substituteVars(bundle.getString(key));
          sysProperties.setProperty(key, value);
        }
      }
    }
    catch (IOException e) {
      //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
      System.err.println("Problem reading from property file: " + propFile.getPath());
    }
  }

  @Contract("null -> null")
  public String substituteVars(String s) {
    final String ideaHomePath = getHomePath();
    return substituteVars(s, ideaHomePath);
  }

  @Contract("null, _ -> null")
  public static String substituteVars(String s, String ideaHomePath) {
    if (s == null) return null;
    if (s.startsWith("..")) {
      s = ideaHomePath + File.separatorChar + BIN_FOLDER + File.separatorChar + s;
    }
    s = StringUtil.replace(s, "${idea.home}", ideaHomePath);
    final Properties props = System.getProperties();
    final Set keys = props.keySet();
    for (final Object key1 : keys) {
      String key = (String)key1;
      String value = props.getProperty(key);
      s = StringUtil.replace(s, "${" + key + "}", value);
    }
    return s;
  }


  // helpers

  private static String getAbsolutePath(String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = getUserHome() + path.substring(1);
    }
    return new File(path).getAbsolutePath();
  }

  @Nonnull
  private static File getAbsoluteFile(String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = getUserHome() + path.substring(1);
    }
    return new File(path);
  }

  private static String trimPathQuotes(String path) {
    if (!(path != null && !(path.length() < 3))) {
      return path;
    }
    if (StringUtil.startsWithChar(path, '\"') && StringUtil.endsWithChar(path, '\"')) {
      return path.substring(1, path.length() - 1);
    }
    return path;
  }

  private static boolean checkAndCreate(String path, boolean createIfNotExists) {
    if (createIfNotExists) {
      File file = new File(path);
      if (!file.exists()) {
        return file.mkdirs();
      }
    }
    return false;
  }

  private static String selectPropertyValue(String... properties) {
    for (String property : properties) {
      String propertyValue = System.getProperty(property);
      if (!StringUtil.isEmpty(propertyValue)) {
        return propertyValue;
      }
    }
    return null;
  }
}
