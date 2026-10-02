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

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.InlineButton;
import consulo.ui.ex.internal.InlineButtonsList;
import consulo.ui.model.FlatDataModel;
import io.qt.core.QEvent;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QHelpEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import io.qt.widgets.QListWidget;
import io.qt.widgets.QListWidgetItem;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionViewItem;
import io.qt.widgets.QToolTip;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
class DesktopQtPopupListBoxImpl<E> extends DesktopQtListBoxImpl<E> implements InlineButtonsList<E> {
    private static final int ourButtonSize = 20;
    private static final int ourIconSize = 16;
    private static final int ourButtonGap = 2;
    private static final int ourRightInset = 4;
    private static final int ourSeparatorInset = 6;
    private static final int ourContentGap = 8;
    private static final int ourSeparatorMargin = 4;
    private static final int ourHighlightAlpha = 40;
    private static final double ourHighlightRadius = 4;

    private record ButtonHit(int row, int index) {
    }

    private class InlineButtonsDelegate extends DesktopQtTextItemDelegate {
        private boolean myEliding;

        InlineButtonsDelegate(QObject parent) {
            super(parent);
        }

        @Override
        protected void initStyleOption(QStyleOptionViewItem option, QModelIndex index) {
            super.initStyleOption(option, index);

            if (!myEliding) {
                return;
            }

            List<InlineButton> buttons = buttonsAt(index.row());
            if (!isStripVisible(option, buttons)) {
                return;
            }

            QWidget widget = option.widget();
            QStyle style = widget != null ? widget.style() : QApplication.style();

            QRect textRect = style.subElementRect(QStyle.SubElement.SE_ItemViewItemText, option, widget);
            int separatorX = buttonRect(option.rect(), buttons.size(), 0).left() - ourSeparatorInset;
            int available = Math.max(0, separatorX - ourContentGap - textRect.left());

            option.setText(option.fontMetrics().elidedText(option.text(), Qt.TextElideMode.ElideRight, available));
        }

        @Override
        public void paint(QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
            myEliding = true;
            try {
                super.paint(painter, option, index);
            }
            finally {
                myEliding = false;
            }

            paintButtons(painter, option, index);
        }

        @Override
        public QSize sizeHint(QStyleOptionViewItem option, QModelIndex index) {
            QSize size = super.sizeHint(option, index);

            int count = buttonsAt(index.row()).size();
            if (count == 0) {
                return size;
            }

            return new QSize(size.width() + stripWidth(count), Math.max(size.height(), ourButtonSize));
        }

        @Override
        protected int reservedTrailingWidth(QStyleOptionViewItem option, QModelIndex index) {
            List<InlineButton> buttons = buttonsAt(index.row());
            if (!isStripVisible(option, buttons)) {
                return 0;
            }

            QRect rowRect = option.rect();
            int separatorX = buttonRect(rowRect, buttons.size(), 0).left() - ourSeparatorInset;
            return Math.max(0, rowRect.right() - separatorX + ourContentGap);
        }

        private void paintButtons(QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
            int row = index.row();

            List<InlineButton> buttons = buttonsAt(row);
            if (!isStripVisible(option, buttons)) {
                return;
            }

            boolean selected = option.state().testFlag(QStyle.StateFlag.State_Selected);

            QRect rowRect = option.rect();
            int count = buttons.size();
            int separatorX = buttonRect(rowRect, count, 0).left() - ourSeparatorInset;

            painter.save();
            try {
                painter.setPen(option.palette().color(QPalette.ColorRole.Mid));
                painter.drawLine(separatorX, rowRect.top() + ourSeparatorMargin, separatorX, rowRect.bottom() - ourSeparatorMargin);

                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

                QColor text = option.palette().color(selected ? QPalette.ColorRole.HighlightedText : QPalette.ColorRole.Text);
                QColor highlight = new QColor(text.red(), text.green(), text.blue(), ourHighlightAlpha);

                for (int i = 0; i < count; i++) {
                    InlineButton button = buttons.get(i);
                    if (!selected && !button.alwaysVisible()) {
                        continue;
                    }

                    QRect buttonRect = buttonRect(rowRect, count, i);

                    if (isHighlighted(row, i, selected)) {
                        painter.setPen(Qt.PenStyle.NoPen);
                        painter.setBrush(highlight);
                        painter.drawRoundedRect(new QRectF(buttonRect), ourHighlightRadius, ourHighlightRadius);
                    }

                    QRect iconRect = new QRect(
                        buttonRect.left() + (buttonRect.width() - ourIconSize) / 2,
                        buttonRect.top() + (buttonRect.height() - ourIconSize) / 2,
                        ourIconSize,
                        ourIconSize
                    );

                    DesktopQtImage.toQIcon(button.icon()).paint(painter, iconRect);
                }
            }
            finally {
                painter.restore();
            }
        }
    }

    private class InlineButtonsFilter extends QObject {
        InlineButtonsFilter(QObject parent) {
            super(parent);
        }

