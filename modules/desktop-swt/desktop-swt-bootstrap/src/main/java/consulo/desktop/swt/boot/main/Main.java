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
import consulo.container.impl.SystemContainerLogger;
import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.container.util.StatCollector;
import consulo.util.nodep.SystemInfoRt;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class Main {
  private Main() {
  }

  public static void main(final String[] args) {
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

    ContainerStartup containerStartup = BootstrapClassLoaderUtil.buildContainerStartup(map, modulesDirectory, SystemContainerLogger.INSTANCE, new DesktopSwtJava9ModuleProcessor());

    containerStartup.run(map);
  }

  public static void showMessage(String title, Throwable t) {
    StringWriter out = new StringWriter();
    PrintWriter printWriter = new PrintWriter(out);
    t.printStackTrace(printWriter);
    showMessage(title, out.toString(), true);
  }

  public static void showMessage(String title, String message, boolean error) {
    System.out.println(title);
    System.out.println(message);
  }
}

