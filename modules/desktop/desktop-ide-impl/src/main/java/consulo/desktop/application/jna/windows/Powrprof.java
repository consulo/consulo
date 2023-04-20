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

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

/**
 * @author VISTALL
 * @since 20/04/2023
 */
public interface Powrprof extends Library {
  static Powrprof load() {
    return Native.load("Powrprof.dll", Powrprof.class);
  }

  interface EFFECTIVE_POWER_MODE_CALLBACK extends Callback {
    void callback(EFFECTIVE_POWER_MODE mode, Pointer pointer);
  }

  enum EFFECTIVE_POWER_MODE implements NativeMapped {
    EffectivePowerModeBatterySaver,
    EffectivePowerModeBetterBattery,
    EffectivePowerModeBalanced,
    EffectivePowerModeHighPerformance,
    EffectivePowerModeMaxPerformance,
    EffectivePowerModeGameMode,
    EffectivePowerModeMixedReality;

    @Override
    public Object toNative() {
      return ordinal();
    }

    @Override
    public Object fromNative(Object o, FromNativeContext fromNativeContext) {
      if (o instanceof Number) {
        int i = ((Number)o).intValue();
        return values()[i];
      }
      throw new IllegalArgumentException(String.valueOf(o));
    }

    @Override
    public Class<?> nativeType() {
      return int.class;
    }
  }

  long EFFECTIVE_POWER_MODE_V1 = 0x1;
  long EFFECTIVE_POWER_MODE_V2 = 0x2;

  WinNT.HRESULT PowerRegisterForEffectivePowerModeNotifications(long version,
                                                                EFFECTIVE_POWER_MODE_CALLBACK callback,
                                                                IntByReference context,
                                                                IntByReference handler);
}
