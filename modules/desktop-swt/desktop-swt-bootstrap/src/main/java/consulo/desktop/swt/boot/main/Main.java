/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.boot.main;

import consulo.container.ExitCodes;
import consulo.container.boot.ContainerStartup;
import consulo.container.impl.ShowErrorCaller;
import consulo.container.impl.SystemContainerLogger;
import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.container.internal.ShowError;
import consulo.container.util.StatCollector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class Main {
  public static void main(final String[] args) {
    ShowError.INSTANCE = new ShowError() {
      @Override
      public void showErrorDialogImpl(String title, String message, Throwable t) {
        MessageBox box = new MessageBox(new Shell(), SWT.OK);
        box.setText(title);
        box.setMessage(message);
        box.open();
      }
    };

    String javaRuntimeError = ExitCodes.validateJavaRuntime();
    if (javaRuntimeError != null) {
      ShowErrorCaller.showErrorDialog("Unsupported Java Version", javaRuntimeError, null);
      System.exit(ExitCodes.UNSUPPORTED_JAVA_VERSION);
    }

    try {
      initAndCallStartup(args);
    }
    catch (Throwable t) {
      ShowErrorCaller.showErrorDialog("Start Failed", t.getMessage(), t);
      System.exit(ExitCodes.STARTUP_EXCEPTION);
    }
  }

  private static void initAndCallStartup(String[] args) throws Exception {
    StatCollector stat = new StatCollector();

    File modulesDirectory = BootstrapClassLoaderUtil.getModulesDirectory();

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(ContainerStartup.ARGS, args);
    map.put(ContainerStartup.STAT_COLLECTOR, stat);

    ContainerStartup containerStartup = BootstrapClassLoaderUtil.buildContainerStartup(map, modulesDirectory, SystemContainerLogger.INSTANCE, new DesktopSwtJava9ModuleProcessor());

    containerStartup.run(map);
  }
}

