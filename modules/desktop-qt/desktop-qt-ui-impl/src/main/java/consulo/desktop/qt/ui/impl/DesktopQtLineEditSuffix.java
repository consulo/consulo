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
public final class DesktopQtLineEditSuffix {
    private final QtComponentDelegate<?> myOwner;

    private @Nullable Component mySuffixComponent;

    public DesktopQtLineEditSuffix(QtComponentDelegate<?> owner) {
        myOwner = owner;
    }

    public QLineEdit createLineEdit(QWidget parent) {
        return new QLineEdit(parent) {
            @Override
            public boolean event(QEvent event) {
                boolean result = super.event(event);

                QEvent.Type type = event.type();
                if (type == QEvent.Type.Resize || type == QEvent.Type.LayoutRequest || type == QEvent.Type.StyleChange) {
                    layoutSuffix(this);
                }
                return result;
            }
        };
    }

    public @Nullable Component get() {
        return mySuffixComponent;
    }

    public void set(@Nullable Component suffixComponent, @Nullable QLineEdit lineEdit) {
        Component oldSuffix = mySuffixComponent;
        if (oldSuffix == suffixComponent) {
            return;
        }

        if (oldSuffix instanceof QtComponentDelegate<?> oldDelegate) {
            oldDelegate.setParent(null);
        }

        mySuffixComponent = suffixComponent;

        if (lineEdit != null && !lineEdit.isDisposed()) {
            attach(lineEdit);
        }
    }

    public void attach(QLineEdit lineEdit) {
        if (mySuffixComponent instanceof QtComponentDelegate<?> suffix) {
            suffix.setParent(myOwner);
            suffix.bind(lineEdit, null);

            QWidget widget = suffix.toQtComponent();
            if (widget != null) {
                widget.setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
                widget.show();
            }
        }

        layoutSuffix(lineEdit);
    }

    private void layoutSuffix(QLineEdit lineEdit) {
        QWidget suffix = mySuffixComponent instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;

        int suffixWidth = 0;
        if (suffix != null && !suffix.isDisposed() && suffix.parentWidget() == lineEdit) {
            int frameWidth = lineEdit.style().pixelMetric(QStyle.PixelMetric.PM_DefaultFrameWidth, null, lineEdit);

            int right = lineEdit.width() - frameWidth;
            for (QObject child : lineEdit.children()) {
                if (child != suffix && child instanceof QToolButton sideButton && sideButton.isVisible() && sideButton.x() > lineEdit.width() / 2) {
                    right = Math.min(right, sideButton.x());
                }
            }

            QSize hint = suffix.sizeHint();
            int height = Math.max(0, Math.min(hint.height(), lineEdit.height() - frameWidth * 2));
            suffixWidth = hint.width();

            suffix.setGeometry(right - suffixWidth, (lineEdit.height() - height) / 2, suffixWidth, height);
        }

        QMargins margins = lineEdit.textMargins();
        if (margins.right() != suffixWidth) {
            lineEdit.setTextMargins(margins.left(), margins.top(), suffixWidth, margins.bottom());
        }
    }
}
