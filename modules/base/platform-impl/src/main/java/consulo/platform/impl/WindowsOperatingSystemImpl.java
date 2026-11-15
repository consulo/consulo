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
package consulo.platform.impl;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.ShortByReference;
import consulo.platform.os.WindowsOperatingSystem;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class WindowsOperatingSystemImpl extends PlatformOperatingSystemImpl implements WindowsOperatingSystem {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemImpl.class);

    // https://learn.microsoft.com/en-us/windows/win32/sysinfo/image-file-machine-constants
    private static final int IMAGE_FILE_MACHINE_I386 = 0x014c;
    private static final int IMAGE_FILE_MACHINE_AMD64 = 0x8664;
    private static final int IMAGE_FILE_MACHINE_ARM64 = 0xAA64;

    private final Function<String, String> myGetEnvFunc;
    private final LazyValue<String> myArchString = LazyValue.notNull(this::detectArchString);
    private final boolean isWin7OrNewer;
    private final boolean isWin8OrNewer;
    private final boolean isWin10OrNewer;

    private Boolean myWindows11OrLater;

    public WindowsOperatingSystemImpl(Map<String, String> jvmProperties,
                                      Function<String, String> getEnvFunc,
                                      Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);
        myGetEnvFunc = getEnvFunc;
        isWin7OrNewer = isOsVersionAtLeast("6.1");
        isWin8OrNewer = isOsVersionAtLeast("6.2");
        isWin10OrNewer = isOsVersionAtLeast("10.0");
    }

    @Override
    public boolean isWindows7OrNewer() {
        return isWin7OrNewer;
    }

    @Override
    public boolean isWindows8OrNewer() {
        return isWin8OrNewer;
    }

    @Override
    public boolean isWindows10OrNewer() {
        return isWin10OrNewer;
    }

    @Override
    public boolean isWindows11OrNewer() {
        if (myWindows11OrLater == null) {
            myWindows11OrLater = isWindows11OrNewerImpl();
        }
        return myWindows11OrLater;
    }

    
    @Override
    public String getWindowsFileVersion(Path path, int parts) {
        if (!isWindows()) {
            throw new IllegalArgumentException("Windows OS required");
        }
        return WindowsVersionHelper.getVersion(path.toString(), parts);
    }

    @Override
    public String archString() {
        return myArchString.get();
    }

    /**
     * The machine's architecture - a 32-bit JVM under WoW64, or an x86-64 one emulated on an ARM64 Windows, is not it.
     */
    private String detectArchString() {
        String nativeMachine = nativeMachine();
        if (nativeMachine != null) {
            return nativeMachine;
        }
        // before Windows 10 1709: a WoW64 process sees its own architecture in PROCESSOR_ARCHITECTURE, this one is the machine's
        String machineArch = myGetEnvFunc.apply("PROCESSOR_ARCHITEW6432");
        if (!StringUtil.isEmpty(machineArch)) {
            return machineArch;
        }
        String processArch = myGetEnvFunc.apply("PROCESSOR_ARCHITECTURE");
        if (!StringUtil.isEmpty(processArch)) {
            return processArch;
        }
        return jvmArchString();
    }

    // https://learn.microsoft.com/en-us/windows/win32/api/wow64apiset/nf-wow64apiset-iswow64process2
    private static @Nullable String nativeMachine() {
        try {
            // BOOL IsWow64Process2(HANDLE process, USHORT *processMachine, USHORT *nativeMachine), absent before Windows 10 1709
            com.sun.jna.Function isWow64Process2 = com.sun.jna.Function.getFunction("kernel32", "IsWow64Process2");
            WinNT.HANDLE process = Kernel32.INSTANCE.GetCurrentProcess();
            ShortByReference processMachine = new ShortByReference();
            ShortByReference nativeMachine = new ShortByReference();
            if (isWow64Process2.invokeInt(new Object[]{process, processMachine, nativeMachine}) == 0) {
                return null;
            }
            return switch (Short.toUnsignedInt(nativeMachine.getValue())) {
                case IMAGE_FILE_MACHINE_I386 -> "x86";
                case IMAGE_FILE_MACHINE_AMD64 -> "AMD64";
                case IMAGE_FILE_MACHINE_ARM64 -> "ARM64";
                default -> null;
            };
        }
        catch (Throwable e) {
            LOG.debug("Cannot ask the native machine of the process", e);
            return null;
        }
    }

    protected boolean isWindows11OrNewerImpl() {
        return OS_NAME.contains("Windows 11") || OS_NAME.contains("Windows Server 2025");
    }
}
