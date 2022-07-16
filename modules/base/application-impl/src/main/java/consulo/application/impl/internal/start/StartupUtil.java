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
package consulo.application.impl.internal.start;

import consulo.application.ApplicationProperties;
import consulo.application.impl.internal.ApplicationInfo;
import consulo.application.util.SystemInfo;
import consulo.container.ExitCodes;
import consulo.container.boot.ContainerPathManager;
import consulo.container.internal.ShowError;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import consulo.logging.internal.LoggerFactoryInitializer;
import consulo.process.local.EnvironmentUtil;
import consulo.util.jna.JnaLoader;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.impl.internal.windows.WindowsFileSystemHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class StartupUtil {
  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance(StartupUtil.class);
  }

  private static ImportantFolderLocker ourFolderLocker;

  private StartupUtil() {
  }

  public synchronized static void addExternalInstanceListener(@Nonnull Consumer<CommandLineArgs> consumer) {
    ourFolderLocker.setExternalInstanceListener(consumer);
  }

  @Nonnull
  public static synchronized ImportantFolderLocker getLocker() {
    if (ourFolderLocker == null) {
      throw new IllegalArgumentException("Called #getLocker() before app start");
    }
    return ourFolderLocker;
  }

  public static void prepareAndStart(String[] args, StatCollector stat, BiFunction<String, String, ImportantFolderLocker> lockFactory, BiConsumer<Boolean, CommandLineArgs> appStarter) {
    boolean newConfigFolder;
    CommandLineArgs commandLineArgs = CommandLineArgs.parse(args);

    if (commandLineArgs.isShowHelp()) {
      CommandLineArgs.printUsage();
      System.exit(ExitCodes.USAGE_INFO);
    }

    if (commandLineArgs.isShowVersion()) {
      ApplicationInfo infoEx = ApplicationInfo.getInstance();
      System.out.println(infoEx.getFullApplicationName());
      System.exit(ExitCodes.VERSION_INFO);
    }

    newConfigFolder = !new File(ContainerPathManager.get().getConfigPath()).exists();

    ActivationResult result = lockSystemFolders(lockFactory, args);
    if (result == ActivationResult.ACTIVATED) {
      System.exit(0);
    }
    else if (result != ActivationResult.STARTED) {
      System.exit(ExitCodes.INSTANCE_CHECK_FAILED);
    }

    ForkJoinPool pool = ForkJoinPool.commonPool();

    Logger log = Logger.getInstance(StartupUtil.class);
    logStartupInfo(log);
    stat.markWith("load.system.libraries", () -> loadSystemLibraries(log));
    pool.execute(() -> EnvironmentUtil.loadEnvironment(stat.mark("load.console.env")));

    appStarter.accept(newConfigFolder, commandLineArgs);
  }

  public static void initializeLogger() {
    LoggerFactoryInitializer.initializeLogger(StartupUtil.class.getClassLoader());
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

    ourFolderLocker = lockFactory.apply(ContainerPathManager.get().getConfigPath(), ContainerPathManager.get().getSystemPath());

    ImportantFolderLocker.ActivateStatus status;
    try {
      status = ourFolderLocker.lock(args);
    }
    catch (Exception e) {
      ShowError.showErrorDialog("Cannot Lock System Folders", e.getMessage(), e);
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
    else if (status == ImportantFolderLocker.ActivateStatus.CANNOT_ACTIVATE) {
      String message = "Only one instance of IDE can be run at a time.";
      ShowError.showErrorDialog("Too Many Instances", message, null);
    }

    return ActivationResult.FAILED;
  }

  public static void loadSystemLibraries(final Logger log) {
    // load JNA in own temp directory - to avoid collisions and work around no-exec /tmp
    File ideTempDir = new File(ContainerPathManager.get().getTempPath());
    if (!(ideTempDir.mkdirs() || ideTempDir.exists())) {
      throw new RuntimeException("Unable to create temp directory '" + ideTempDir + "'");
    }
    if (System.getProperty("jna.tmpdir") == null) {
      System.setProperty("jna.tmpdir", ideTempDir.getPath());
    }
    if (System.getProperty("jna.nosys") == null) {
      System.setProperty("jna.nosys", "true");  // prefer bundled JNA dispatcher lib
    }

    JnaLoader.load(org.slf4j.LoggerFactory.getLogger(StartupUtil.class));

    if (SystemInfo.isWin2kOrNewer) {
      WindowsFileSystemHelper.isAvailable();  // logging is done there
    }

    if (SystemInfo.isWindows) {
      // WinP should not unpack .dll files into parent directory
      System.setProperty("winp.folder.preferred", ideTempDir.getPath());
    }
  }

  private static void logStartupInfo(final Logger log) {
    LoggerFactory factory = LoggerFactoryInitializer.getFactory();

    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown hook - logging") {
      @Override
      public void run() {
        log.info("------------------------------------------------------ IDE SHUTDOWN ------------------------------------------------------");
        factory.shutdown();
      }
    });
    log.info("------------------------------------------------------ IDE STARTED ------------------------------------------------------");
    log.info("Using logger factory: " + factory.getClass().getSimpleName());

    ApplicationInfo appInfo = ApplicationInfo.getInstance();
    String buildDate = new SimpleDateFormat("dd MMM yyyy HH:ss", Locale.US).format(appInfo.getBuildDate().getTime());
    log.info("IDE: Consulo (build #" + appInfo.getBuild() + ", " + buildDate + ")");
    log.info("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
    log.info("JRE: " + System.getProperty("java.runtime.version", "-") + " (" + System.getProperty("java.vendor", "-") + ")");
    log.info("JVM: " + System.getProperty("java.vm.version", "-") + " (" + System.getProperty("java.vm.name", "-") + ")");

    List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (arguments != null) {
      log.info("JVM Args: " + StringUtil.join(arguments, " "));
    }
  }

  public static void handleComponentError(@Nonnull Throwable t, @Nullable Class componentClass, @Nullable Object config) {
    if (t instanceof StartupAbortedException) {
      throw (StartupAbortedException)t;
    }

    PluginId pluginId = null;
    if (config != null) {
      pluginId = PluginManager.getPluginId(config.getClass());
    }
    if (pluginId == null || PluginIds.CONSULO_BASE.equals(pluginId)) {
      pluginId = componentClass == null ? null : PluginManager.getPluginId(componentClass);
    }

    if (pluginId != null && !PluginIds.isPlatformPlugin(pluginId)) {
      LoggerHolder.ourLogger.warn(t);

      if (!ApplicationProperties.isInSandbox()) {
        PluginManager.disablePlugin(pluginId.getIdString());
      }

      StringWriter message = new StringWriter();
      message.append("Plugin '").append(pluginId.getIdString()).append("' failed to initialize and will be disabled. ");
      message.append(" Please restart IDE").append('.');
      message.append("\n\n");
      t.printStackTrace(new PrintWriter(message));

      ShowError.showErrorDialog("Plugin Error", message.toString(), null);

      throw new StartupAbortedException(t).exitCode(ExitCodes.PLUGIN_ERROR).logError(false);
    }
    else {
      throw new StartupAbortedException("Fatal error initializing '" + (componentClass == null ? null : componentClass.getName()) + "'", t);
    }
  }
}
