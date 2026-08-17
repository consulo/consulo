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
package consulo.desktop.qt.ui.impl.titleless;

import consulo.annotation.component.ServiceImpl;
import consulo.desktop.qt.ui.impl.DesktopQtWindowImpl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Which windows of the qt frontend draw their own decoration.
 * <p/>
 * Only the two windows the ide itself lives in are titleless. A dialog and a popup keep whatever the display server
 * gives them: a dialog is short lived and carries no menu bar to put in a header, and a popup is frameless already.
 * Changing that is a matter of naming another window id below.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
@Singleton
@ServiceImpl
public class DesktopQtTitlelessDecoratorService implements TitlelessDecoratorService {
    @RequiredUIAccess
    @Override
    public TitlelessDecorator of(Object pane, String windowId) {
        DesktopQtTitleBarPlacement placement = placementOf(windowId);

        if (placement != null && pane instanceof DesktopQtWindowImpl window) {
            window.installTitleBar(placement);
        }

        // a header standing in the layout as a strip holds the room it needs itself, only one floating over the
        // content has to be kept clear by that content
        return placement == DesktopQtTitleBarPlacement.OVERLAY
            ? new DesktopQtTitlelessDecorator(DesktopQtTitleBar.getBarHeight())
            : TitlelessDecorator.NOTHING;
    }

    private static @Nullable DesktopQtTitleBarPlacement placementOf(String windowId) {
        return switch (windowId) {
            // the frame carries the menu bar of the ide, and the header is where a titleless window shows it
            case TitlelessDecorator.MAIN_WINDOW -> DesktopQtTitleBarPlacement.STRIP;
            // the welcome screen has no menu bar and no title of its own to show, so a header over it would be an
            // empty band - the controls float over its content instead, and it is dragged by its background
            case TitlelessDecorator.WELCOME_WINDOW -> DesktopQtTitleBarPlacement.OVERLAY;
            default -> null;
        };
    }
}
