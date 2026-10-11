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
package consulo.desktop.qt.ui.impl;

import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public record DesktopQtSpotlightHole(QWidget widget, boolean section, int tabIndex) {
    public static DesktopQtSpotlightHole component(QWidget widget, boolean section) {
        return new DesktopQtSpotlightHole(widget, section, -1);
    }

    public static DesktopQtSpotlightHole tab(QWidget tabBar, int tabIndex) {
        return new DesktopQtSpotlightHole(tabBar, false, tabIndex);
    }

    public boolean isTab() {
        return tabIndex >= 0;
    }
}
