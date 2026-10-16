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

import consulo.ui.Toast;
import consulo.ui.internal.ToastInternal;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The toasts floating over one window: child widgets of its central widget, outside of its layout, in a column in the
 * bottom right corner of its area - the whole central widget when it has none - the newest at the bottom. Children and not top level windows - those cannot be placed on
 * Wayland.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
final class DesktopQtToastStack extends QObject {
    private static final int WIDTH = 360;
    private static final int MARGIN = 8;

    private final QWidget myHost;
    private final List<Toast> myToasts = new ArrayList<>();

    private @Nullable QtComponentDelegate<?> myArea;
    private @Nullable QWidget myAreaWidget;

    DesktopQtToastStack(QWidget host) {
        super(host);
        myHost = host;
        host.installEventFilter(this);
    }

    @Override
    public boolean eventFilter(QObject watched, QEvent event) {
        QEvent.Type type = event.type();
        if (watched == myHost && type == QEvent.Type.Resize) {
            relayout();
        }
        else if (watched == myAreaWidget && (type == QEvent.Type.Resize || type == QEvent.Type.Move)) {
            relayout();
        }
        return false;
    }

    void setArea(@Nullable QtComponentDelegate<?> area) {
        myArea = area;
        relayout();
    }

    void show(Toast toast) {
        if (myToasts.contains(toast) || !(toast instanceof QtComponentDelegate<?> delegate)) {
            return;
        }
        if (toast instanceof ToastInternal internal) {
            internal.setShadow(true);
        }
        delegate.bind(myHost, null);
        myToasts.add(toast);
        QWidget widget = delegate.toQtComponent();
        if (widget != null) {
            widget.show();
            widget.raise();
        }
        relayout();
    }

    void hide(Toast toast) {
        if (!myToasts.remove(toast)) {
            return;
        }
        if (toast instanceof QtComponentDelegate<?> delegate) {
            QWidget widget = delegate.toQtComponent();
            if (widget != null && !widget.isDisposed()) {
                widget.hide();
            }
            delegate.disposeQt();
        }
        relayout();
    }

    private void relayout() {
        if (myHost.isDisposed()) {
            return;
        }
        QRect area = areaBounds();
        int bottom = area.bottom() + 1 - MARGIN;
        int x = area.right() + 1 - WIDTH - MARGIN;
        for (int i = myToasts.size() - 1; i >= 0; i--) {
            if (!(myToasts.get(i) instanceof QtComponentDelegate<?> delegate)) {
                continue;
            }
            QWidget widget = delegate.toQtComponent();
            if (widget == null || widget.isDisposed()) {
                continue;
            }
            int height = widget.hasHeightForWidth() ? widget.heightForWidth(WIDTH) : widget.sizeHint().height();
            int y = bottom - height;
            widget.setGeometry(x, y, WIDTH, height);
            widget.setVisible(y >= area.top());
            widget.raise();
            bottom = y;
        }
    }

    private QRect areaBounds() {
        QtComponentDelegate<?> area = myArea;
        QWidget widget = area == null ? null : area.toQtComponent();
        if (widget != myAreaWidget) {
            if (myAreaWidget != null && !myAreaWidget.isDisposed()) {
                myAreaWidget.removeEventFilter(this);
            }
            myAreaWidget = widget;
            if (widget != null && !widget.isDisposed()) {
                widget.installEventFilter(this);
            }
        }
        if (widget != null && !widget.isDisposed() && myHost.isAncestorOf(widget)) {
            QPoint topLeft = widget.mapTo(myHost, new QPoint(0, 0));
            return new QRect(topLeft.x(), topLeft.y(), widget.width(), widget.height());
        }
        return new QRect(0, 0, myHost.width(), myHost.height());
    }
}
