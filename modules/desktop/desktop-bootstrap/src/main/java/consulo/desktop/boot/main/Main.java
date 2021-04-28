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
package consulo.desktop.boot.main;

import consulo.container.ExitCodes;
import consulo.container.StartupError;
import consulo.container.boot.ContainerStartup;
import consulo.container.impl.SystemContainerLogger;
import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.container.util.StatCollector;
import consulo.util.nodep.SystemInfoRt;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class Main {
  private static final String AWT_HEADLESS = "java.awt.headless";

  private static boolean isHeadless;
  private static boolean hasGraphics = true;

  private Main() {
  }

  public static void main(final String[] args) {
    setFlags(args);

    if (isHeadless()) {
      System.setProperty(AWT_HEADLESS, Boolean.TRUE.toString());
    }
    else {
      if (GraphicsEnvironment.isHeadless()) {
        throw new HeadlessException("Unable to detect graphics environment");
      }
    }

    if (!SystemInfoRt.isJavaVersionAtLeast(11)) {
      showMessage("Unsupported Java Version", "Cannot start under Java " + SystemInfoRt.JAVA_RUNTIME_VERSION + ": Java 11 or later is required.", true);
      System.exit(ExitCodes.UNSUPPORTED_JAVA_VERSION);
    }

    try {
      initAndCallStartup(args);
    }
    catch (Throwable t) {
      showMessage("Start Failed", t);
      System.exit(ExitCodes.STARTUP_EXCEPTION);
    }
  }

  private static void initAndCallStartup(String[] args) throws Exception {
    StatCollector stat = new StatCollector();

    File modulesDirectory = BootstrapClassLoaderUtil.getModulesDirectory();


    Map<String, Object> map = new HashMap<String, Object>();
    map.put(ContainerStartup.ARGS, args);
    map.put(ContainerStartup.STAT_COLLECTOR, stat);

    ContainerStartup containerStartup = BootstrapClassLoaderUtil.buildContainerStartup(map, modulesDirectory, SystemContainerLogger.INSTANCE, DesktopAwtJava9Processor.INSTANCE);

    containerStartup.run(map);
  }

  public static boolean isHeadless() {
    return isHeadless;
  }

  public static void setFlags(String[] args) {
    isHeadless = isHeadless(args);
  }

  private static boolean isHeadless(String[] args) {
    return Boolean.getBoolean(AWT_HEADLESS);
  }

  public static void showMessage(String title, Throwable t) {
    StringWriter message = new StringWriter();

    AWTError awtError = findGraphicsError(t);
    if (awtError != null) {
      message.append("Failed to initialize graphics environment\n\n");
      hasGraphics = false;
      t = awtError;
    }
    else {
      message.append("Internal error. Please post to ");
      message.append("https://discuss.consulo.io");
      message.append("\n\n");
    }

    t.printStackTrace(new PrintWriter(message));
    showMessage(title, message.toString(), true);
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

  @SuppressWarnings({"UseJBColor", "UndesirableClassUsage", "UseOfSystemOutOrSystemErr"})
  public static void showMessage(String title, String message, boolean error) {
    StartupError.hasStartupError = true;

    PrintStream stream = error ? System.err : System.out;
    stream.println("\n" + title + ": " + message);

    boolean headless = !hasGraphics || GraphicsEnvironment.isHeadless();
    if (!headless) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Throwable ignore) {
      }

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
