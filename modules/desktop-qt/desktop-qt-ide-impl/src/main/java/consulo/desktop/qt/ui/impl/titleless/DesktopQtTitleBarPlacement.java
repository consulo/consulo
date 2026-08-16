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

/**
 * What a window which draws its own decoration puts where the title bar of the display server used to be.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public enum DesktopQtTitleBarPlacement {
    /**
     * A {@link DesktopQtTitleBar} above the content - the menu bar of the window, its title and the window buttons
     * in one row, the way a kde application which decorates itself is built.
     */
    STRIP,
    /**
     * No header, but the window controls all the same - they float over the top right corner of the content rather
     * than standing in a row of their own, so nothing the window shows is pushed down by them. A window with no
     * menu bar and no title worth a row of its own is decorated this way.
     */
    OVERLAY,
    /**
     * No header at all: the content reaches every edge of the window, and the window is dragged by whatever of its
     * background a press falls through to.
     */
    NONE
}
