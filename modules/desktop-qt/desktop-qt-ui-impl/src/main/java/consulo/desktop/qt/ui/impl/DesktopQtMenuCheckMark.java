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

import consulo.desktop.qt.ui.impl.image.DesktopQtEmptyImageImpl;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.gui.QColor;
import io.qt.gui.QIcon;
import io.qt.gui.QIconEngine;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPixmap;
import io.qt.widgets.QApplication;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionMenuItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopQtMenuCheckMark extends QIconEngine {
    private static final QPalette.ColorRole[] CHECK_ROLES = {
        QPalette.ColorRole.WindowText,
        QPalette.ColorRole.Text,
        QPalette.ColorRole.ButtonText,
        QPalette.ColorRole.HighlightedText
    };

    public static QIcon create(@Nullable QWidget widget) {
        return new QIcon(new DesktopQtMenuCheckMark(widget));
    }

    private final @Nullable QWidget myWidget;

    private DesktopQtMenuCheckMark(@Nullable QWidget widget) {
        myWidget = widget;
    }

    @Override
    public void paint(QPainter painter, QRect rect, QIcon.Mode mode, QIcon.State state) {
        QWidget widget = myWidget != null && !myWidget.isDisposed() ? myWidget : null;
        QStyle style = widget != null ? widget.style() : QApplication.style();

        boolean selected = mode == QIcon.Mode.Selected || mode == QIcon.Mode.Active;

        QStyleOptionMenuItem option = new QStyleOptionMenuItem();
        option.setRect(rect);
        option.setPalette(checkPalette(widget != null ? widget.palette() : QApplication.palette(), mode, selected));
        option.setMenuItemType(QStyleOptionMenuItem.MenuItemType.Normal);
        option.setCheckType(QStyleOptionMenuItem.CheckType.NonExclusive);
        option.setChecked(true);

        QStyle.State optionState = option.state();
        optionState.setFlag(QStyle.StateFlag.State_On, true);
        optionState.setFlag(QStyle.StateFlag.State_Enabled, mode != QIcon.Mode.Disabled);
        optionState.setFlag(QStyle.StateFlag.State_Selected, selected);
        option.setState(optionState);

        style.drawPrimitive(QStyle.PrimitiveElement.PE_IndicatorMenuCheckMark, option, painter, widget);
    }

    private static QPalette checkPalette(QPalette source, QIcon.Mode mode, boolean selected) {
        if (mode == QIcon.Mode.Disabled) {
            return source;
        }

        QPalette palette = new QPalette(source);
        QColor color = selected ? source.color(QPalette.ColorRole.HighlightedText) : source.color(QPalette.ColorRole.Accent);
        for (QPalette.ColorRole role : CHECK_ROLES) {
            palette.setColor(QPalette.ColorGroup.Active, role, color);
            palette.setColor(QPalette.ColorGroup.Inactive, role, color);
        }
        return palette;
    }

    @Override
    public QSize actualSize(QSize size, QIcon.Mode mode, QIcon.State state) {
        return size;
    }

    @Override
    public QPixmap pixmap(QSize size, QIcon.Mode mode, QIcon.State state) {
        return scaledPixmap(size, mode, state, 1);
    }

    @Override
    public QPixmap scaledPixmap(QSize size, QIcon.Mode mode, QIcon.State state, double scale) {
        QPixmap pixmap = DesktopQtEmptyImageImpl.createPixmap(size.width(), size.height(), scale > 0 ? scale : 1);
        if (size.isEmpty()) {
            return pixmap;
        }

        QPainter painter = new QPainter(pixmap);
        try {
            paint(painter, new QRect(0, 0, size.width(), size.height()), mode, state);
        }
        finally {
            painter.end();
        }
        return pixmap;
    }

    @Override
    public QIconEngine clone() {
        return new DesktopQtMenuCheckMark(myWidget);
    }
}
