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

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.PopupMenu;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QPoint;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * Menu bound to a target widget - what a toolbar button holding an action group opens.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtPopupMenuImpl extends DesktopQtMenuImpl implements PopupMenu {
    private @Nullable QWidget myTargetWidget;

    private boolean myOpenOnClick;

    public DesktopQtPopupMenuImpl(Component target) {
        super(LocalizeValue.empty());

        if (target instanceof QtComponentDelegate<?> delegate) {
            delegate.whenBound(this::bindTarget);
        }
    }

    private void bindTarget(QWidget widget) {
        myTargetWidget = widget;

        if (widget instanceof QAbstractButton button) {
            // the flag is set after the menu is created, so it is read on the click and not on the connect
            button.clicked.connect(checked -> {
                if (myOpenOnClick) {
                    showBelowTarget();
                }
            });
        }
    }

    @Override
    public void setOpenOnClick(boolean openOnClick) {
        myOpenOnClick = openOnClick;
    }

    @RequiredUIAccess
    @Override
    public void show(int relativeX, int relativeY) {
        QWidget target = myTargetWidget;
        if (target == null || target.isDisposed()) {
            return;
        }

        popupAt(target.mapToGlobal(new QPoint(relativeX, relativeY)));
    }

    @RequiredUIAccess
    private void showBelowTarget() {
        QWidget target = myTargetWidget;
        if (target == null || target.isDisposed()) {
            return;
        }

        popupAt(target.mapToGlobal(new QPoint(0, target.height())));
    }

    @RequiredUIAccess
    private void popupAt(QPoint globalPosition) {
        popupDetached(myTargetWidget, globalPosition);
    }
}
