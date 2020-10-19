// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.process;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.logging.Logger;

/**
 * Do not call this class directly - use {@link consulo.execution.process.OSProcessUtil} instead.
 *
 * @author Alexey.Ushakov
 */
public class WinProcessManager {
  private static final Logger LOG = Logger.getInstance(WinProcessManager.class);

  private WinProcessManager() {
  }

  @Deprecated
  public static int getProcessId(Process process) {
    return (int)process.pid();
  }

  public static boolean kill(Process process, boolean tree) {
    return kill(-1, process, tree);
  }

  public static boolean kill(int pid, boolean tree) {
    return kill(pid, null, tree);
  }

  private static boolean kill(int pid, Process process, boolean tree) {
    LOG.assertTrue(pid > 0 || process != null);
    try {
      if (process != null) {
        pid = getProcessId(process);
      }
      String[] cmdArray = {"taskkill", "/f", "/pid", String.valueOf(pid), tree ? "/t" : ""};
      if (LOG.isDebugEnabled()) {
        LOG.debug(StringUtil.join(cmdArray, " "));
      }
      Process p = new ProcessBuilder(cmdArray).redirectErrorStream(true).start();
      String output = FileUtil.loadTextAndClose(p.getInputStream());
      int res = p.waitFor();

      if (res != 0 && (process == null || process.isAlive())) {
        LOG.warn(StringUtil.join(cmdArray, " ") + " failed: " + output);
        return false;
      }
      else if (LOG.isDebugEnabled()) {
        LOG.debug(output);
      }

      return true;
    }
    catch (Exception e) {
      LOG.warn(e);
    }
    return false;
  }
}