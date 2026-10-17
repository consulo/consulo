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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.MemoryIndicator;
import consulo.ui.style.ComponentColors;
import io.qt.core.QEvent;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class DesktopQtMemoryIndicatorImpl extends QtComponentDelegate<QWidget> implements MemoryIndicator {
    private static final int HORIZONTAL_PADDING = 6;
    private static final int VERTICAL_PADDING = 2;
    private static final int ARC = 4;

    private class IndicatorWidget extends QWidget {
        private boolean myHovered;

        IndicatorWidget(QWidget parent) {
            super(parent);

            setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);
        }

        @Override
        public QSize sizeHint() {
            QFontMetrics metrics = fontMetrics();
            int width = metrics.horizontalAdvance(myWidestText.get()) + 2 * HORIZONTAL_PADDING;
            return new QSize(width, metrics.height() + 2 * VERTICAL_PADDING);
        }

        @Override
        public QSize minimumSizeHint() {
            return sizeHint();
        }

        @Override
        protected void enterEvent(QEnterEvent event) {
            super.enterEvent(event);
            myHovered = true;
            update();
        }

        @Override
        protected void leaveEvent(QEvent event) {
            super.leaveEvent(event);
            myHovered = false;
            update();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QRect rect = rect();
            if (rect.width() <= 1 || rect.height() <= 1) {
                return;
            }

            QPalette palette = palette();
            QColor midlight = palette.color(QPalette.ColorRole.Midlight);
            QColor mid = palette.color(QPalette.ColorRole.Mid);
            QColor hover = DesktopQtStyleApplier.themeColor(ComponentColors.HOVER_BACKGROUND, midlight);
            QColor allocated = DesktopQtStyleApplier.themeColor(ComponentColors.MEMORY_INDICATOR_ALLOCATED_BACKGROUND, midlight);
            QColor used = DesktopQtStyleApplier.themeColor(ComponentColors.MEMORY_INDICATOR_USED_BACKGROUND, mid);
            QColor border = DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, mid);

            QPainterPath shape = new QPainterPath();
            shape.addRoundedRect(new QRectF(0.5, 0.5, rect.width() - 1, rect.height() - 1), ARC, ARC);

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                painter.setPen(Qt.PenStyle.NoPen);

                if (myHovered) {
                    painter.fillPath(shape, new QBrush(hover));
                }

                if (myMax > 0) {
                    painter.fillPath(part(shape, rect, myAllocated), new QBrush(allocated));
                    painter.fillPath(part(shape, rect, myUsed), new QBrush(used));
                }

                painter.setBrush(Qt.BrushStyle.NoBrush);
                painter.setPen(new QPen(border, 1));
                painter.drawPath(shape);

                QPalette.ColorGroup group = isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled;
                painter.setPen(palette.color(group, QPalette.ColorRole.WindowText));
                painter.drawText(rect, Qt.AlignmentFlag.AlignCenter.value(), myText.get());
            }
            finally {
                painter.end();
            }
        }

        private QPainterPath part(QPainterPath shape, QRect rect, long value) {
            QPainterPath part = new QPainterPath();
            part.addRect(new QRectF(0, 0, rect.width() * (double) Math.min(value, myMax) / myMax, rect.height()));
            return shape.intersected(part);
        }
    }

    private final LocalizeValue myWidestText;

    private LocalizeValue myText = LocalizeValue.empty();
    private long myUsed;
    private long myAllocated;
    private long myMax;

    public DesktopQtMemoryIndicatorImpl(LocalizeValue widestText) {
        myWidestText = widestText;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        return new IndicatorWidget(parent);
    }

    @RequiredUIAccess
    @Override
    public void setUsage(long used, long allocated, long max, LocalizeValue text) {
        myUsed = used;
        myAllocated = allocated;
        myMax = max;
        myText = text;

        if (myComponent != null) {
            myComponent.update();
        }
    }
}
