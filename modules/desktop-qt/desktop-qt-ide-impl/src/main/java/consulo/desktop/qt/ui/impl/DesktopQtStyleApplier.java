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

import consulo.ui.color.RGBColor;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleColorValue;
import io.qt.gui.QColor;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;

import java.util.List;

/**
 * Paints qt with the colors of a consulo theme, the way {@code WebStyleCssRegistry} writes them into css custom
 * properties on the web frontend.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class DesktopQtStyleApplier {
    /**
     * Every desktop style of qt - breeze on kde, windowsvista, macos - draws part of its widgets with colors of the
     * desktop rather than of the palette handed to it, so a theme applied over one of them shows through only where
     * the style happens to ask. Fusion is the one style qt ships which takes all of its colors from the palette, and
     * it is what makes a consulo theme rather than the desktop decide the look.
     */
    private static final String STYLE_NAME = "Fusion";

    private static final List<QPalette.ColorGroup> ourGroups =
        List.of(QPalette.ColorGroup.Active, QPalette.ColorGroup.Inactive, QPalette.ColorGroup.Disabled);

    /**
     * An application style sheet wraps the style of the application into a proxy, so once one is written the style
     * asked for cannot be read back off {@code QApplication.style()} any more.
     */
    private static boolean ourStyleForced;

    private DesktopQtStyleApplier() {
    }

    public static void apply(Style style) {
        if (!ourStyleForced) {
            ourStyleForced = true;

            // setStyle rebuilds the application palette out of the standard palette of the new style, so the theme
            // has to be written after it
            QApplication.setStyle(STYLE_NAME);
        }

        QApplication.setPalette(buildPalette(style));

        QApplication.instance().setStyleSheet(buildStyleSheet(style));
    }

    private static QPalette buildPalette(Style style) {
        QColor layout = color(style, ComponentColors.LAYOUT);
        QColor text = color(style, ComponentColors.TEXT_FOREGROUND);
        QColor disabledText = color(style, ComponentColors.DISABLED_TEXT);
        QColor componentBackground = color(style, ComponentColors.COMPONENT_BACKGROUND);
        QColor border = color(style, ComponentColors.BORDER);
        QColor link = color(style, ComponentColors.LINK_FOREGROUND);
        QColor selectionBackground = color(style, ComponentColors.SELECTION_BACKGROUND);
        QColor selectionForeground = color(style, ComponentColors.SELECTION_FOREGROUND);
        QColor inactiveSelectionBackground = color(style, ComponentColors.SELECTION_INACTIVE_BACKGROUND);
        QColor inactiveSelectionForeground = color(style, ComponentColors.SELECTION_INACTIVE_FOREGROUND);
        QColor toolTipBackground = color(style, ComponentColors.TOOLTIP_BACKGROUND);
        QColor toolTipForeground = color(style, ComponentColors.TOOLTIP_FOREGROUND);

        // seeding off the background of the theme gives Light, Midlight, Dark and Shadow - the shades fusion draws
        // frames and gradients with, and which no theme key spells out - a series which belongs to that background
        QPalette palette = new QPalette(layout);

        for (QPalette.ColorGroup group : ourGroups) {
            palette.setColor(group, QPalette.ColorRole.Window, layout);
            palette.setColor(group, QPalette.ColorRole.Button, layout);
            palette.setColor(group, QPalette.ColorRole.AlternateBase, layout);
            palette.setColor(group, QPalette.ColorRole.Base, componentBackground);
            palette.setColor(group, QPalette.ColorRole.WindowText, text);
            palette.setColor(group, QPalette.ColorRole.Text, text);
            palette.setColor(group, QPalette.ColorRole.ButtonText, text);
            palette.setColor(group, QPalette.ColorRole.BrightText, text);
            palette.setColor(group, QPalette.ColorRole.PlaceholderText, disabledText);
            palette.setColor(group, QPalette.ColorRole.ToolTipBase, toolTipBackground);
            palette.setColor(group, QPalette.ColorRole.ToolTipText, toolTipForeground);
            palette.setColor(group, QPalette.ColorRole.Link, link);
            palette.setColor(group, QPalette.ColorRole.LinkVisited, link);
            palette.setColor(group, QPalette.ColorRole.Mid, border);
            palette.setColor(group, QPalette.ColorRole.Accent, color(style, ComponentColors.TABBED_PANE_UNDERLINE));
            palette.setColor(group, QPalette.ColorRole.Highlight, selectionBackground);
            palette.setColor(group, QPalette.ColorRole.HighlightedText, selectionForeground);
        }

        palette.setColor(QPalette.ColorGroup.Inactive, QPalette.ColorRole.Highlight, inactiveSelectionBackground);
        palette.setColor(QPalette.ColorGroup.Inactive, QPalette.ColorRole.HighlightedText, inactiveSelectionForeground);

        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.WindowText, disabledText);
        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.Text, disabledText);
        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.ButtonText, disabledText);
        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.Mid, color(style, ComponentColors.DISABLED_BORDER));
        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.Highlight, inactiveSelectionBackground);
        palette.setColor(QPalette.ColorGroup.Disabled, QPalette.ColorRole.HighlightedText, inactiveSelectionForeground);

        return palette;
    }

    /**
     * The keys the palette of qt has no role for. Only widget parts are named here, never a widget as a whole - a
     * rule which fills a widget would take over the drawing of it from fusion, and would also outrank whatever style
     * sheet the component wrote for itself.
     */
    private static String buildStyleSheet(Style style) {
        String border = css(style, ComponentColors.BORDER);
        String thumb = css(style, ComponentColors.SCROLL_BAR_THUMB);
        String hoverThumb = css(style, ComponentColors.SCROLL_BAR_HOVER_THUMB);
        String tabBackground = css(style, ComponentColors.TABBED_PANE_BACKGROUND);
        String tabForeground = css(style, ComponentColors.TABBED_PANE_FOREGROUND);
        String tabHover = css(style, ComponentColors.TABBED_PANE_HOVER);
        String tabUnderline = css(style, ComponentColors.TABBED_PANE_UNDERLINE);
        String separator = css(style, ComponentColors.SEPARATOR);
        String toolTipBackground = css(style, ComponentColors.TOOLTIP_BACKGROUND);
        String toolTipForeground = css(style, ComponentColors.TOOLTIP_FOREGROUND);

        return """
            QToolTip { background-color: %s; color: %s; border: 1px solid %s; }

            QScrollBar:vertical { background: transparent; width: 12px; margin: 0px; }
            QScrollBar:horizontal { background: transparent; height: 12px; margin: 0px; }
            QScrollBar::handle:vertical { background: %s; border-radius: 4px; min-height: 24px; margin: 2px; }
            QScrollBar::handle:horizontal { background: %s; border-radius: 4px; min-width: 24px; margin: 2px; }
            QScrollBar::handle:hover { background: %s; }
            QScrollBar::add-line, QScrollBar::sub-line { width: 0px; height: 0px; }
            QScrollBar::add-page, QScrollBar::sub-page { background: transparent; }

            QTabWidget::pane { border: 1px solid %s; }
            QTabBar::tab { background: %s; color: %s; padding: 4px 10px; border: none; border-bottom: 2px solid transparent; }
            QTabBar::tab:hover { background: %s; }
            QTabBar::tab:selected { border-bottom: 2px solid %s; }

            QMenu::separator { height: 1px; background: %s; margin: 4px 0px; }
            """.formatted(
            toolTipBackground,
            toolTipForeground,
            border,
            thumb,
            thumb,
            hoverThumb,
            border,
            tabBackground,
            tabForeground,
            tabHover,
            tabUnderline,
            separator
        );
    }

    private static QColor color(Style style, StyleColorValue colorValue) {
        RGBColor rgb = style.getColorValue(colorValue).toRGB();
        return new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha());
    }

    private static String css(Style style, StyleColorValue colorValue) {
        RGBColor rgb = style.getColorValue(colorValue).toRGB();
        return "rgba(%d, %d, %d, %d%%)".formatted(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha() * 100 / 255);
    }
}
