/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.application;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.io.URLUtil;
import com.sun.jna.TypeMapper;
import com.sun.jna.platform.FileUtils;
import consulo.application.DefaultPaths;
import consulo.util.SandboxUtil;
import gnu.trove.THashSet;
import org.apache.log4j.Appender;
import org.apache.oro.text.regex.PatternMatcher;
import org.jdom.Document;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.io.*;
import java.net.URL;
import java.util.*;

import static com.intellij.util.SystemProperties.getUserHome;

public class PathManager {
  public static final String PROPERTIES_FILE = "idea.properties.file";
  public static final String PROPERTY_SYSTEM_PATH = "idea.system.path";
  public static final String PROPERTY_SCRATCH_PATH = "idea.scratch.path";
  public static final String PROPERTY_CONFIG_PATH = "idea.config.path";
  public static final String PROPERTY_PLUGINS_PATH = "idea.plugins.path";
  public static final String PROPERTY_HOME_PATH = "idea.home.path";
  public static final String PROPERTY_LOG_PATH = "idea.log.path";
  public static final String MAC_APP_HOME_PATH = "consulo.mac.app.home.path";

  private static final String PLATFORM_FOLDER = "platform";
  private static final String LIB_FOLDER = "lib";
  private static final String PLUGINS_FOLDER = "plugins";
  private static final String BIN_FOLDER = "bin";
  private static final String OPTIONS_FOLDER = "options";

  private static String ourHomePath;
  private static String ourSystemPath;
  private static String ourConfigPath;
  private static String ourScratchPath;
  private static String ourPluginsPath;
  private static String ourLogPath;

  // IDE installation paths

  @NotNull
  public static String getHomePath() {
    if (ourHomePath != null) return ourHomePath;

    String fromProperty = System.getProperty(PROPERTY_HOME_PATH);
    if (fromProperty != null) {
      ourHomePath = getAbsolutePath(fromProperty);
      if (!new File(ourHomePath).isDirectory()) {
        throw new RuntimeException("Invalid home path '" + ourHomePath + "'");
      }
    }
    else {
      if (ourHomePath == null) {
        throw new RuntimeException("Could not find installation home path.");
      }
    }

    if (SystemInfo.isWindows) {
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
  @NotNull
  public static File getExternalPlatformDirectory() {
    if (SystemInfo.isMac && !SandboxUtil.isInsideSandbox()) {
      return new File(SystemProperties.getUserHome(), "Library/Application Support/Consulo Platform");
    }
    return new File(getDistributionDirectory(), PLATFORM_FOLDER);
  }

  @NotNull
  public static File getDistributionDirectory() {
    String macAppHomePath = System.getProperty(MAC_APP_HOME_PATH);
    if (macAppHomePath != null) {
      return new File(macAppHomePath, "Contents");
    }

    File homeDir = new File(getHomePath());

    // 'platform' directory
    File parentFile = homeDir.getParentFile();
    if (!parentFile.getName().equals(PLATFORM_FOLDER)) {
      throw new IllegalArgumentException("Parent dir is not platform: " + parentFile.getName());
    }

    return parentFile.getParentFile();
  }

  @NotNull
  public static String getBinPath() {
    return getHomePath() + File.separator + BIN_FOLDER;
  }

  @NotNull
  public static String getLibPath() {
    return getHomePath() + File.separator + LIB_FOLDER;
  }

  @SuppressWarnings("MethodNamesDifferingOnlyByCase")
  @NotNull
  public static String getPreInstalledPluginsPath() {
    return getHomePath() + File.separatorChar + PLUGINS_FOLDER;
  }

  // config paths
  @NotNull
  public static String getConfigPath() {
    if (ourConfigPath != null) return ourConfigPath;

    if (System.getProperty(PROPERTY_CONFIG_PATH) != null) {
      ourConfigPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_CONFIG_PATH)));
    }
    else {
      ourConfigPath = DefaultPaths.getInstance().getRoamingSettingsDir();
    }

