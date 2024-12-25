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

import consulo.platform.os.UnixOperationSystem;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-25
 */
public class UnixOperationSystemImpl extends PlatformOperatingSystemImpl implements UnixOperationSystem {
    private boolean isWayland;
    private boolean isGNOME;
    private boolean isI3;
    private boolean isKDE;
    private boolean isXfce;

    public UnixOperationSystemImpl(Map<String, String> jvmProperties,
                                   Function<String, String> getEnvFunc,
                                   Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);

        isWayland = getEnvFunc.apply("WAYLAND_DISPLAY") != null;
        String desktop = getEnvFunc.apply("XDG_CURRENT_DESKTOP"), gdmSession = getEnvFunc.apply("GDMSESSION");
        isGNOME = desktop != null && desktop.contains("GNOME") || gdmSession != null && gdmSession.contains("gnome");
        isKDE = !isGNOME && (desktop != null && desktop.contains("KDE") || getEnvFunc.apply("KDE_FULL_SESSION") != null);
        isXfce = !isGNOME && !isKDE && (desktop != null && desktop.contains("XFCE"));
        isI3 = !isGNOME && !isKDE && !isXfce && (desktop != null && desktop.contains("i3"));
    }

    @Override
    public boolean isGNOME() {
        return isGNOME;
    }

    @Override
    public boolean isUnix() {
        return true;
    }

    @Override
    public boolean isXWindow() {
        return true; // always true? sure?
    }

    @Override
    public boolean isKDE() {
        return isKDE;
    }

    @Override
    public boolean isXfce() {
        return isXfce;
    }

    @Override
    public boolean isI3() {
        return isI3;
    }

    @Override
    public boolean isWayland() {
        return isWayland;
    }
}
