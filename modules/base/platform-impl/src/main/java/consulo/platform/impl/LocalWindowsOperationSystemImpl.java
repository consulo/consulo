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
package consulo.platform.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-11-15
 */
public class LocalWindowsOperationSystemImpl extends WindowsOperatingSystemImpl {
    public LocalWindowsOperationSystemImpl(Map<String, String> jvmProperties, Function<String, String> getEnvFunc, Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);
    }

    @Override
    protected boolean isWindows11OrNewerImpl() {
        if (super.isWindows11OrNewerImpl()) {
            return true;
        }
        try {
            // windows server check
            if (OS_NAME.contains("Windows Server 2022")) {
                int buildNumber = getBuildNumberFromKernel32();
                if (buildNumber >= 26_039) {
                    return true;
                }
            }
            else if (OS_NAME.contains("Windows 10")) {
                int buildNumber = getBuildNumberFromKernel32();
                if (buildNumber >= 22_000) {
                    return true;
                }
            }
        }
        catch (Throwable ignored) {
        }

        return false;
    }

    private int getBuildNumberFromKernel32() {
        Path kernel32dll = Path.of(System.getenv("windir")).resolve("System32/kernel32.dll");
        if (Files.exists(kernel32dll)) {
            WindowsVersionHelper.VS_FIXEDFILEINFO versionRaw = WindowsVersionHelper.getVersionRaw(kernel32dll.toString());
            return versionRaw.dwProductVersionLS >> 16;
        }

        return 0;
    }
}
