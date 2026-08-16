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
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QPoint;
import io.qt.widgets.QWidget;

/**
 * A popup which hangs off its target rather than being placed - what a completion list, a hint or a submenu is
 * shown with.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLightPopupImpl extends DesktopQtPopupImpl implements LightPopup {
    public DesktopQtLightPopupImpl(PopupOptions options) {
        super(options);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component target) {
        QWidget widget = toQtWidget(target);
        if (widget == null) {
            return;
        }

        QPoint anchor = myOptions.getPosition() == PopupPosition.END
            ? new QPoint(widget.width(), 0)
            : new QPoint(0, widget.height());

        setOwner(widget);

        showAtGlobal(widget.mapToGlobal(anchor));
    }

    @Override
    @RequiredUIAccess
    public void showAt(Component target, int x, int y, int anchorHeight) {
        showAtComponent(target, x, y, anchorHeight);
    }
}
