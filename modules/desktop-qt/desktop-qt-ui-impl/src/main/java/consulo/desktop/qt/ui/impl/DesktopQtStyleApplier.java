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
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Paints qt with the colors of a consulo theme, the way {@code WebStyleCssRegistry} writes them into css custom
 * properties on the web frontend.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class DesktopQtStyleApplier {
    private static final List<QPalette.ColorGroup> ourGroups =
        List.of(QPalette.ColorGroup.Active, QPalette.ColorGroup.Inactive, QPalette.ColorGroup.Disabled);

    /**
     * The {@code Button.arc} of the awt look and feel, which the header of a titleless window rounds its menu
     * entries with. Nothing of qt is shaped here - the style of the desktop decides whether a widget is rounded,
     * and the header is drawn by consulo rather than by that style.
     */
    private static final int ourButtonArc = 6;

    /**
     * What an item of the menu bar in the header of a titleless window is padded by, which is what decides how
     * tall the menu bar asks to be.
     */
    private static final int ourMenuBarItemVerticalPadding = 3;
    private static final int ourMenuBarItemHorizontalPadding = 8;

    /**
     * The last style applied, so a popup built after a theme change is bordered with the colors of that theme
     * rather than of the one which happened to be up when the class was loaded.
     */
    private static volatile @Nullable Style ourStyle;

    private DesktopQtStyleApplier() {
    }

    public static void apply(Style style) {
        ourStyle = style;

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
            palette.setColor(group, QPalette.ColorRole.Accent, color(style, ComponentColors.TABBED_LAYOUT_UNDERLINE));
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
     * Only the widgets consulo draws itself are named here. A rule naming a widget of qt takes over the drawing of
     * that widget from the style of the desktop, which decides how everything of it looks - whether it is rounded
     * among the rest - and the theme reaches it through the palette instead.
     */
    private static String buildStyleSheet(Style style) {
        return buildTitleBarStyleSheet(style);
    }

    /**
     * The header a titleless window is decorated with. It is the one widget named as a whole - the header is drawn
     * by consulo rather than by the style of the desktop, so there is nothing of that style to be taken over.
     * Keeping the rules in the application style sheet is also what repaints a header in the colors of a theme
     * picked while the window is already up.
     */
    private static String buildTitleBarStyleSheet(Style style) {
        String layout = css(style, ComponentColors.LAYOUT);
        String border = css(style, ComponentColors.BORDER);
        String text = css(style, ComponentColors.TEXT_FOREGROUND);
        String menuSelection = css(style, ComponentColors.MENU_SELECTION_BACKGROUND);
        String selectionForeground = css(style, ComponentColors.SELECTION_FOREGROUND);

        // the padding of an item is what a menu bar asks its height for, and the header is one row of a height of
        // its own - breeze pads an item enough to push the menu text off the line the title is drawn on
        return """

            #consuloTitleBar { background: %s; border-bottom: 1px solid %s; }
            #consuloTitleBar QMenuBar { background: transparent; border: none; padding: 0px; }
            #consuloTitleBar QMenuBar::item { background: transparent; color: %s; padding: %dpx %dpx; margin: 0px; border-radius: %dpx; }
            #consuloTitleBar QMenuBar::item:selected { background: %s; color: %s; }
            #consuloTitleBar QMenuBar::item:pressed { background: %s; color: %s; }
            """.formatted(
            layout,
            border,
            text,
            ourMenuBarItemVerticalPadding,
            ourMenuBarItemHorizontalPadding,
            ourButtonArc,
            menuSelection,
            selectionForeground,
            menuSelection,
            selectionForeground
        );
    }


    /**
     * The border of a frameless popup, written against the object name so it reaches the frame and none of the
     * widgets inside it.
     */
    public static String popupFrameStyleSheet(String objectName, int cornerRadius) {
        Style style = ourStyle;
        String border = style == null ? "palette(mid)" : css(style, ComponentColors.BORDER);
        String background = style == null ? "palette(window)" : css(style, ComponentColors.LAYOUT);

        return "#%s { border: 1px solid %s; border-radius: %dpx; background: %s; }".formatted(
            objectName,
            border,
            cornerRadius,
            background
        );
    }

    /**
     * A color of the theme for whatever is painted rather than styled - the decoration a titleless window draws
     * itself is, and none of it belongs to a role of the qt palette.
     *
     * @param fallback what the color is before a theme has been applied, which is the case while the application
     *                 is still coming up
     */
    public static QColor themeColor(StyleColorValue colorValue, QColor fallback) {
        Style style = ourStyle;

        return style == null ? fallback : color(style, colorValue);
    }

    /**
     * A palette which paints the same whether the window holding it is the active one. Wayland never hands an
     * ungrabbed popup the keyboard, so a popup is never active and qt would draw every selection of it out of the
     * Inactive group.
     */
    public static QPalette alwaysActive(QPalette source) {
        QPalette palette = new QPalette(source);

        for (QPalette.ColorRole role : List.of(
            QPalette.ColorRole.Highlight,
            QPalette.ColorRole.HighlightedText,
            QPalette.ColorRole.Text,
            QPalette.ColorRole.WindowText,
            QPalette.ColorRole.Base,
            QPalette.ColorRole.Window
        )) {
            palette.setColor(QPalette.ColorGroup.Inactive, role, palette.color(QPalette.ColorGroup.Active, role));
        }

        return palette;
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
