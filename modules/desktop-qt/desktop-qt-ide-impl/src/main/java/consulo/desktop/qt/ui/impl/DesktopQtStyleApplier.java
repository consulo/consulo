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
     * The corner radii of the awt look and feel, which is where the shape of consulo is decided - flatlaf defaults
     * for {@code Button.arc}, {@code ProgressBar.arc} and {@code Popup.borderCornerRadius}, and the
     * {@code TextComponent.arc} of {@code ConsuloLightLaf}/{@code ConsuloDarkLaf} over the flatlaf default of 0.
     * {@code Component.arc} is what a combo box and a spinner are drawn with.
     */
    private static final int ourTextArc = 8;
    private static final int ourComponentArc = 5;
    private static final int ourButtonArc = 6;
    private static final int ourProgressArc = 4;

    /**
     * The last style applied, so a popup built after a theme change is bordered with the colors of that theme
     * rather than of the one which happened to be up when the class was loaded.
     */
    private static volatile @Nullable Style ourStyle;

    /**
     * An application style sheet wraps the style of the application into a proxy, so once one is written the style
     * asked for cannot be read back off {@code QApplication.style()} any more.
     */
    private static boolean ourStyleForced;

    private DesktopQtStyleApplier() {
    }

    public static void apply(Style style) {
        ourStyle = style;

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
        ) + buildCornerStyleSheet(style);
    }

    /**
     * The corners. Qt only rounds what a style sheet draws, so every rule here has to state the border and the fill
     * the widget would otherwise have had from fusion - and the states with them, or a field would keep the frame of
     * an idle one while it has the focus.
     * <p/>
     * A combo box and a spinner reach their arrow through a subcontrol, and the moment the widget is styled qt stops
     * asking fusion for it. Both arrows are therefore drawn again here as a css triangle - a box of no size whose
     * borders meet in a point - which is what keeps them once the frame is ours.
     * <p/>
     * A list, a tree and a scroll pane are left square on purpose: {@code ScrollPane.arc} is 0 in the awt look and
     * feel, so a rounded frame around one would be a shape consulo does not have.
     */
    private static String buildCornerStyleSheet(Style style) {
        String border = css(style, ComponentColors.BORDER);
        String disabledBorder = css(style, ComponentColors.DISABLED_BORDER);
        String focus = css(style, ComponentColors.FOCUS_COLOR);
        String componentBackground = css(style, ComponentColors.COMPONENT_BACKGROUND);
        String layout = css(style, ComponentColors.LAYOUT);
        String hover = css(style, ComponentColors.HOVER_BACKGROUND);
        String text = css(style, ComponentColors.TEXT_FOREGROUND);
        String disabledText = css(style, ComponentColors.DISABLED_TEXT);
        String accent = css(style, ComponentColors.TABBED_PANE_UNDERLINE);

        return """

            QLineEdit, QPlainTextEdit { border: 1px solid %s; border-radius: %dpx; background: %s; padding: 3px 6px; }
            QLineEdit:focus, QPlainTextEdit:focus { border: 1px solid %s; }
            QLineEdit:disabled, QPlainTextEdit:disabled { border: 1px solid %s; }

            QPushButton { border: 1px solid %s; border-radius: %dpx; background: %s; padding: 4px 14px; }
            QPushButton:hover { background: %s; }
            QPushButton:focus { border: 1px solid %s; }
            QPushButton:disabled { border: 1px solid %s; color: %s; }

            QProgressBar { border: 1px solid %s; border-radius: %dpx; background: %s; text-align: center; }
            QProgressBar::chunk { background: %s; border-radius: %dpx; }

            QComboBox, QAbstractSpinBox { border: 1px solid %s; border-radius: %dpx; background: %s; padding: 3px 6px; }
            QComboBox:focus, QAbstractSpinBox:focus { border: 1px solid %s; }
            QComboBox:disabled, QAbstractSpinBox:disabled { border: 1px solid %s; color: %s; }
            QComboBox::drop-down { subcontrol-origin: padding; subcontrol-position: center right; width: 16px; border: none; background: transparent; }
            QComboBox::down-arrow { width: 0px; height: 0px; border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 5px solid %s; }
            QAbstractSpinBox::up-button { subcontrol-origin: border; subcontrol-position: top right; width: 16px; border: none; background: transparent; }
            QAbstractSpinBox::down-button { subcontrol-origin: border; subcontrol-position: bottom right; width: 16px; border: none; background: transparent; }
            QAbstractSpinBox::up-arrow { width: 0px; height: 0px; border-left: 3px solid transparent; border-right: 3px solid transparent; border-bottom: 4px solid %s; }
            QAbstractSpinBox::down-arrow { width: 0px; height: 0px; border-left: 3px solid transparent; border-right: 3px solid transparent; border-top: 4px solid %s; }
            """.formatted(
            border, ourTextArc, componentBackground,
            focus,
            disabledBorder,
            border, ourButtonArc, layout,
            hover,
            focus,
            disabledBorder, disabledText,
            border, ourProgressArc, componentBackground,
            accent, ourProgressArc,
            border, ourComponentArc, componentBackground,
            focus,
            disabledBorder, disabledText,
            text,
            text,
            text
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
