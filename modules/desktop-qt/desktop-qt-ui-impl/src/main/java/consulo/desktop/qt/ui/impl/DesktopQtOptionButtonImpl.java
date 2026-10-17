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
import consulo.ui.MenuItem;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.PopupMenu;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import io.qt.widgets.QWidget;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class DesktopQtOptionButtonImpl extends DesktopQtButtonImpl implements OptionButton {
    private static final int ARROW_AREA_WIDTH = 20;

    private static final int FRAME_INSET = 2;

    private static final int SEPARATOR_INSET = 4;

    private static final int SEPARATOR_ALPHA = 70;

    private static final int TEXT_PADDING_SIDE = 14;

    private class OptionPushButton extends HighlightPushButton {
        private OptionPushButton(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            super.paintEvent(event);

            if (myOptions.isEmpty()) {
                return;
            }

            QPainter painter = new QPainter(this);
            try {
                QRect area = arrowArea(this);

                QColor separator = new QColor(palette().color(QPalette.ColorRole.ButtonText));
                separator.setAlpha(SEPARATOR_ALPHA);
                painter.setPen(separator);

                int separatorX = isRightToLeft() ? area.right() : area.left();
                painter.drawLine(separatorX, SEPARATOR_INSET, separatorX, height() - SEPARATOR_INSET - 1);

                QStyleOption option = new QStyleOption();
                option.initFrom(this);
                option.setRect(area);
                style().drawPrimitive(QStyle.PrimitiveElement.PE_IndicatorArrowDown, option, painter, this);
            }
            finally {
                painter.end();
            }
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            if (event.button() == Qt.MouseButton.LeftButton && !myOptions.isEmpty() && isEnabled()) {
                QRect area = arrowArea(this);
                int x = (int) event.position().x();
                if (x >= area.left() && x <= area.right()) {
                    event.accept();

                    setFocus();

                    showOptions();
                    return;
                }
            }

            super.mousePressEvent(event);
        }

        @Override
        protected void keyPressEvent(QKeyEvent event) {
            int key = event.key();
            boolean enter = key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value();
            Qt.KeyboardModifiers modifiers = event.modifiers();
            if (enter && modifiers.testFlag(Qt.KeyboardModifier.AltModifier) && modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier)) {
                event.accept();

                showOptions();
                return;
            }

            super.keyPressEvent(event);
        }
    }

    private List<OptionButtonItem> myOptions = List.of();

    public DesktopQtOptionButtonImpl(LocalizeValue text) {
        super(text);
    }

    private static QRect arrowArea(QWidget widget) {
        int x = widget.isRightToLeft() ? FRAME_INSET : widget.width() - FRAME_INSET - ARROW_AREA_WIDTH;
        return new QRect(x, 0, ARROW_AREA_WIDTH, widget.height());
    }

    @Override
    protected QPushButton createQt(QWidget parent) {
        return new OptionPushButton(parent);
    }

    @Override
    protected String textPadding() {
        if (myOptions.isEmpty()) {
            return super.textPadding();
        }

        int arrowSide = TEXT_PADDING_SIDE + ARROW_AREA_WIDTH;
        QPushButton component = myComponent;
        boolean rtl = component != null && component.isRightToLeft();
        int left = rtl ? arrowSide : TEXT_PADDING_SIDE;
        int right = rtl ? TEXT_PADDING_SIDE : arrowSide;
        return "2px " + right + "px 2px " + left + "px";
    }

    @Override
    public List<OptionButtonItem> getOptions() {
        return myOptions;
    }

    @RequiredUIAccess
    @Override
    public void setOptions(List<OptionButtonItem> options) {
        myOptions = List.copyOf(options);

        if (myComponent != null) {
            updateStyleSheet();

            myComponent.update();
        }
    }

    @RequiredUIAccess
    @Override
    public void showOptions() {
        QPushButton component = myComponent;
        if (myOptions.isEmpty() || component == null || component.isDisposed() || !component.isVisible()) {
            return;
        }

        PopupMenu menu = PopupMenu.create(this);
        for (OptionButtonItem option : myOptions) {
            MenuItem item = MenuItem.create(option.text());
            item.setIcon(option.icon());
            item.setEnabled(option.enabled());
            item.addClickListener(event -> option.clickListener().onEvent(new ClickEvent(this, event.getInputDetails())));
            menu.add(item);
        }

        menu.show(0, component.height());
    }
}
