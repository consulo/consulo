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

import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;

/**
 * A layout keeps the size hint a child had when it was last laid out, so a window or popup fitted to its content
 * again has to tell every child that its hint may have changed first.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class DesktopQtGeometry {
    private DesktopQtGeometry() {
    }

    public static void invalidateHierarchy(QWidget root) {
        for (QWidget child : root.findChildren(QWidget.class)) {
            child.updateGeometry();
        }
        QCoreApplication.sendPostedEvents(null, QEvent.Type.LayoutRequest);
        for (QLayout child : root.findChildren(QLayout.class)) {
            child.invalidate();
        }
        QLayout layout = root.layout();
        if (layout != null) {
            layout.invalidate();
            layout.activate();
        }
    }
}
