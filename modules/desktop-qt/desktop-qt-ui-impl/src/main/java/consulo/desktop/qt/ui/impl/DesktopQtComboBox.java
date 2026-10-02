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

import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtComboBox extends QComboBox {
    private static final int MINIMUM_VISIBLE_CHARS = 8;
    private static final int ICON_TEXT_GAP = 4;

    public DesktopQtComboBox(QWidget parent) {
        super(parent);
    }

    @Override
    public QSize minimumSizeHint() {
        QSize hint = super.minimumSizeHint();

        QStyleOptionComboBox option = new QStyleOptionComboBox();
        initStyleOption(option);

        int contentWidth = fontMetrics().horizontalAdvance("x".repeat(MINIMUM_VISIBLE_CHARS)) + iconSize().width();
        QSize minimum = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(contentWidth, hint.height()), this);
        return new QSize(Math.min(hint.width(), minimum.width()), hint.height());
    }

    @Override
    protected void initStyleOption(QStyleOptionComboBox option) {
        super.initStyleOption(option);

        decorateStyleOption(option);

        QRect textRect = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxEditField, this);
        int available = textRect.width() - (option.currentIcon().isNull() ? 0 : iconSize().width() + ICON_TEXT_GAP);
        if (available > 0) {
            option.setCurrentText(fontMetrics().elidedText(option.currentText(), Qt.TextElideMode.ElideRight, available));
        }
    }

    protected void decorateStyleOption(QStyleOptionComboBox option) {
    }
}
