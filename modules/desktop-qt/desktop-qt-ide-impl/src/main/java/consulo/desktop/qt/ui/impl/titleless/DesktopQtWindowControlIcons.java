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

import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.os.UnixOperationSystem;
import consulo.ui.image.Image;

/**
 * The images a window control of a titleless window is drawn with, one set per desktop.
 * <p/>
 * A window control is the one widget of the ide the user compares against the rest of the desktop rather than
 * against the ide, so it is drawn as the desktop draws it: the round buttons of a plasma title bar under plasma,
 * the flat circles of adwaita under gnome. Which desktop is running is read off {@code XDG_CURRENT_DESKTOP} by
 * {@link UnixOperationSystem}; every other desktop, and every session which names none, is given the plasma set,
 * which is a shape both desktops draw and neither owns.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public final class DesktopQtWindowControlIcons {
    /**
     * How the button is drawn - and {@link #INACTIVE} is the state of the window rather than of the button: a
     * desktop dims the controls of a window which does not hold the focus.
     */
    public enum State {
        NORMAL,
        HOVER,
        PRESSED,
        INACTIVE
    }

    private enum Theme {
        KDE,
        GNOME
    }

    private static final Theme ourTheme = detectTheme();

    private DesktopQtWindowControlIcons() {
    }

    public static Image iconOf(DesktopQtWindowButton.Kind kind, State state) {
        return switch (ourTheme) {
            case KDE -> kdeIcon(kind, state);
            case GNOME -> gnomeIcon(kind, state);
        };
    }

    private static Theme detectTheme() {
        return Platform.current().os() instanceof UnixOperationSystem unix && unix.isGNOME() ? Theme.GNOME : Theme.KDE;
    }

    private static Image kdeIcon(DesktopQtWindowButton.Kind kind, State state) {
        return switch (kind) {
            case MINIMIZE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxKdeMinimize();
                case HOVER -> PlatformIconGroup.linuxKdeMinimizehover();
                case PRESSED -> PlatformIconGroup.linuxKdeMinimizepressed();
                case INACTIVE -> PlatformIconGroup.linuxKdeMinimizeinactive();
            };
            case MAXIMIZE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxKdeMaximize();
                case HOVER -> PlatformIconGroup.linuxKdeMaximizehover();
                case PRESSED -> PlatformIconGroup.linuxKdeMaximizepressed();
                case INACTIVE -> PlatformIconGroup.linuxKdeMaximizeinactive();
            };
            case RESTORE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxKdeRestore();
                case HOVER -> PlatformIconGroup.linuxKdeRestorehover();
                case PRESSED -> PlatformIconGroup.linuxKdeRestorepressed();
                case INACTIVE -> PlatformIconGroup.linuxKdeRestoreinactive();
            };
            case CLOSE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxKdeClose();
                case HOVER -> PlatformIconGroup.linuxKdeClosehover();
                case PRESSED -> PlatformIconGroup.linuxKdeClosepressed();
                case INACTIVE -> PlatformIconGroup.linuxKdeCloseinactive();
            };
        };
    }

    private static Image gnomeIcon(DesktopQtWindowButton.Kind kind, State state) {
        return switch (kind) {
            case MINIMIZE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxGnomeMinimize();
                case HOVER -> PlatformIconGroup.linuxGnomeMinimizehover();
                case PRESSED -> PlatformIconGroup.linuxGnomeMinimizepressed();
                case INACTIVE -> PlatformIconGroup.linuxGnomeMinimizeinactive();
            };
            case MAXIMIZE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxGnomeMaximize();
                case HOVER -> PlatformIconGroup.linuxGnomeMaximizehover();
                case PRESSED -> PlatformIconGroup.linuxGnomeMaximizepressed();
                case INACTIVE -> PlatformIconGroup.linuxGnomeMaximizeinactive();
            };
            case RESTORE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxGnomeRestore();
                case HOVER -> PlatformIconGroup.linuxGnomeRestorehover();
                case PRESSED -> PlatformIconGroup.linuxGnomeRestorepressed();
                case INACTIVE -> PlatformIconGroup.linuxGnomeRestoreinactive();
            };
            case CLOSE -> switch (state) {
                case NORMAL -> PlatformIconGroup.linuxGnomeClose();
                case HOVER -> PlatformIconGroup.linuxGnomeClosehover();
                case PRESSED -> PlatformIconGroup.linuxGnomeClosepressed();
                case INACTIVE -> PlatformIconGroup.linuxGnomeCloseinactive();
            };
        };
    }
}
