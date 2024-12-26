/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.application.dateTime;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.internal.dateTime.DateTimeFormatProvider;
import consulo.platform.Platform;
import consulo.util.jna.JnaLoader;
import consulo.util.lang.StringUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author VISTALL
 * @since 2024-12-26
 */
@ExtensionImpl
public class WindowsDateTimeFormatProvider implements DateTimeFormatProvider {

    @Override
    public DateFormat[] getFormats(Platform platform) {
        return platform.os().isWindows() && JnaLoader.isLoaded() ? getWindowsFormats() : null;
    }

    private static DateFormat[] getWindowsFormats() {
        Kernel32 kernel32 = Native.load("Kernel32", Kernel32.class);
        int dataSize = 128, rv;
        Memory data = new Memory(dataSize);

        rv = kernel32.GetLocaleInfoEx(Kernel32.LOCALE_NAME_USER_DEFAULT, Kernel32.LOCALE_SSHORTDATE, data, dataSize);
        assert rv > 1 : kernel32.GetLastError();
        String shortDate = fixWindowsFormat(new String(data.getCharArray(0, rv - 1)));

        rv = kernel32.GetLocaleInfoEx(Kernel32.LOCALE_NAME_USER_DEFAULT, Kernel32.LOCALE_SSHORTTIME, data, dataSize);
        assert rv > 1 : kernel32.GetLastError();
        String shortTime = fixWindowsFormat(new String(data.getCharArray(0, rv - 1)));

        rv = kernel32.GetLocaleInfoEx(Kernel32.LOCALE_NAME_USER_DEFAULT, Kernel32.LOCALE_STIMEFORMAT, data, dataSize);
        assert rv > 1 : kernel32.GetLastError();
        String mediumTime = fixWindowsFormat(new String(data.getCharArray(0, rv - 1)));

        DateFormat[] formats = new DateFormat[4];
        formats[0] = new SimpleDateFormat(shortDate);
        formats[1] = new SimpleDateFormat(shortTime);
        formats[2] = new SimpleDateFormat(mediumTime);
        formats[3] = new SimpleDateFormat(shortDate + " " + shortTime);
        return formats;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private interface Kernel32 extends StdCallLibrary {
        String LOCALE_NAME_USER_DEFAULT = null;

        int LOCALE_SSHORTDATE = 0x0000001F;
        int LOCALE_SSHORTTIME = 0x00000079;
        int LOCALE_STIMEFORMAT = 0x00001003;

        int GetLocaleInfoEx(String localeName, int lcType, Pointer lcData, int dataSize);

        int GetLastError();
    }

    private static String fixWindowsFormat(String format) {
        format = format.replaceAll("g+", "G");
        format = StringUtil.replace(format, "tt", "a");
        return format;
    }
}
