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

import consulo.ui.Separator;
import consulo.ui.SeparatorStyle;
import io.qt.core.QRect;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QFrame;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionFrame;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtSeparatorImpl extends QtComponentDelegate<QFrame> implements Separator {
    private static final int LINE_INSET = 4;

    private static final class Line extends QFrame {
        private Line(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QStyleOptionFrame option = new QStyleOptionFrame();
            initStyleOption(option);

            QRect rect = option.rect();
            option.setRect(frameShape() == QFrame.Shape.VLine
                ? rect.adjusted(0, LINE_INSET, 0, -LINE_INSET)
                : rect.adjusted(LINE_INSET, 0, -LINE_INSET, 0));

            QPainter painter = new QPainter(this);
            try {
                style().drawControl(QStyle.ControlElement.CE_ShapedFrame, option, painter, this);
            }
            finally {
                painter.end();
            }
        }
    }

    private final SeparatorStyle myStyle;

    public DesktopQtSeparatorImpl(SeparatorStyle style) {
        myStyle = style;
    }

    @Override
    protected QFrame createQt(QWidget parent) {
        return new Line(parent);
    }

    @Override
    protected void initialize(QFrame component) {
        component.setFrameShadow(QFrame.Shadow.Plain);

        if (myStyle == SeparatorStyle.VERTICAL) {
            component.setFrameShape(QFrame.Shape.VLine);
            component.setFixedWidth(1);
            component.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Preferred);
        }
        else {
            component.setFrameShape(QFrame.Shape.HLine);
            component.setFixedHeight(1);
            component.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed);
        }
    }

    @Override
    public SeparatorStyle getSeparatorStyle() {
        return myStyle;
    }
}
