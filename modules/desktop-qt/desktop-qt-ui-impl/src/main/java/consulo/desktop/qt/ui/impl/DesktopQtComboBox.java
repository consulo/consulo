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
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QResizeEvent;
import io.qt.gui.QPalette;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QStylePainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtComboBox extends QComboBox {
    private static final int MINIMUM_VISIBLE_CHARS = 8;
    private static final int MAXIMUM_VISIBLE_CHARS = 32;
    private static final int ICON_TEXT_GAP = 4;

    private @Nullable QWidget mySuffix;

    public DesktopQtComboBox(QWidget parent) {
        super(parent);
    }

    public void setSuffixWidget(@Nullable QWidget suffix) {
        mySuffix = suffix;
        layoutSuffix();
        updateGeometry();
        update();
    }

    private int suffixWidth() {
        QWidget suffix = mySuffix;
        return suffix == null || suffix.isDisposed() ? 0 : suffix.sizeHint().width();
    }

    private void layoutSuffix() {
        QWidget suffix = mySuffix;
        if (suffix == null || suffix.isDisposed()) {
            return;
        }

        QStyleOptionComboBox option = new QStyleOptionComboBox();
        super.initStyleOption(option);
        QRect arrow = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxArrow, this);

        QSize hint = suffix.sizeHint();
        int height = Math.max(0, Math.min(hint.height(), height()));
        suffix.setGeometry(arrow.left() - hint.width(), (height() - height) / 2, hint.width(), height);
        suffix.raise();
        suffix.show();
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);
        layoutSuffix();
    }

    @Override
    public QSize sizeHint() {
        QSize hint = super.sizeHint();
        return new QSize(hint.width() + suffixWidth(), hint.height());
    }

    @Override
    public QSize minimumSizeHint() {
        QSize hint = super.minimumSizeHint();

        QStyleOptionComboBox option = new QStyleOptionComboBox();
        initStyleOption(option);

        int contentWidth = minimumContentWidth();
        QSize minimum = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(contentWidth, hint.height()), this);
        return new QSize(Math.min(hint.width(), minimum.width()) + suffixWidth(), hint.height());
    }

    @Override
    protected void initStyleOption(QStyleOptionComboBox option) {
        super.initStyleOption(option);

        decorateStyleOption(option);

        elideLabel(option);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QStylePainter painter = new QStylePainter(this);
        try {
            painter.setPen(palette().color(QPalette.ColorRole.Text));

            QStyleOptionComboBox option = new QStyleOptionComboBox();
            initStyleOption(option);
            painter.drawComplexControl(QStyle.ComplexControl.CC_ComboBox, option);

            String placeholder = labelPlaceholder();
            if (!placeholder.isEmpty()) {
                QPalette palette = option.palette();
                palette.setBrush(QPalette.ColorRole.ButtonText, palette.placeholderText());
                option.setPalette(palette);
                option.setCurrentText(placeholder);
                elideLabel(option);
            }

            painter.drawControl(QStyle.ControlElement.CE_ComboBoxLabel, option);
        }
        finally {
            painter.end();
        }
    }

    protected int minimumContentWidth() {
        return fontMetrics().horizontalAdvance("x".repeat(MINIMUM_VISIBLE_CHARS)) + iconSize().width();
    }

    protected void decorateStyleOption(QStyleOptionComboBox option) {
    }

    protected String labelPlaceholder() {
        return currentIndex() < 0 ? placeholderText() : "";
    }

    protected final int adaptiveLabelWidth(String text) {
        QFontMetrics metrics = fontMetrics();
        int textWidth = Math.max(metrics.boundingRect(text).width(), metrics.horizontalAdvance(text));
        return labelWidth(Math.min(textWidth, metrics.averageCharWidth() * MAXIMUM_VISIBLE_CHARS));
    }

    private int labelWidth(int textWidth) {
        QStyleOptionComboBox option = new QStyleOptionComboBox();
        initStyleOption(option);

        QSize size = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(textWidth, fontMetrics().height()), this);

        option.setRect(new QRect(0, 0, size.width(), size.height()));
        QRect textRect = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxEditField, this);
        return size.width() + Math.max(0, textWidth - textRect.width());
    }

    private void elideLabel(QStyleOptionComboBox option) {
        QRect textRect = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxEditField, this);
        int available = textRect.width() - suffixWidth() - (option.currentIcon().isNull() ? 0 : iconSize().width() + ICON_TEXT_GAP);
        if (available > 0) {
            option.setCurrentText(fontMetrics().elidedText(option.currentText(), Qt.TextElideMode.ElideRight, available));
        }
    }
}
