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

import consulo.platform.os.WindowsOperatingSystem;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class WindowsOperatingSystemImpl extends PlatformOperatingSystemImpl implements WindowsOperatingSystem {
    private final boolean isWin7OrNewer;
    private final boolean isWin8OrNewer;
    private final boolean isWin10OrNewer;

    private Boolean myWindows11OrLater;

    public WindowsOperatingSystemImpl(Map<String, String> jvmProperties,
                                      Function<String, String> getEnvFunc,
                                      Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);
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

    @Nonnull
    @Override
    public String getWindowsFileVersion(@Nonnull Path path, int parts) {
        if (!isWindows()) {
            throw new IllegalArgumentException("Windows OS required");
        }
        return WindowsVersionHelper.getVersion(path.toString(), parts);
    }

    protected boolean isWindows11OrNewerImpl() {
        return OS_NAME.contains("Windows 11") || OS_NAME.contains("Windows Server 2025");
    }
}
