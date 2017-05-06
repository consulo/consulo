/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.idea;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.io.win32.IdeaWin32;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.Consumer;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.lang.UrlClassLoader;
import com.sun.jna.Native;
import consulo.start.CommandLineArgs;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.BuiltInServer;

import javax.swing.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * @author yole
 */
public class StartupUtil {
  private static SocketLock ourSocketLock;

  private StartupUtil() {
  }

  public synchronized static void addExternalInstanceListener(@NotNull Consumer<CommandLineArgs> consumer) {
    ourSocketLock.setExternalInstanceListener(consumer);
  }

  @Nullable
  public synchronized static BuiltInServer getServer() {
    return ourSocketLock == null ? null : ourSocketLock.getServer();
  }

  public static void prepareAndStart(String[] args, Consumer<CommandLineArgs> appStarter) {
    CommandLineArgs commandLineArgs = CommandLineArgs.parse(args);

    if (commandLineArgs.isShowHelp()) {
      CommandLineArgs.printUsage();
      System.exit(Main.USAGE_INFO);
    }

    if (commandLineArgs.isShowVersion()) {
      ApplicationInfoEx infoEx = ApplicationInfoImpl.getShadowInstance();
      System.out.println(infoEx.getFullApplicationName());
      System.exit(Main.VERSION_INFO);
    }

    if (!Main.isHeadless()) {
      AppUIUtil.updateFrameClass();
    }

    // avoiding "log4j:WARN No appenders could be found"
    System.setProperty("log4j.defaultInitOverride", "true");
    try {
      org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
      if (!root.getAllAppenders().hasMoreElements()) {
        root.setLevel(Level.WARN);
        root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.DEFAULT_CONVERSION_PATTERN)));
      }
    }
    catch (Throwable e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

    ActivationResult result = lockSystemFolders(args);
    if (result == ActivationResult.ACTIVATED) {
      System.exit(0);
    }
    else if (result != ActivationResult.STARTED) {
      System.exit(Main.INSTANCE_CHECK_FAILED);
    }

    Logger.setFactory(LoggerFactory.class);
    Logger log = Logger.getInstance(Main.class);
    startLogging(log);
    loadSystemLibraries(log);
    fixProcessEnvironment(log);

    if (!Main.isHeadless()) {
      AppUIUtil.updateWindowIcon(JOptionPane.getRootFrame());
      AppUIUtil.registerBundledFonts();
    }

    appStarter.consume(commandLineArgs);
  }

  private synchronized static boolean checkSystemFolders() {
    String configPath = PathManager.getConfigPath();
    PathManager.ensureConfigFolderExists();
    if (!new File(configPath).isDirectory()) {
      String message = "Config path '" +
                       configPath +
                       "' is invalid.\n" +
                       "If you have modified the '" +
                       PathManager.PROPERTY_CONFIG_PATH +
                       "' property please make sure it is correct,\n" +
                       "otherwise please re-install the IDE.";
      Main.showMessage("Invalid Config Path", message, true);
      return false;
    }

    String systemPath = PathManager.getSystemPath();
    if (!new File(systemPath).isDirectory()) {
      String message = "System path '" +
                       systemPath +
                       "' is invalid.\n" +
                       "If you have modified the '" +
                       PathManager.PROPERTY_SYSTEM_PATH +
                       "' property please make sure it is correct,\n" +
                       "otherwise please re-install the IDE.";
      Main.showMessage("Invalid System Path", message, true);
      return false;
    }

    File ideTempDir = new File(PathManager.getTempPath());
    String tempInaccessible = null;

    if (!ideTempDir.isDirectory() && !ideTempDir.mkdirs()) {
      tempInaccessible = "unable to create the directory";
    }
    else {
      try {
        File ideTempFile = new File(ideTempDir, "idea_tmp_check.sh");
        FileUtil.writeToFile(ideTempFile, "#!/bin/sh\nexit 0");

        if (SystemInfo.isWindows || SystemInfo.isMac) {
          tempInaccessible = null;
        }
        else if (!ideTempFile.setExecutable(true, true)) {
          tempInaccessible = "cannot set executable permission";
        }
        else if (new ProcessBuilder(ideTempFile.getAbsolutePath()).start().waitFor() != 0) {
          tempInaccessible = "cannot execute test script";
        }

        if (!FileUtilRt.delete(ideTempFile)) {
          ideTempFile.deleteOnExit();
        }
      }
      catch (Exception e) {
        tempInaccessible = e.getClass().getSimpleName() + ": " + e.getMessage();
      }
    }

    if (tempInaccessible != null) {
      String message = "Temp directory '" +
                       ideTempDir +
                       "' is inaccessible.\n" +
                       "If you have modified the '" +
                       PathManager.PROPERTY_SYSTEM_PATH +
                       "' property please make sure it is correct,\n" +
                       "otherwise please re-install the IDE.\n\nDetails: " +
                       tempInaccessible;
      Main.showMessage("Invalid System Path", message, true);
      return false;
    }

    return true;
  }

  private enum ActivationResult {
    STARTED,
    ACTIVATED,
    FAILED
  }

  @NotNull
  private synchronized static ActivationResult lockSystemFolders(String[] args) {
    if (ourSocketLock != null) {
      throw new AssertionError();
    }

    ourSocketLock = new SocketLock(PathManager.getConfigPath(), PathManager.getSystemPath());

    SocketLock.ActivateStatus status;
    try {
      status = ourSocketLock.lock(args);
    }
    catch (Exception e) {
      Main.showMessage("Cannot Lock System Folders", e);
      return ActivationResult.FAILED;
    }

    if (status == SocketLock.ActivateStatus.NO_INSTANCE) {
      ShutDownTracker.getInstance().registerShutdownTask(() -> {
        //noinspection SynchronizeOnThis
        synchronized (StartupUtil.class) {
          ourSocketLock.dispose();
          ourSocketLock = null;
        }
      });
      return ActivationResult.STARTED;
    }
    else if (status == SocketLock.ActivateStatus.ACTIVATED) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Already running");
      return ActivationResult.ACTIVATED;
    }
    else if (Main.isHeadless() || status == SocketLock.ActivateStatus.CANNOT_ACTIVATE) {
      String message = "Only one instance of " + ApplicationNamesInfo.getInstance().getFullProductName() + " can be run at a time.";
      Main.showMessage("Too Many Instances", message, true);
    }

    return ActivationResult.FAILED;
  }

  private static void fixProcessEnvironment(Logger log) {
    if (!Main.isCommandLine()) {
      System.setProperty("__idea.mac.env.lock", "unlocked");
    }
    boolean envReady = EnvironmentUtil.isEnvironmentReady();  // trigger environment loading
    if (!envReady) {
      log.info("initializing environment");
    }
  }

  private static final String JAVA_IO_TEMP_DIR = "java.io.tmpdir";

  private static void loadSystemLibraries(final Logger log) {
    // load JNA and Snappy in own temp directory - to avoid collisions and work around no-exec /tmp
    File ideTempDir = new File(PathManager.getTempPath());
    if (!(ideTempDir.mkdirs() || ideTempDir.exists())) {
      throw new RuntimeException("Unable to create temp directory '" + ideTempDir + "'");
    }

    String javaTempDir = System.getProperty(JAVA_IO_TEMP_DIR);
    try {
      System.setProperty(JAVA_IO_TEMP_DIR, ideTempDir.getPath());
      if (System.getProperty("jna.nosys") == null && System.getProperty("jna.nounpack") == null) {
        // force using bundled JNA dispatcher (if not explicitly stated)
        System.setProperty("jna.nosys", "true");
        System.setProperty("jna.nounpack", "false");
      }
      try {
        final long t = System.currentTimeMillis();
        log.info("JNA library loaded (" + (Native.POINTER_SIZE * 8) + "-bit) in " + (System.currentTimeMillis() - t) + " ms");
      }
      catch (Throwable t) {
        logError(log, "Unable to load JNA library", t);
      }
    }
    finally {
      System.setProperty(JAVA_IO_TEMP_DIR, javaTempDir);
    }

    if (SystemInfo.isWin2kOrNewer) {
      IdeaWin32.isAvailable();  // logging is done there
    }

    if (SystemInfo.isWin2kOrNewer && !Main.isHeadless()) {
      try {
        UrlClassLoader.loadPlatformLibrary("focusKiller");
        log.info("Using \"FocusKiller\" library to prevent focus stealing.");
      }
      catch (Throwable t) {
        log.info("\"FocusKiller\" library not found or there were problems loading it.", t);
      }
    }
  }

  private static void logError(Logger log, String message, Throwable t) {
    message = message + " (OS: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + ")";
    log.error(message, t);
  }

  private static void startLogging(final Logger log) {
    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown hook - logging") {
      @Override
      public void run() {
        log.info("------------------------------------------------------ IDE SHUTDOWN ------------------------------------------------------");
      }
    });
    log.info("------------------------------------------------------ IDE STARTED ------------------------------------------------------");

    ApplicationInfo appInfo = ApplicationInfoImpl.getShadowInstance();
    ApplicationNamesInfo namesInfo = ApplicationNamesInfo.getInstance();
    String buildDate = new SimpleDateFormat("dd MMM yyyy HH:ss", Locale.US).format(appInfo.getBuildDate().getTime());
    log.info("IDE: " + namesInfo.getFullProductName() + " (build #" + appInfo.getBuild() + ", " + buildDate + ")");
    log.info("OS: " + SystemInfoRt.OS_NAME + " (" + SystemInfoRt.OS_VERSION + ", " + SystemInfo.OS_ARCH + ")");
    log.info("JRE: " + System.getProperty("java.runtime.version", "-") + " (" + System.getProperty("java.vendor", "-") + ")");
    log.info("JVM: " + System.getProperty("java.vm.version", "-") + " (" + System.getProperty("java.vm.name", "-") + ")");

    List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (arguments != null) {
      log.info("JVM Args: " + StringUtil.join(arguments, " "));
    }
  }
}
