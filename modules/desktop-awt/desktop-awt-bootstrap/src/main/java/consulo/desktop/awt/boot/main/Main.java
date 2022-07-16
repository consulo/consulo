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
package consulo.desktop.awt.boot.main;

import consulo.container.ExitCodes;
import consulo.container.boot.ContainerStartup;
import consulo.container.impl.SystemContainerLogger;
import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.container.internal.ShowError;
import consulo.container.util.StatCollector;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(final String[] args) {
    ShowError.INSTANCE = new ShowError() {
      @Override
      public void showErrorDialogImpl(String title, String message, Throwable t) {
        Main.showErrorDialogImpl(title, message);
      }
    };

    if (GraphicsEnvironment.isHeadless()) {
      throw new HeadlessException("Unable to detect graphics environment");
    }

    String javaRuntimeError = ExitCodes.validateJavaRuntime();
    if (javaRuntimeError != null) {
      ShowError.showErrorDialog("Unsupported Java Version", javaRuntimeError, null);
      System.exit(ExitCodes.UNSUPPORTED_JAVA_VERSION);
    }

    try {
      initAndCallStartup(args);
    }
    catch (Throwable t) {
      ShowError.showErrorDialog("Start Failed", t.getMessage(), t);
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

  private static void showErrorDialogImpl(String title, String message) {
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

      int type = JOptionPane.ERROR_MESSAGE;
      JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), scrollPane, title, type);
    }
    catch (Throwable t) {
      System.err.println("\nAlso, an UI exception occurred on attempt to show above message:");
      t.printStackTrace(System.err);
    }
  }
}