        @Override
        public boolean eventFilter(QObject watched, QEvent event) {
            QEvent.Type type = event.type();

            if (type == QEvent.Type.MouseMove && event instanceof QMouseEvent mouse) {
                setHovered(buttonAt(mouse.position().toPoint()));
                return false;
            }

            if (type == QEvent.Type.Leave) {
                setHovered(null);
                return false;
            }

            if ((type == QEvent.Type.MouseButtonPress || type == QEvent.Type.MouseButtonDblClick) && event instanceof QMouseEvent mouse) {
                ButtonHit hit = buttonAt(mouse.position().toPoint());
                if (hit == null) {
                    return false;
                }

                myPressed = mouse.button() == Qt.MouseButton.LeftButton ? hit : null;
                return true;
            }

            if (type == QEvent.Type.MouseButtonRelease && event instanceof QMouseEvent mouse) {
                ButtonHit pressed = myPressed;
                if (pressed == null) {
                    return false;
                }

                myPressed = null;

                if (pressed.equals(buttonAt(mouse.position().toPoint()))) {
                    List<InlineButton> buttons = buttonsAt(pressed.row());
                    if (pressed.index() < buttons.size()) {
                        buttons.get(pressed.index()).action().accept(DesktopQtInputDetails.mouse(myComponent, mouse));
                    }
                }
                return true;
            }

            if (type == QEvent.Type.ToolTip && event instanceof QHelpEvent help) {
                ButtonHit hit = buttonAt(help.pos());
                if (hit == null) {
                    return false;
                }

                List<InlineButton> buttons = buttonsAt(hit.row());
                LocalizeValue toolTip = hit.index() < buttons.size() ? buttons.get(hit.index()).toolTip() : LocalizeValue.empty();
                if (toolTip.isEmpty()) {
                    QToolTip.hideText();
                }
                else {
                    QToolTip.showText(help.globalPos(), toolTip.get(), watched instanceof QWidget widget ? widget : null);
                }
                return true;
            }

            return false;
        }
    }

    private final Map<Integer, List<InlineButton>> myButtons = new HashMap<>();

    private @Nullable Function<E, List<InlineButton>> myInlineButtons;

    private @Nullable ButtonHit myHovered;
    private @Nullable ButtonHit myPressed;
    private int myActiveButton = -1;

    DesktopQtPopupListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected void initialize(QListWidget component) {
        super.initialize(component);

        QWidget viewport = component.viewport();
        viewport.installEventFilter(new InlineButtonsFilter(viewport));
    }

    @Override
    protected DesktopQtTextItemDelegate createItemDelegate(QListWidget component) {
        return new InlineButtonsDelegate(component);
    }

    @Override
    protected void rebuild(QListWidget component) {
        myButtons.clear();
        myHovered = null;
        myPressed = null;

        super.rebuild(component);
    }

    @Override
    protected void rowsChanged() {
        myButtons.clear();
        myHovered = null;
        myPressed = null;
    }

    @Override
    @RequiredUIAccess
    public void setInlineButtons(Function<E, List<InlineButton>> buttons) {
        myInlineButtons = buttons;

        QListWidget component = myComponent;
        if (component != null) {
            rebuild(component);
        }
        else {
            myButtons.clear();
        }
    }

    @Override
    @RequiredUIAccess
    public void setActiveInlineButton(int index) {
        if (myActiveButton == index) {
            return;
        }

        myActiveButton = index;

        updateViewport();
    }

    private List<InlineButton> buttonsAt(int row) {
        Function<E, List<InlineButton>> provider = myInlineButtons;
        FlatDataModel<E> model = getDataModel();
        if (provider == null || row < 0 || row >= model.getSize()) {
            return List.of();
        }

        return myButtons.computeIfAbsent(row, it -> provider.apply(model.get(it)));
    }

    private @Nullable ButtonHit buttonAt(QPoint point) {
        QListWidget component = myComponent;
        if (component == null || component.isDisposed()) {
            return null;
        }

        QModelIndex index = component.indexAt(point);
        if (!index.isValid()) {
            return null;
        }

        int row = index.row();
        List<InlineButton> buttons = buttonsAt(row);
        if (buttons.isEmpty()) {
            return null;
        }

        QListWidgetItem item = component.item(row);
        boolean selected = item != null && item.isSelected();

        QRect rowRect = component.visualRect(index);
        for (int i = 0; i < buttons.size(); i++) {
            if (!selected && !buttons.get(i).alwaysVisible()) {
                continue;
            }

            if (buttonRect(rowRect, buttons.size(), i).contains(point)) {
                return new ButtonHit(row, i);
            }
        }
        return null;
    }

    private static boolean isStripVisible(QStyleOptionViewItem option, List<InlineButton> buttons) {
        if (buttons.isEmpty()) {
            return false;
        }

        return option.state().testFlag(QStyle.StateFlag.State_Selected) || buttons.stream().anyMatch(InlineButton::alwaysVisible);
    }

    private boolean isHighlighted(int row, int index, boolean selected) {
        ButtonHit hovered = myHovered;
        if (hovered != null && hovered.row() == row && hovered.index() == index) {
            return true;
        }

        return selected && index == myActiveButton;
    }

    private void setHovered(@Nullable ButtonHit hovered) {
        if (Objects.equals(myHovered, hovered)) {
            return;
        }

        myHovered = hovered;

        updateViewport();
    }

    private void updateViewport() {
        QListWidget component = myComponent;
        if (component != null && !component.isDisposed()) {
            component.viewport().update();
        }
    }

    private static QRect buttonRect(QRect rowRect, int count, int index) {
        int buttonsWidth = count * ourButtonSize + (count - 1) * ourButtonGap;
        int left = rowRect.right() - ourRightInset - buttonsWidth + 1 + index * (ourButtonSize + ourButtonGap);
        int top = rowRect.top() + (rowRect.height() - ourButtonSize) / 2;
        return new QRect(left, top, ourButtonSize, ourButtonSize);
    }

    private static int stripWidth(int count) {
        return ourContentGap + 1 + ourSeparatorInset + count * ourButtonSize + (count - 1) * ourButtonGap + ourRightInset;
    }
}
