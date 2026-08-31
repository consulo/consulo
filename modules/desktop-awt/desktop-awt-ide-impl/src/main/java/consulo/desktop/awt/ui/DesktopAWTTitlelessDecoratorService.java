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
package consulo.desktop.awt.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import consulo.ui.ex.awt.AWTTitlelessDecorator;
import jakarta.inject.Singleton;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@ServiceImpl
@Singleton
public class DesktopAWTTitlelessDecoratorService implements TitlelessDecoratorService {
    @Override
    public TitlelessDecorator of(Object pane, String windowId) {
        JRootPane rootPane = (JRootPane) pane;

        PlatformOperatingSystem os = Platform.current().os();
        if (os.isMac()) {
            return new AWTTitlelessDecorator.MacFrameDecorator(rootPane);
        }

        if (os.isWindows() || os.isLinux()) {
            // no sense for it - we already without title
            if (TitlelessDecorator.MAIN_WINDOW.equals(windowId)) {
                return TitlelessDecorator.NOTHING;
            }

            if (TitlelessDecorator.WELCOME_WINDOW.equals(windowId)) {
                return new AWTTitlelessDecorator.WindowsFameDecorator(rootPane);
            }

            // FIXME for now we not support other titleless - due bug with moving
        }

        return TitlelessDecorator.NOTHING;
    }
}
