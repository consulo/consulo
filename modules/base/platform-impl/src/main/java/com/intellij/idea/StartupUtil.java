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

import com.intellij.jna.JnaLoader;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.win32.IdeaWin32;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.PairConsumer;
import com.intellij.util.containers.ContainerUtil;
import consulo.start.CommandLineArgs;
import consulo.start.ImportantFolderLocker;
import consulo.util.logging.LoggerFactory;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class StartupUtil {
  private static ImportantFolderLocker ourFolderLocker;

  private StartupUtil() {
  }

  public synchronized static void addExternalInstanceListener(@Nonnull Consumer<CommandLineArgs> consumer) {
    ourFolderLocker.setExternalInstanceListener(consumer);
  }

  @Nonnull
  public static synchronized ImportantFolderLocker getLocker() {
    if(ourFolderLocker == null) {
      throw new IllegalArgumentException("Called #getLocker() before app start");
    }
    return ourFolderLocker;
  }

  public static void prepareAndStart(String[] args, BiFunction<String, String, ImportantFolderLocker> lockFactory, PairConsumer<Boolean, CommandLineArgs> appStarter) {
    boolean newConfigFolder = false;
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
      newConfigFolder = !new File(PathManager.getConfigPath()).exists();
    }

    List<LoggerFactory> factories = ContainerUtil.newArrayList(ServiceLoader.load(LoggerFactory.class, StartupUtil.class.getClassLoader()));
    ContainerUtil.weightSort(factories, LoggerFactory::getPriority);
    LoggerFactory factory = factories.get(0);
    Logger.setFactory(factory);

    ActivationResult result = lockSystemFolders(lockFactory, args);
    if (result == ActivationResult.ACTIVATED) {
      System.exit(0);
    }
    else if (result != ActivationResult.STARTED) {
      System.exit(Main.INSTANCE_CHECK_FAILED);
    }

    Logger log = Logger.getInstance(Main.class);
    startLogging(log, factory);
    loadSystemLibraries(log);
    fixProcessEnvironment(log);

    if (!Main.isHeadless()) {
      AppUIUtil.updateWindowIcon(JOptionPane.getRootFrame());
      AppUIUtil.registerBundledFonts();
    }

    appStarter.consume(newConfigFolder, commandLineArgs);
  }

  private enum ActivationResult {
    STARTED,
    ACTIVATED,
    FAILED
  }

  @Nonnull
  private synchronized static ActivationResult lockSystemFolders(BiFunction<String, String, ImportantFolderLocker> lockFactory, String[] args) {
    if (ourFolderLocker != null) {
      throw new AssertionError();
    }

    ourFolderLocker = lockFactory.apply(PathManager.getConfigPath(), PathManager.getSystemPath());

    ImportantFolderLocker.ActivateStatus status;
    try {
      status = ourFolderLocker.lock(args);
    }
    catch (Exception e) {
      Main.showMessage("Cannot Lock System Folders", e);
      return ActivationResult.FAILED;
    }

    if (status == ImportantFolderLocker.ActivateStatus.NO_INSTANCE) {
      ShutDownTracker.getInstance().registerShutdownTask(() -> {
        //noinspection SynchronizeOnThis
        synchronized (StartupUtil.class) {
          ourFolderLocker.dispose();
          ourFolderLocker = null;
        }
      });
      return ActivationResult.STARTED;
    }
    else if (status == ImportantFolderLocker.ActivateStatus.ACTIVATED) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Already running");
      return ActivationResult.ACTIVATED;
    }
    else if (Main.isHeadless() || status == ImportantFolderLocker.ActivateStatus.CANNOT_ACTIVATE) {
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

  public static void loadSystemLibraries(final Logger log) {
    // load JNA in own temp directory - to avoid collisions and work around no-exec /tmp
    File ideTempDir = new File(PathManager.getTempPath());
    if (!(ideTempDir.mkdirs() || ideTempDir.exists())) {
      throw new RuntimeException("Unable to create temp directory '" + ideTempDir + "'");
    }
    if (System.getProperty("jna.tmpdir") == null) {
      System.setProperty("jna.tmpdir", ideTempDir.getPath());
    }
    if (System.getProperty("jna.nosys") == null) {
      System.setProperty("jna.nosys", "true");  // prefer bundled JNA dispatcher lib
    }
    try {
      JnaLoader.load(log);
    }
    catch (Throwable t) {
      log.error("Unable to load JNA library (OS: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + ")", t);
    }

    if (SystemInfo.isWin2kOrNewer) {
      IdeaWin32.isAvailable();  // logging is done there
    }

    if (SystemInfo.isWindows) {
      // WinP should not unpack .dll files into parent directory
      System.setProperty("winp.folder.preferred", ideTempDir.getPath());
    }
  }

  private static void startLogging(final Logger log, LoggerFactory factory) {
    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown hook - logging") {
      @Override
      public void run() {
        log.info("------------------------------------------------------ IDE SHUTDOWN ------------------------------------------------------");
        factory.shutdown();
      }
    });
    log.info("------------------------------------------------------ IDE STARTED ------------------------------------------------------");
    log.info("Using logger factory: " + factory.getClass().getSimpleName());

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
