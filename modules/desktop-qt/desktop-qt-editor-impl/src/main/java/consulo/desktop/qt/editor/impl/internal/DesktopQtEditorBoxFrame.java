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
package consulo.desktop.qt.editor.impl.internal;

import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionFrame;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

final class DesktopQtEditorBoxFrame extends QFrame {
    private static final int HORIZONTAL_MARGIN = 2;
    private static final int VERTICAL_MARGIN = 1;

    private final QHBoxLayout myLayout;
    private final QObject myFocusFilter;
    private final QLineEdit myMetricsLineEdit;

    private boolean myOneLine;

    private @Nullable Runnable myPaletteListener;

    DesktopQtEditorBoxFrame(QWidget parent) {
        super(parent);
        setFrameShape(QFrame.Shape.NoFrame);

        myMetricsLineEdit = new QLineEdit(this);
        myMetricsLineEdit.setVisible(false);

        myLayout = new QHBoxLayout(this);
        myLayout.setSpacing(0);

        myFocusFilter = new QObject(this) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (type == QEvent.Type.FocusIn || type == QEvent.Type.FocusOut) {
                    update();
                }
                return false;
            }
        };

        updateMargins();
    }

    QHBoxLayout getBoxLayout() {
        return myLayout;
    }

    void setOneLine(boolean oneLine) {
        myOneLine = oneLine;
        if (oneLine) {
            setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Fixed);
        }
        updateMargins();
        updateGeometry();
    }

    void watchFocus(QWidget widget) {
        widget.installEventFilter(myFocusFilter);
    }

    void setPaletteListener(@Nullable Runnable listener) {
        myPaletteListener = listener;
    }

    QColor getBaseColor() {
        return palette().color(QPalette.ColorRole.Base);
    }

    private QStyleOptionFrame createStyleOption() {
        QStyleOptionFrame option = new QStyleOptionFrame();
        option.initFrom(this);
        option.setLineWidth(style().pixelMetric(QStyle.PixelMetric.PM_DefaultFrameWidth, option, myMetricsLineEdit));
        option.setMidLineWidth(0);
        option.setState(option.state().combined(QStyle.StateFlag.State_Sunken));
        return option;
    }

    private void updateMargins() {
        QStyleOptionFrame option = createStyleOption();
        QRect bounds = new QRect(0, 0, 200, 40);
        option.setRect(bounds);

        QRect contents = style().subElementRect(QStyle.SubElement.SE_LineEditContents, option, myMetricsLineEdit);

        myLayout.setContentsMargins(
            contents.left() - bounds.left() + HORIZONTAL_MARGIN,
            myOneLine ? 0 : contents.top() - bounds.top() + VERTICAL_MARGIN,
            bounds.right() - contents.right() + HORIZONTAL_MARGIN,
            myOneLine ? 0 : bounds.bottom() - contents.bottom() + VERTICAL_MARGIN
        );
    }

    private int getLineEditHeight() {
        return myMetricsLineEdit.sizeHint().height();
    }

    @Override
    public QSize sizeHint() {
        QSize hint = super.sizeHint();
        if (!myOneLine) {
            return hint;
        }
        return new QSize(hint.width(), getLineEditHeight());
    }

    @Override
    public QSize minimumSizeHint() {
        QSize hint = super.minimumSizeHint();
        if (!myOneLine) {
            return hint;
        }
        return new QSize(hint.width(), getLineEditHeight());
    }

    @Override
    protected void changeEvent(QEvent event) {
        super.changeEvent(event);

        QEvent.Type type = event.type();
        if (type == QEvent.Type.StyleChange) {
            updateMargins();
        }

        if (type == QEvent.Type.StyleChange || type == QEvent.Type.PaletteChange) {
            Runnable listener = myPaletteListener;
            if (listener != null) {
                listener.run();
            }
        }
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            style().drawPrimitive(QStyle.PrimitiveElement.PE_PanelLineEdit, createStyleOption(), painter, this);
        }
        finally {
            painter.end();
        }
    }
}
