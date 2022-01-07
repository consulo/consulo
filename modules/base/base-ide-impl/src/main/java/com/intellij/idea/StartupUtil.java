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
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.win32.IdeaWin32;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.PairConsumer;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.ExitCodes;
import consulo.container.StartupError;
import consulo.container.boot.ContainerPathManager;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import consulo.logging.internal.LoggerFactoryInitializer;
import consulo.start.CommandLineArgs;
import consulo.start.ImportantFolderLocker;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
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
    if (ourFolderLocker == null) {
      throw new IllegalArgumentException("Called #getLocker() before app start");
    }
    return ourFolderLocker;
  }

  public static void prepareAndStart(String[] args, StatCollector stat, BiFunction<String, String, ImportantFolderLocker> lockFactory, PairConsumer<Boolean, CommandLineArgs> appStarter) {
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

    appStarter.consume(newConfigFolder, commandLineArgs);
  }

  public static void initializeLogger() {
    List<LoggerFactory> factories = ContainerUtil.newArrayList(ServiceLoader.load(LoggerFactory.class, StartupUtil.class.getClassLoader()));
    ContainerUtil.weightSort(factories, LoggerFactory::getPriority);
    LoggerFactory factory = factories.get(0);
    LoggerFactoryInitializer.setFactory(factory);
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
      showMessage("Cannot Lock System Folders", e);
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
      String message = "Only one instance of " + ApplicationNamesInfo.getInstance().getFullProductName() + " can be run at a time.";
      showMessage("Too Many Instances", message, true, false);
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

    JnaLoader.load(log);

    if (SystemInfo.isWin2kOrNewer) {
      IdeaWin32.isAvailable();  // logging is done there
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

  public static void showMessage(String title, Throwable t) {
    StringWriter message = new StringWriter();
    boolean graphError = false;
    AWTError awtError = findGraphicsError(t);
    if (awtError != null) {
      message.append("Failed to initialize graphics environment\n\n");
      graphError = true;
      t = awtError;
    }
    else {
      message.append("Internal error. Please post to ");
      message.append("https://discuss.consulo.io");
      message.append("\n\n");
    }

    t.printStackTrace(new PrintWriter(message));
    showMessage(title, message.toString(), true, graphError);
  }

  private static AWTError findGraphicsError(Throwable t) {
    while (t != null) {
      if (t instanceof AWTError) {
        return (AWTError)t;
      }
      t = t.getCause();
    }
    return null;
  }

  // Copy&Paste from desktop Main
  public static void showMessage(String title, String message, boolean error, boolean graphError) {
    if (StartupError.hasStartupError) {
      return;
    }

    PrintStream stream = error ? System.err : System.out;
    stream.println("\n" + title + ": " + message);

    boolean headless = GraphicsEnvironment.isHeadless() || graphError;
    if (!headless) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Throwable ignore) {
      }

      StartupError.hasStartupError = true;

      try {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setText(message.replaceAll("\t", "    "));
        textPane.setBackground(UIManager.getColor("Panel.background"));
        textPane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        int maxHeight = Toolkit.getDefaultToolkit().getScreenSize().height / 2;
        int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width / 2;
        Dimension component = scrollPane.getPreferredSize();
        if (component.height > maxHeight || component.width > maxWidth) {
          scrollPane.setPreferredSize(new Dimension(Math.min(maxWidth, component.width), Math.min(maxHeight, component.height)));
        }

        int type = error ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), scrollPane, title, type);
      }
      catch (Throwable t) {
        stream.println("\nAlso, an UI exception occurred on attempt to show above message:");
        t.printStackTrace(stream);
      }
    }
  }
}
