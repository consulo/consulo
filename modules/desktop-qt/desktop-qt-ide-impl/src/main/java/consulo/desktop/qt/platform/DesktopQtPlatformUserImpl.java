/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.platform;

import consulo.desktop.util.windows.WindowsElevationUtil;
import consulo.platform.Platform;
import consulo.platform.impl.PlatformUserImpl;
import io.qt.core.Qt;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QPalette;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-29
 */
public class DesktopQtPlatformUserImpl extends PlatformUserImpl {
    private final Platform myPlatform;

    public DesktopQtPlatformUserImpl(Platform platform, Map<String, String> jvmProperties) {
        super(jvmProperties);
        myPlatform = platform;
    }

    @Override
    public boolean superUser() {
        // this is correct ?
        if (myPlatform.os().isUnix() && "root".equals(System.getenv("USER"))) {
            return true;
        }
        return WindowsElevationUtil.isUnderElevation();
    }

    @Override
    public boolean darkTheme() {
        Qt.ColorScheme colorScheme = QGuiApplication.styleHints().colorScheme();

        return switch (colorScheme) {
            case Dark -> true;
            case Light -> false;
            default -> QGuiApplication.palette().color(QPalette.ColorRole.Window).lightness() < 128;
        };
    }
}
