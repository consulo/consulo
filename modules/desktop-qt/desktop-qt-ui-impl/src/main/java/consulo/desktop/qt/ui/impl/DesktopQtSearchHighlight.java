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

import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.color.RGBColor;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionButton;
import org.jspecify.annotations.Nullable;

/**
 * The search highlight of a qt text. A label shows rich text, so its matches are marked up; a check box, a radio
 * button and a push button draw plain text only, so their label is painted here instead - the control without its
 * text by the style, then the text run by run with the matches on the search match colour.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopQtSearchHighlight {
    private static final String CLOSE_TAG = "</span>";

    private static final int ARC = 3;
    private static final int ICON_TEXT_GAP = 4;

    private DesktopQtSearchHighlight() {
    }

    private static String openTag() {
        return "<span style=\"background-color:#" + ColorValueUtil.toHex(ComponentColors.SEARCH_MATCH_BACKGROUND)
            + ";color:#" + ColorValueUtil.toHex(ComponentColors.SEARCH_MATCH_FOREGROUND) + "\">";
    }

    private static QColor toQColor(RGBColor color) {
        return new QColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * @return rich text of the plain text with the matches marked, or {@code null} if the query matches nothing
     */
    public static @Nullable String toRichText(String plainText, @Nullable String query) {
        if (!SearchHighlightUtil.matches(plainText, query)) {
            return null;
        }

        return "<span style=\"white-space:pre\">" + SearchHighlightUtil.markupText(plainText, query, openTag(), CLOSE_TAG) + "</span>";
    }

    /**
     * @return the rich text with the matches of its shown text marked, or {@code null} if the query matches nothing
     */
    public static @Nullable String markupRichText(String richText, @Nullable String query) {
        if (query == null) {
            return null;
        }

        String marked = SearchHighlightUtil.markupHtml(richText, query, openTag(), CLOSE_TAG);
        return marked.equals(richText) ? null : marked;
    }

    /**
     * Paints a check box or a radio button whose label matches the query.
     */
    public static void paintCheckable(
        QAbstractButton button,
        QStyleOptionButton option,
        QStyle.ControlElement control,
        QStyle.SubElement contents,
        String text,
        String query
    ) {
        QStyle style = button.style();
        QRect textRect = style.subElementRect(contents, option, button);

        option.setText("");

        QPainter painter = new QPainter(button);
        try {
            style.drawControl(control, option, painter, button);

            paintText(painter, button, textRect, text, query, textColor(button, QPalette.ColorRole.WindowText));
        }
        finally {
            painter.end();
        }
    }

    /**
     * Paints a push button whose label matches the query, with its icon beside the text the way the style places it.
     */
    public static void paintPushButton(QAbstractButton button, QStyleOptionButton option, String text, String query) {
        QStyle style = button.style();
        QRect contents = style.subElementRect(QStyle.SubElement.SE_PushButtonContents, option, button);

        QIcon icon = option.icon();
        QSize iconSize = option.iconSize();
        boolean hasIcon = !icon.isNull() && iconSize.width() > 0;

        QStyle.State state = option.state();
        if (state.testFlag(QStyle.StateFlag.State_Sunken) || state.testFlag(QStyle.StateFlag.State_On)) {
            contents.translate(
                style.pixelMetric(QStyle.PixelMetric.PM_ButtonShiftHorizontal, option, button),
                style.pixelMetric(QStyle.PixelMetric.PM_ButtonShiftVertical, option, button)
            );
        }

        option.setText("");
        option.setIcon(new QIcon());

        QPainter painter = new QPainter(button);
        try {
            style.drawControl(QStyle.ControlElement.CE_PushButtonBevel, option, painter, button);

            int textWidth = new QFontMetrics(button.font()).horizontalAdvance(text);
            int iconWidth = hasIcon ? iconSize.width() + ICON_TEXT_GAP : 0;
            int left = contents.left() + Math.max(0, (contents.width() - iconWidth - textWidth) / 2);

            if (hasIcon) {
                int iconTop = contents.top() + (contents.height() - iconSize.height()) / 2;
                QRect iconRect = new QRect(left, iconTop, iconSize.width(), iconSize.height());
                icon.paint(
                    painter,
                    iconRect,
                    new Qt.Alignment(Qt.AlignmentFlag.AlignCenter),
                    button.isEnabled() ? QIcon.Mode.Normal : QIcon.Mode.Disabled
                );
            }

            int textLeft = left + iconWidth;
            QRect textRect = new QRect(textLeft, contents.top(), Math.max(0, contents.right() + 1 - textLeft), contents.height());
            paintText(painter, button, textRect, text, query, textColor(button, QPalette.ColorRole.ButtonText));
        }
        finally {
            painter.end();
        }
    }

    private static void paintText(
        QPainter painter,
        QAbstractButton button,
        QRect area,
        String text,
        String query,
        QColor textColor
    ) {
        QFontMetrics metrics = new QFontMetrics(button.font());

        int[] x = {area.left()};

        int boxHeight = Math.min(area.height(), metrics.height());
        int boxTop = area.top() + (area.height() - boxHeight) / 2;

        QColor background = toQColor(ComponentColors.SEARCH_MATCH_BACKGROUND.toRGB());
        QColor foreground = toQColor(ComponentColors.SEARCH_MATCH_FOREGROUND.toRGB());

        painter.setFont(button.font());
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

        SearchHighlightUtil.forEachFragment(text, query, (fragment, matched) -> {
            int width = metrics.horizontalAdvance(fragment);

            if (matched) {
                painter.setPen(Qt.PenStyle.NoPen);
                painter.setBrush(new QBrush(background));
                painter.drawRoundedRect(new QRect(x[0], boxTop, width, boxHeight), ARC, ARC);
            }

            painter.setPen(matched ? foreground : textColor);
            painter.drawText(
                new QRect(x[0], area.top(), width, area.height()),
                Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(),
                fragment
            );

            x[0] += width;
        });
    }

    private static QColor textColor(QAbstractButton button, QPalette.ColorRole role) {
        return button.palette().color(button.isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled, role);
    }
}
