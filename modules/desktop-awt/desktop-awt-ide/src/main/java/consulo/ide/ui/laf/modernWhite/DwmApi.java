/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.ui.laf.modernWhite;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

/**
 * @author VISTALL
 * @since 27.02.2015
 */
public interface DwmApi extends Library {
  public static class Wrapper {
    public static boolean DwmIsCompositionEnabled() {
      IntByReference reference = new IntByReference();
      WinNT.HRESULT hresult = INSTANCE.DwmIsCompositionEnabled(reference);
      return W32Errors.S_OK.equals(hresult) && reference.getValue() == 1;
    }

    /**
     * @return 0 - color in format 0xAARRGGBB, 1 - opaque
     */
    public static int[] DwmGetColorizationColor() {
      IntByReference colorRef = new IntByReference();
      IntByReference opaqueRef = new IntByReference();
      WinNT.HRESULT hresult = DwmApi.INSTANCE.DwmGetColorizationColor(colorRef, opaqueRef);
      if(W32Errors.S_OK.equals(hresult)) {
        return new int[] {colorRef.getValue(), opaqueRef.getValue()};
      }
      return new int[2];
    }
  }

  DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);

  WinNT.HRESULT DwmIsCompositionEnabled(IntByReference pfEnabled);

  WinNT.HRESULT DwmGetColorizationColor(IntByReference pcrColorization, IntByReference pfOpaqueBlend);
}
