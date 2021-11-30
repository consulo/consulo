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
package consulo.desktop.start;

import com.intellij.jna.JnaLoader;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import consulo.logging.Logger;

/**
 * @author VISTALL
 * @since 25/10/2021
 * <p>
 * https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-registerapplicationrestart
 * <p>
 * https://github.com/chromium/chromium/blob/72ceeed2ebcd505b8d8205ed7354e862b871995e/chrome/browser/chrome_browser_main_win.cc#L779
 */
public class WindowsAutoRestartManager {
  private static final Logger LOG = Logger.getInstance(WindowsAutoRestartManager.class);

  private static final int RESTART_NO_CRASH = 1;
  private static final int RESTART_NO_HANG = 2;
  private static final int RESTART_NO_PATCH = 4;
  // just for info
  @SuppressWarnings("unused")
  private static final int RESTART_NO_REBOOT = 8;

  /**
   * Windows only
   */
  public static void register() {
    // if jna failed or restart not supported - do not register
    if (!JnaLoader.isLoaded()) {
      return;
    }

    try {
      int flags = RESTART_NO_CRASH | RESTART_NO_HANG | RESTART_NO_PATCH;
      // set zero arguments for not start Consulo again with same args
      WinNT.HRESULT code = Kernel32.INSTANCE.RegisterApplicationRestart(new char[0], flags);
      LOG.info("RegisterApplicationRestart: " + code);
    }
    catch (Throwable t) {
      LOG.warn(t);
    }
  }
}