    return ourConfigPath;
  }

  @NotNull
  public static String getScratchPath() {
    if (ourScratchPath != null) return ourScratchPath;

    if (System.getProperty(PROPERTY_SCRATCH_PATH) != null) {
      ourScratchPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_SCRATCH_PATH)));
    }
    else {
      ourScratchPath = getConfigPath();
    }

    return ourScratchPath;
  }

  public static void ensureConfigFolderExists() {
    checkAndCreate(getConfigPath(), true);
  }

  @NotNull
  public static String getOptionsPath() {
    return getConfigPath() + File.separator + OPTIONS_FOLDER;
  }

  @NotNull
  public static File getOptionsFile(@NotNull String fileName) {
    return new File(getOptionsPath(), fileName + ".xml");
  }

  @NotNull
  public static String getPluginsPath() {
    if (ourPluginsPath != null) return ourPluginsPath;

    if (System.getProperty(PROPERTY_PLUGINS_PATH) != null) {
      ourPluginsPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_PLUGINS_PATH)));
    }
    else if (System.getProperty(PROPERTY_CONFIG_PATH) != null) {
      // if config path overridden, use another logic for plugins
      ourPluginsPath = getConfigPath() + File.separatorChar + "plugins";
    }
    else {
      ourPluginsPath = DefaultPaths.getInstance().getRoamingPluginsDir();
    }

    return ourPluginsPath;
  }

  // runtime paths

  @NotNull
  public static String getSystemPath() {
    if (ourSystemPath != null) return ourSystemPath;

    if (System.getProperty(PROPERTY_SYSTEM_PATH) != null) {
      ourSystemPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_SYSTEM_PATH)));
    }
    else {
      ourSystemPath = DefaultPaths.getInstance().getLocalSettingsDir();
    }

    checkAndCreate(ourSystemPath, true);
    return ourSystemPath;
  }

  @NotNull
  public static String getTempPath() {
    return getSystemPath() + File.separator + "tmp";
  }

  @NotNull
  public static File getIndexRoot() {
    String indexRoot = System.getProperty("index_root_path", getSystemPath() + "/index");
    checkAndCreate(indexRoot, true);
    return new File(indexRoot);
  }

  @NotNull
  public static String getLogPath() {
    if (ourLogPath != null) return ourLogPath;

    if (System.getProperty(PROPERTY_LOG_PATH) != null) {
      ourLogPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_LOG_PATH)));
    }
    else if (System.getProperty(PROPERTY_SYSTEM_PATH) != null) {
      // if system path overridden, use another logic for logs
      ourLogPath = getSystemPath() + File.separatorChar + "logs";
    }
    else {
      ourLogPath = DefaultPaths.getInstance().getLocalLogsDir();
    }

    return ourLogPath;
  }

  @NotNull
  public static String getPluginTempPath() {
    return getSystemPath() + File.separator + PLUGINS_FOLDER;
  }

  // misc stuff

  /**
   * Attempts to detect classpath entry which contains given resource.
   */
  @Nullable
  public static String getResourceRoot(@NotNull Class context, String path) {
    URL url = context.getResource(path);
    if (url == null) {
      url = ClassLoader.getSystemResource(path.substring(1));
    }
    return url != null ? extractRoot(url, path) : null;
  }

  /**
   * Attempts to extract classpath entry part from passed URL.
   */
  @Nullable
  @NonNls
  private static String extractRoot(URL resourceURL, String resourcePath) {
    if (!(StringUtil.startsWithChar(resourcePath, '/') || StringUtil.startsWithChar(resourcePath, '\\'))) {
      //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
      System.err.println("precondition failed: " + resourcePath);
      return null;
    }

    String resultPath = null;
    String protocol = resourceURL.getProtocol();
    if (URLUtil.FILE_PROTOCOL.equals(protocol)) {
      String path = resourceURL.getFile();
      String testPath = path.replace('\\', '/');
      String testResourcePath = resourcePath.replace('\\', '/');
      if (StringUtil.endsWithIgnoreCase(testPath, testResourcePath)) {
        resultPath = path.substring(0, path.length() - resourcePath.length());
      }
    }
    else if (URLUtil.JAR_PROTOCOL.equals(protocol)) {
      Pair<String, String> paths = URLUtil.splitJarUrl(resourceURL.getFile());
      if (paths != null) {
        resultPath = paths.first;
      }
    }

    if (resultPath == null) {
      //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
      System.err.println("cannot extract: " + resourcePath + " from " + resourceURL);
      return null;
    }

    resultPath = StringUtil.trimEnd(resultPath, File.separator);
    resultPath = URLUtil.unescapePercentSequences(resultPath);

    return resultPath;
  }

  public static void loadProperties() {
    File propFile =
            FileUtil.findFirstThatExist(System.getProperty(PROPERTIES_FILE), getUserHome() + "/idea.properties", getHomePath() + "/bin/idea.properties");

    if (propFile != null) {
      try {
        InputStream fis = new BufferedInputStream(new FileInputStream(propFile));
        try {
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
        finally {
          fis.close();
        }
      }
      catch (IOException e) {
        //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
        System.err.println("Problem reading from property file: " + propFile.getPath());
      }
    }
  }

  @Contract("null -> null")
  public static String substituteVars(String s) {
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

  @NotNull
  public static File findFileInLibDirectory(@NotNull String relativePath) {
    return new File(getLibPath() + File.separator + relativePath);
  }

  @Nullable
  public static String getJarPathForClass(@NotNull Class aClass) {
    String resourceRoot = getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
    return resourceRoot != null ? new File(resourceRoot).getAbsolutePath() : null;
  }

  @NotNull
  public static Collection<String> getUtilClassPath() {
    final Class<?>[] classes = {PathManager.class,            // module 'util'
            NotNull.class,                // module 'annotations'
            SystemInfoRt.class,           // module 'util-rt'
            Document.class,               // jDOM
            Appender.class,               // log4j
            THashSet.class,               // trove4j
            PicoContainer.class,          // PicoContainer
            TypeMapper.class,             // JNA
            FileUtils.class,              // JNA (jna-platform)
            PatternMatcher.class          // OROMatcher
    };

    final Set<String> classPath = new HashSet<String>();
    for (Class<?> aClass : classes) {
      final String path = getJarPathForClass(aClass);
      if (path != null) {
        classPath.add(path);
      }
    }

    final String resourceRoot = getResourceRoot(PathManager.class, "/messages/CommonBundle.properties");  // platform-resources-en
    if (resourceRoot != null) {
      classPath.add(new File(resourceRoot).getAbsolutePath());
    }

    return Collections.unmodifiableCollection(classPath);
  }

  // helpers

  private static String getAbsolutePath(String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = getUserHome() + path.substring(1);
    }
    return new File(path).getAbsolutePath();
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
}
