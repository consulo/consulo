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

import consulo.desktop.qt.ui.impl.image.DesktopQtAnimationHost;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.gui.QAction;
import io.qt.widgets.QMenu;
import io.qt.widgets.QMenuBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

final class DesktopQtActionAnimationHost implements DesktopQtAnimationHost {
    private final QAction myAction;

    DesktopQtActionAnimationHost(QAction action) {
        myAction = action;
    }

    @RequiredUIAccess
    @Override
    public void repaintAnimatedImage() {
        QAction action = myAction;
        if (action.isDisposed()) {
            return;
        }

        for (QObject object : action.associatedObjects()) {
            if (!(object instanceof QWidget widget) || widget.isDisposed() || !widget.isVisible()) {
                continue;
            }

            QRect geometry = actionGeometry(widget, action);
            if (geometry != null && !geometry.isEmpty()) {
                widget.update(geometry);
            }
        }
    }

    private static @Nullable QRect actionGeometry(QWidget widget, QAction action) {
        if (widget instanceof QMenu menu) {
            return menu.actionGeometry(action);
        }
        if (widget instanceof QMenuBar menuBar) {
            return menuBar.actionGeometry(action);
        }
        return null;
    }
}
