/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.application.jna.windows;

import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.PowerSaveModeSystemTracker;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.jna.JnaLoader;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20/04/2023
 */
@ExtensionImpl
public class WindowsPowerSaveModeTracker implements PowerSaveModeSystemTracker {
  private static final Logger LOG = Logger.getInstance(WindowsPowerSaveModeTracker.class);

  @Override
  public void register(@Nonnull Notifier notifier) {
    if (!Platform.current().os().isWindows() || !JnaLoader.isLoaded()) {
      return;
    }

    try {
      Powrprof powrprof = Powrprof.load();

      WinNT.HRESULT code = powrprof.PowerRegisterForEffectivePowerModeNotifications(Powrprof.EFFECTIVE_POWER_MODE_V2, (mode, pointer) -> {
        if (mode == Powrprof.EFFECTIVE_POWER_MODE.EffectivePowerModeBatterySaver) {
          notifier.enterPowerSaveMode();
        } else {
          notifier.exitPowerSaveMode();
        }
      }, new IntByReference(0), new IntByReference(0));

      LOG.info("PowerRegisterForEffectivePowerModeNotifications: " + code);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }
}
