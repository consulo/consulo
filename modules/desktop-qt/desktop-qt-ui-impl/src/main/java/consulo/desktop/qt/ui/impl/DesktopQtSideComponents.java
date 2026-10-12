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
import consulo.ui.Space;
import io.qt.core.QEvent;
import io.qt.core.QMargins;
import io.qt.core.QObject;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QStyle;
import io.qt.widgets.QToolButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public final class DesktopQtSideComponents {
    private static final int INPLACE_INSET = DesktopQtSpace.toPixels(Space.MEDIUM);

    private final QtComponentDelegate<?> myOwner;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    public DesktopQtSideComponents(QtComponentDelegate<?> owner) {
        myOwner = owner;
    }

    public QLineEdit createLineEdit(QWidget parent) {
        return new QLineEdit(parent) {
            @Override
            public boolean event(QEvent event) {
                boolean result = super.event(event);

                if (isLayoutEvent(event)) {
                    layoutSides(this);
                }
                return result;
            }
        };
    }

    public static boolean isLayoutEvent(QEvent event) {
        QEvent.Type type = event.type();
        return type == QEvent.Type.Resize || type == QEvent.Type.LayoutRequest || type == QEvent.Type.StyleChange;
    }

    public @Nullable Component getPrefix() {
        return myPrefixComponent;
    }

    public @Nullable Component getSuffix() {
        return mySuffixComponent;
    }

    public void setPrefix(@Nullable Component prefixComponent, @Nullable QWidget host) {
        Component oldPrefix = myPrefixComponent;
        if (oldPrefix == prefixComponent) {
            return;
        }

        detach(oldPrefix);

        myPrefixComponent = prefixComponent;

        reattach(host);
    }

    public void setSuffix(@Nullable Component suffixComponent, @Nullable QWidget host) {
        Component oldSuffix = mySuffixComponent;
        if (oldSuffix == suffixComponent) {
            return;
        }

        detach(oldSuffix);

        mySuffixComponent = suffixComponent;

        reattach(host);
    }

    public void attach(QWidget host) {
        bind(myPrefixComponent, host);
        bind(mySuffixComponent, host);

        layoutSides(host);
    }

    private void reattach(@Nullable QWidget host) {
        if (host != null && !host.isDisposed()) {
            attach(host);
        }
    }

    private static void detach(@Nullable Component component) {
        if (component instanceof QtComponentDelegate<?> delegate) {
            delegate.setParent(null);
        }
    }

    private void bind(@Nullable Component component, QWidget host) {
        if (component instanceof QtComponentDelegate<?> delegate) {
            delegate.setParent(myOwner);
            delegate.bind(host, null);

            QWidget widget = delegate.toQtComponent();
            if (widget != null) {
                widget.setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
                widget.show();
            }
        }
    }

    private static @Nullable QWidget sideWidget(@Nullable Component component, QWidget host) {
        QWidget widget = component instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
        return widget != null && !widget.isDisposed() && widget.parentWidget() == host ? widget : null;
    }

    public void layoutSides(QWidget host) {
        boolean inplace = host instanceof QLineEdit lineEdit && !lineEdit.hasFrame();
        int inset = inplace ? INPLACE_INSET : host.style().pixelMetric(QStyle.PixelMetric.PM_DefaultFrameWidth, null, host);

        QWidget prefix = sideWidget(myPrefixComponent, host);
        QWidget suffix = sideWidget(mySuffixComponent, host);

        int prefixWidth = 0;
        if (prefix != null) {
            int left = inset;
            for (QObject child : host.children()) {
                if (child != prefix && child != suffix && child instanceof QToolButton sideButton && sideButton.isVisible() && sideButton.x() < host.width() / 2) {
                    left = Math.max(left, sideButton.x() + sideButton.width());
                }
            }

            QSize hint = prefix.sizeHint();
            int height = Math.max(0, Math.min(hint.height(), host.height() - inset * 2));
            prefixWidth = hint.width();

            prefix.setGeometry(left, (host.height() - height) / 2, prefixWidth, height);
        }

        int suffixWidth = 0;
        if (suffix != null) {
            int right = host.width() - inset;
            for (QObject child : host.children()) {
                if (child != suffix && child != prefix && child instanceof QToolButton sideButton && sideButton.isVisible() && sideButton.x() > host.width() / 2) {
                    right = Math.min(right, sideButton.x());
                }
            }

            QSize hint = suffix.sizeHint();
            int height = Math.max(0, Math.min(hint.height(), host.height() - inset * 2));
            suffixWidth = hint.width();

            suffix.setGeometry(right - suffixWidth, (host.height() - height) / 2, suffixWidth, height);
        }

        if (host instanceof QLineEdit lineEdit) {
            int leftMargin = inplace ? INPLACE_INSET + prefixWidth : prefixWidth;
            int rightMargin = inplace ? INPLACE_INSET + suffixWidth : suffixWidth;
            int verticalMargin = inplace ? INPLACE_INSET : 0;

            QMargins margins = lineEdit.textMargins();
            if (inplace && (margins.top() != verticalMargin || margins.bottom() != verticalMargin)) {
                lineEdit.setTextMargins(leftMargin, verticalMargin, rightMargin, verticalMargin);
            }
            else if (margins.left() != leftMargin || margins.right() != rightMargin) {
                lineEdit.setTextMargins(leftMargin, margins.top(), rightMargin, margins.bottom());
            }
        }
        else if (host instanceof DesktopQtSideMarginsHost marginsHost) {
            marginsHost.setSideMargins(prefixWidth, suffixWidth);
        }
    }
}
