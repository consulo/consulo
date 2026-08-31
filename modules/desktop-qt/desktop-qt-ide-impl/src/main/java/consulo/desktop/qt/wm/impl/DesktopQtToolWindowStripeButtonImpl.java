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
package consulo.desktop.qt.wm.impl;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import io.qt.core.QEvent;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowStripeButtonImpl
    extends QtComponentDelegate<DesktopQtToolWindowStripeButtonImpl.StripeButton> implements ToolWindowStripeButton {
    public class StripeButton extends QWidget {
        private String myText;

        private boolean myHovered;

        public StripeButton(QWidget parent) {
            super(parent);

            myText = myInternalDecorator.getWindowInfo().getId();

            setAttribute(Qt.WidgetAttribute.WA_Hover, true);
        }

        @Override
        protected void enterEvent(QEnterEvent event) {
            myHovered = true;
            update();
        }

        @Override
        protected void leaveEvent(QEvent event) {
            myHovered = false;
            update();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter painter = new QPainter(this);
            try {
                QSize size = size();

                if (isVertical()) {
                    painter.rotate(-90);

                    painter.setBrush(new QColor(0, 255, 0));
                    painter.drawRect(0, 0, size.width(), size.height());

                    painter.drawText(0, 0, myText);
                }
                else {
                    if (myHovered) {
                        painter.setBrush(new QColor(55, 55, 55, 85));
                        painter.drawRect(0, 0, size.width(), size.height());
                    }

                    painter.drawText(0, 0, myText);
                }
            }
            finally {
                painter.end();
            }
        }

        @Override
        public QSize sizeHint() {
            QWidget parent = parentWidget();

            QSize parentSize = parent == null ? size() : parent.size();

            int textLength = Math.max(myText.length() * 2, new QFontMetrics(font()).horizontalAdvance(myText));

            if (isVertical()) {
                return new QSize(parentSize.width(), textLength);
            }
            else {
                return new QSize(textLength, parentSize.height());
            }
        }
    }

    private final DesktopQtToolWindowInternalDecorator myInternalDecorator;

    public DesktopQtToolWindowStripeButtonImpl(
        DesktopQtToolWindowInternalDecorator internalDecorator,
        DesktopQtToolWindowPanelImpl toolWindowPanel
    ) {
        myInternalDecorator = internalDecorator;
    }

    @Override
    protected StripeButton createQt(QWidget parent) {
        return new StripeButton(parent);
    }

    private boolean isVertical() {
        ToolWindowAnchor anchor = myInternalDecorator.getWindowInfo().getAnchor();
        return anchor == ToolWindowAnchor.LEFT || anchor == ToolWindowAnchor.RIGHT;
    }

    @Override
    protected void initialize(StripeButton component) {
        super.initialize(component);
    }

    @Override
    public WindowInfo getWindowInfo() {
        return myInternalDecorator.getWindowInfo();
    }

    @Override
    public void apply(WindowInfo windowInfo) {
    }

    @RequiredUIAccess
    @Override
    public void updatePresentation() {
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void dispose() {
    }
}
