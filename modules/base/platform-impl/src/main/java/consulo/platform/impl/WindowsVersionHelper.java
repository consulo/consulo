/*
 * Copyright 2013-2015 must-be.org
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

package consulo.platform.impl;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 24.01.15
 */
public class WindowsVersionHelper {
  public interface Version extends Library {
    Version INSTANCE = (Version)Native.load("Version", Version.class, W32APIOptions.UNICODE_OPTIONS);

    public int GetFileVersionInfoSizeW(String lptstrFilename, int dwDummy);

    public boolean GetFileVersionInfoW(String lptstrFilename, int dwHandle, int dwLen, Pointer lpData);

    public int VerQueryValueW(Pointer pBlock, String lpSubBlock, PointerByReference lplpBuffer, IntByReference puLen);
  }

  public static class VS_FIXEDFILEINFO extends com.sun.jna.Structure {
    private static final List __FIELDS =
            Arrays.asList("dwSignature", "dwStrucVersion", "dwFileVersionMS", "dwFileVersionLS", "dwProductVersionMS", "dwProductVersionLS", "dwFileFlagsMask", "dwFileFlags", "dwFileOS", "dwFileType",
                          "dwFileSubtype", "dwFileDateMS", "dwFileDateLS");

    public int dwSignature;
    public int dwStrucVersion;
    public int dwFileVersionMS;
    public int dwFileVersionLS;
    public int dwProductVersionMS;
    public int dwProductVersionLS;
    public int dwFileFlagsMask;
    public int dwFileFlags;
    public int dwFileOS;
    public int dwFileType;
    public int dwFileSubtype;
    public int dwFileDateMS;
    public int dwFileDateLS;

    public VS_FIXEDFILEINFO(com.sun.jna.Pointer p) {
      super(p);
    }

    @Override
    protected List getFieldOrder() {
      return __FIELDS;
    }
  }

  @Nonnull
  public static String getVersion(String path, int parts) {
    path = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);

    int dwDummy = 0;
    int versionlength = Version.INSTANCE.GetFileVersionInfoSizeW(path, dwDummy);

    Pointer lpData = new Memory(versionlength);

    PointerByReference lplpBuffer = new PointerByReference();
    IntByReference puLen = new IntByReference();
    Version.INSTANCE.GetFileVersionInfoW(path, 0, versionlength, lpData);
    Version.INSTANCE.VerQueryValueW(lpData, "\\", lplpBuffer, puLen);

    VS_FIXEDFILEINFO lplpBufStructure = new VS_FIXEDFILEINFO(lplpBuffer.getValue());
    lplpBufStructure.read();

    int[] rtnData = new int[4];
    rtnData[0] = lplpBufStructure.dwFileVersionMS >> 16;
    rtnData[1] = lplpBufStructure.dwFileVersionMS & 0xffff;
    rtnData[2] = lplpBufStructure.dwFileVersionLS >> 16;
    rtnData[3] = lplpBufStructure.dwFileVersionLS & 0xffff;

    if (parts < 0 || parts >= 4) {
      throw new IllegalArgumentException("Must be bigger than 0 and lower 4");
    }
    
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts; i++) {
      if (i != 0) {
        builder.append(".");
      }
      builder.append(rtnData[i]);
    }
    return builder.toString();
  }
}