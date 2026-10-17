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

import consulo.desktop.qt.ui.impl.font.DesktopQtFontImpl;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QFontMetricsF;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import io.qt.widgets.QListWidgetItem;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionViewItem;
import io.qt.widgets.QStyledItemDelegate;
import io.qt.widgets.QTableWidgetItem;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class DesktopQtTextItemDelegate extends QStyledItemDelegate {
    public static final int PRESENTATION_ROLE = Qt.ItemDataRole.UserRole + 1;
    public static final int EDITING_ROLE = Qt.ItemDataRole.UserRole + 2;

    private static final int SUFFIX_GAP = 16;
    private static final int SUFFIX_ICON_GAP = 4;
    private static final int TRAILING_PADDING = 6;

    public DesktopQtTextItemDelegate(QObject parent) {
        super(parent);
    }

    public static void bind(QListWidgetItem item, DesktopQtTextItemPresentation presentation) {
        item.setData(PRESENTATION_ROLE, presentation);
        item.setToolTip(presentation.getTooltip().get());
    }

    public static void bind(QTreeWidgetItem item, int column, DesktopQtTextItemPresentation presentation) {
        item.setData(column, PRESENTATION_ROLE, presentation);
        item.setToolTip(column, presentation.getTooltip().get());
    }

    public static void bind(QTableWidgetItem item, DesktopQtTextItemPresentation presentation) {
        item.setData(PRESENTATION_ROLE, presentation);
        item.setToolTip(presentation.getTooltip().get());
    }

    public static QBrush toBrush(@Nullable ColorValue color) {
        return color == null ? new QBrush() : new QBrush(TargetQt.to(color));
    }

    protected int reservedTrailingWidth(QStyleOptionViewItem option, QModelIndex index) {
        return 0;
    }

    public static @Nullable QFont toQFont(@Nullable Font font) {
        return font instanceof DesktopQtFontImpl qtFont ? qtFont.toQFont() : null;
    }

    @Override
    protected void initStyleOption(QStyleOptionViewItem option, QModelIndex index) {
        super.initStyleOption(option, index);

        DesktopQtTextItemPresentation presentation = presentationOf(index);
        if (presentation == null) {
            return;
        }

        QFont font = toQFont(presentation.getFont());
        if (font != null) {
            option.setFont(font);
            option.setFontMetrics(new QFontMetrics(font));
        }

        Image image = presentation.getImage();
        if (image == null || !option.features().testFlag(QStyleOptionViewItem.ViewItemFeature.HasDecoration)) {
            return;
        }

        QSize decorationSize = option.decorationSize();
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > decorationSize.width() || height > decorationSize.height()) {
            option.setDecorationSize(new QSize(Math.max(width, decorationSize.width()), Math.max(height, decorationSize.height())));
        }
    }

    @Override
    public void updateEditorGeometry(@Nullable QWidget editor, QStyleOptionViewItem option, QModelIndex index) {
        if (editor == null || !isEditingRow(index)) {
            super.updateEditorGeometry(editor, option, index);
            return;
        }

        QStyleOptionViewItem styleOption = new QStyleOptionViewItem(option);
        initStyleOption(styleOption, index);

        QWidget widget = styleOption.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();

        QRect rect = option.rect();
        int left = rect.left();
        if (styleOption.features().testFlag(QStyleOptionViewItem.ViewItemFeature.HasCheckIndicator)) {
            QRect check = style.subElementRect(QStyle.SubElement.SE_ItemViewItemCheckIndicator, styleOption, widget);
            left = check.right() + 1 + style.pixelMetric(QStyle.PixelMetric.PM_FocusFrameHMargin, styleOption, widget) + 1;
        }

        editor.setGeometry(new QRect(left, rect.top(), Math.max(0, rect.right() + 1 - left), rect.height()));
    }

    private void paintEditing(QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        QStyleOptionViewItem styleOption = new QStyleOptionViewItem(option);
        initStyleOption(styleOption, index);

        QStyleOptionViewItem.ViewItemFeatures features = styleOption.features();
        features.setFlag(QStyleOptionViewItem.ViewItemFeature.HasDecoration, false);
        features.setFlag(QStyleOptionViewItem.ViewItemFeature.HasDisplay, false);
        styleOption.setFeatures(features);
        styleOption.setText("");

        QWidget widget = styleOption.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();
        style.drawControl(QStyle.ControlElement.CE_ItemViewItem, styleOption, painter, widget);
    }

    protected static boolean isEditingRow(QModelIndex index) {
        return index.isValid() && Boolean.TRUE.equals(index.data(EDITING_ROLE));
    }

    @Override
    public void paint(@Nullable QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        if (painter != null && isEditingRow(index)) {
            paintEditing(painter, option, index);
            return;
        }

        DesktopQtTextItemPresentation presentation = presentationOf(index);
        if (painter == null || presentation == null || !isStyled(presentation)) {
            super.paint(painter, option, index);
            return;
        }

        QStyleOptionViewItem styleOption = new QStyleOptionViewItem(option);
        initStyleOption(styleOption, index);

        QWidget widget = styleOption.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();

        QRect textRect = style.subElementRect(QStyle.SubElement.SE_ItemViewItemText, styleOption, widget);
        int left = textRect.left() + leadingPadding(style, styleOption, widget, presentation);
        int right = textRect.right() - reservedTrailingWidth(option, index) - TRAILING_PADDING;

        styleOption.setText("");
        style.drawControl(QStyle.ControlElement.CE_ItemViewItem, styleOption, painter, widget);

        painter.save();
        try {
            paintPresentation(painter, styleOption, presentation, left, right, textRect.top(), textRect.height());
        }
        finally {
            painter.restore();
        }
    }

    @Override
    public QSize sizeHint(QStyleOptionViewItem option, QModelIndex index) {
        QSize size = super.sizeHint(option, index);

        DesktopQtTextItemPresentation presentation = presentationOf(index);
        if (presentation == null || !isStyled(presentation)) {
            return size;
        }

        QFont presentationFont = toQFont(presentation.getFont());
        QFont font = presentationFont == null ? option.font() : presentationFont;
        int plainWidth = new QFontMetrics(font).horizontalAdvance(presentation.toString());

        int styledWidth = fragmentsWidth(font, presentation);
        if (presentation.hasSuffix()) {
            styledWidth += SUFFIX_GAP + suffixWidth(font, presentation);
        }

        QWidget widget = option.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();
        int leading = leadingPadding(style, option, widget, presentation);

        return new QSize(size.width() + Math.max(0, styledWidth - plainWidth) + leading + TRAILING_PADDING, size.height());
    }

    private static int leadingPadding(
        QStyle style,
        QStyleOptionViewItem option,
        @Nullable QWidget widget,
        DesktopQtTextItemPresentation presentation
    ) {
        if (presentation.getImage() != null) {
            return 0;
        }

        int styleMargin = style.pixelMetric(QStyle.PixelMetric.PM_FocusFrameHMargin, option, widget) + 1;
        return Math.max(0, TRAILING_PADDING - styleMargin);
    }

    private static @Nullable DesktopQtTextItemPresentation presentationOf(QModelIndex index) {
        if (!index.isValid()) {
            return null;
        }

        return index.data(PRESENTATION_ROLE) instanceof DesktopQtTextItemPresentation presentation ? presentation : null;
    }

    private static boolean isStyled(DesktopQtTextItemPresentation presentation) {
        if (presentation.hasSuffix() || presentation.getImage() == null) {
            return true;
        }

        for (DesktopQtTextFragment fragment : presentation.getFragments()) {
            TextAttribute attribute = fragment.attribute();
            if (attribute.getForegroundColor() != null
                || attribute.getBackgroundColor() != null
                || TextAttributeUtil.getFontStyle(attribute) != Font.PLAIN
                || !TextAttributeUtil.getEffects(attribute).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void paintPresentation(
        QPainter painter,
        QStyleOptionViewItem option,
        DesktopQtTextItemPresentation presentation,
        int left,
        int right,
        int top,
        int height
    ) {
        QStyle.State state = option.state();
        boolean selected = state.testFlag(QStyle.StateFlag.State_Selected);

        QPalette.ColorGroup group = !state.testFlag(QStyle.StateFlag.State_Enabled)
            ? QPalette.ColorGroup.Disabled
            : state.testFlag(QStyle.StateFlag.State_Active) ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Inactive;

        QColor defaultColor = option.palette().color(group, selected ? QPalette.ColorRole.HighlightedText : QPalette.ColorRole.Text);

        QFont baseFont = option.font();

        int fragmentsRight = right;
        if (presentation.hasSuffix()) {
            int suffixLeft = Math.max(left, right + 1 - suffixWidth(baseFont, presentation));
            QColor suffixColor = selected ? defaultColor : TargetQt.to(ComponentColors.DISABLED_TEXT);
            paintSuffix(painter, presentation, baseFont, suffixColor, suffixLeft, right, top, height);
            fragmentsRight = suffixLeft - SUFFIX_GAP;
        }

        int x = left;

        Qt.Alignment alignment = option.displayAlignment();
        boolean alignRight = alignment.testFlag(Qt.AlignmentFlag.AlignRight);
        if (alignRight || alignment.testFlag(Qt.AlignmentFlag.AlignHCenter)) {
            int free = fragmentsRight - left + 1 - fragmentsWidth(baseFont, presentation);
            if (free > 0) {
                x += alignRight ? free : free / 2;
            }
        }

        for (DesktopQtTextFragment fragment : presentation.getFragments()) {
            int available = fragmentsRight - x + 1;
            if (available <= 0) {
                break;
            }

            String text = fragment.text().get();
            if (text.isEmpty()) {
                continue;
            }

            TextAttribute attribute = fragment.attribute();
            QFont font = fragmentFont(baseFont, attribute);
            QFontMetrics metrics = new QFontMetrics(font);

            int width = metrics.horizontalAdvance(text);
            boolean elided = width > available;
            if (elided) {
                text = metrics.elidedText(text, Qt.TextElideMode.ElideRight, available);
                width = metrics.horizontalAdvance(text);
            }

            QRect rect = new QRect(x, top, width, height);

            ColorValue background = attribute.getBackgroundColor();
            if (background != null && !selected) {
                painter.fillRect(rect, TargetQt.to(background));
            }

            ColorValue foreground = attribute.getForegroundColor();
            painter.setFont(font);
            painter.setPen(selected || foreground == null ? defaultColor : TargetQt.to(foreground));
            painter.drawText(rect, Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(), text);

            x += width;
            if (elided) {
                break;
            }
        }
    }

    private static void paintSuffix(
        QPainter painter,
        DesktopQtTextItemPresentation presentation,
        QFont font,
        QColor color,
        int left,
        int right,
        int top,
        int height
    ) {
        Image icon = presentation.getSuffixImage();
        int textRight = right;
        if (icon != null) {
            int iconLeft = right - icon.getWidth() + 1;
            int iconTop = top + (height - icon.getHeight()) / 2;
            DesktopQtImage.paint(painter, new QRect(iconLeft, iconTop, icon.getWidth(), icon.getHeight()), icon);
            textRight = iconLeft - SUFFIX_ICON_GAP - 1;
        }

        String text = presentation.getSuffixText().get();
        int available = textRight - left + 1;
        if (text.isEmpty() || available <= 0) {
            return;
        }

        String elided = new QFontMetricsF(font).horizontalAdvance(text) <= available
            ? text
            : new QFontMetrics(font).elidedText(text, Qt.TextElideMode.ElideRight, available);

        painter.setFont(font);
        painter.setPen(color);
        painter.drawText(
            new QRect(left, top, available, height),
            Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(),
            elided
        );
    }

    private static int fragmentsWidth(QFont font, DesktopQtTextItemPresentation presentation) {
        int width = 0;
        for (DesktopQtTextFragment fragment : presentation.getFragments()) {
            width += new QFontMetrics(fragmentFont(font, fragment.attribute())).horizontalAdvance(fragment.text().get());
        }
        return width;
    }

    private static int suffixWidth(QFont font, DesktopQtTextItemPresentation presentation) {
        int width = 0;

        Image icon = presentation.getSuffixImage();
        String text = presentation.getSuffixText().get();
        if (icon != null) {
            width += icon.getWidth();
            if (!text.isEmpty()) {
                width += SUFFIX_ICON_GAP;
            }
        }

        if (!text.isEmpty()) {
            width += (int) Math.ceil(new QFontMetricsF(font).horizontalAdvance(text));
        }
        return width;
    }

    private static QFont fragmentFont(QFont baseFont, TextAttribute attribute) {
        int fontStyle = TextAttributeUtil.getFontStyle(attribute);
        Set<TextEffect> effects = TextAttributeUtil.getEffects(attribute);

        QFont font = new QFont(baseFont);
        if ((fontStyle & Font.BOLD) != 0) {
            font.setBold(true);
        }
        if ((fontStyle & Font.ITALIC) != 0) {
            font.setItalic(true);
        }
        if (effects.contains(TextEffect.STRIKEOUT)) {
            font.setStrikeOut(true);
        }
        if (effects.contains(TextEffect.UNDERLINE) || effects.contains(TextEffect.WAVED)) {
            font.setUnderline(true);
        }
        return font;
    }
}
