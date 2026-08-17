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

import consulo.ui.Component;
import consulo.ui.HeavyPopup;
import consulo.ui.PopupOptions;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.gui.QScreen;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * A popup with nothing to point at, so it is placed rather than anchored - what a menu action or a shortcut
 * raises.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtHeavyPopupImpl extends DesktopQtPopupImpl implements HeavyPopup {
    public DesktopQtHeavyPopupImpl(PopupOptions options) {
        super(options);
    }

    @Override
    @RequiredUIAccess
    public void showInCenterOf(@Nullable Window window) {
        checkNotDisposed();

        setOwner(window == null ? null : TargetQt.to(window));

        myComponent.adjustSize();

        // measured against the window the popup was actually hung under and not against the one it was asked to
        // centre over - the popup is placed relative to its parent surface, so the two have to be the same window
        QRect over = ownerGeometry(ownerWindow());
        QSize size = myComponent.size();

        showAtGlobal(new QPoint(
            over.x() + (over.width() - size.width()) / 2,
            over.y() + (over.height() - size.height()) / 2
        ));
    }

    @Override
    @RequiredUIAccess
    public void showAt(Component target, int x, int y, int anchorHeight) {
        showAtComponent(target, x, y, anchorHeight);
    }

    private static QRect ownerGeometry(@Nullable QWidget owner) {
        if (owner != null) {
            return owner.geometry();
        }

        QScreen screen = QApplication.primaryScreen();

        return screen == null ? new QRect(0, 0, 0, 0) : screen.availableGeometry();
    }
}
