/*
 * Copyright 2013-2022 consulo.io
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
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.style.StyleManager;

import javax.swing.border.Border;
import java.awt.*;

public class JBCurrentTheme {
    public static class Advertiser {
        private static final JBInsets DEFAULT_AD_INSETS = JBInsets.create(1, 5);

        
        @Deprecated
        public static Color foreground() {
            return UIUtil.getLabelForeground();
        }

        
        public static Border border() {
            return new JBEmptyBorder(JBUI.insets("Popup.Advertiser.borderInsets", DEFAULT_AD_INSETS));
        }
    }

    public static class ActionsList {
        
        public static Insets numberMnemonicInsets() {
            return JBUI.insets("ActionsList.mnemonicsBorderInsets", JBUI.insets(0, 8, 1, 6));
        }

        
        public static Insets cellPadding() {
            return JBUI.insets("ActionsList.cellBorderInsets", JBUI.insets(3, 4, 4, 4));
        }
    }

    public static class Arrow {
        
        public static Color foregroundColor(boolean enabled) {
            return enabled
                ? JBColor.namedColor("ComboBox.ArrowButton.iconColor", JBColor.namedColor("ComboBox.darcula.arrowButtonForeground", Gray.x66))
                : JBColor.namedColor("ComboBox.ArrowButton.disabledIconColor", JBColor.namedColor("ComboBox.darcula.arrowButtonDisabledForeground", Gray.xAB));

        }

        
        public static Color backgroundColor(boolean enabled, boolean editable) {
            return enabled ? editable
                ? JBColor.namedColor("ComboBox.ArrowButton.background", JBColor.namedColor("ComboBox.darcula.editable.arrowButtonBackground", Gray.xFC))
                : JBColor.namedColor("ComboBox.ArrowButton.nonEditableBackground", JBColor.namedColor("ComboBox.darcula.arrowButtonBackground", Gray.xFC)) : UIUtil.getPanelBackground();
        }
    }

    public static class Validator {
        
        public static Color errorBorderColor() {
            return JBColor.namedColor("ValidationTooltip.errorBorderColor", 0xE0A8A9);
        }

        
        public static Color errorBackgroundColor() {
            return JBColor.namedColor("ValidationTooltip.errorBackground", JBColor.namedColor("ValidationTooltip.errorBackgroundColor", 0xF5E6E7));
        }

        
        public static Color warningBorderColor() {
            return JBColor.namedColor("ValidationTooltip.warningBorderColor", 0xE0CEA8);
        }

        
        public static Color warningBackgroundColor() {
            return JBColor.namedColor("ValidationTooltip.warningBackground", JBColor.namedColor("ValidationTooltip.warningBackgroundColor", 0xF5F0E6));
        }
    }

    public static class NewClassDialog {
        
        @Deprecated
        public static Color bordersColor() {
            return JBColor.border();
        }

        public static int fieldsSeparatorWidth() {
            return JBUI.getInt("NewClass.separatorWidth", JBUIScale.scale(10));
        }
    }

    public interface Notification {
        Color FOREGROUND = JBColor.namedColor("Notification.foreground", Label.foreground());
        Color BACKGROUND = JBColor.namedColor("Notification.background", 0xFFF8D1, 0x1D3857);

        interface Error {
            Color FOREGROUND = JBColor.namedColor("Notification.errorForeground", Notification.FOREGROUND);
            Color BACKGROUND = JBColor.namedColor("Notification.errorBackground", 0xF5E6E7, 0x593D41);
            Color BORDER_COLOR = JBColor.namedColor("Notification.errorBorderColor", 0xE0A8A9, 0x73454B);
        }
    }

    public static class Focus {
        private static final Color GRAPHITE_COLOR = new JBColor(new Color(0x8099979d, true), new Color(0x676869));

        
        public static Color focusColor() {
            return UIUtil.isGraphite() ? GRAPHITE_COLOR : JBColor.namedColor("Component.focusColor", JBColor.namedColor("Focus.borderColor", 0x8ab2eb));
        }

        
        public static Color defaultButtonColor() {
            return StyleManager.get().getCurrentStyle().isDark()
                ? JBColor.namedColor(
                "Button.default.focusColor",
                JBColor.namedColor("Focus.defaultButtonBorderColor", 0x97C3F3)
            )
                : focusColor();
        }

        
        public static Color errorColor(boolean active) {
            return active
                ? JBColor.namedColor(
                "Component.errorFocusColor",
                JBColor.namedColor("Focus.activeErrorBorderColor", 0xE53E4D)
            )
                : JBColor.namedColor(
                "Component.inactiveErrorFocusColor",
                JBColor.namedColor("Focus.inactiveErrorBorderColor", 0xEBBCBC)
            );
        }

        
        public static Color warningColor(boolean active) {
            return active
                ? JBColor.namedColor(
                "Component.warningFocusColor",
                JBColor.namedColor("Focus.activeWarningBorderColor", 0xE2A53A)
            )
                : JBColor.namedColor(
                "Component.inactiveWarningFocusColor",
                JBColor.namedColor("Focus.inactiveWarningBorderColor", 0xFFD385)
            );
        }
    }

    @Deprecated
    public static class ActionButton {
        
        public static Color pressedBackground() {
            return JBColor.namedColor("ActionButton.pressedBackground", Gray.xCF);
        }

        
        public static Color pressedBorder() {
            return JBColor.namedColor("ActionButton.pressedBorderColor", Gray.xCF);
        }

        
        public static Color hoverBackground() {
            return JBColor.namedColor("ActionButton.hoverBackground", Gray.xDF);
        }

        
        public static Color hoverBorder() {
            return JBColor.namedColor("ActionButton.hoverBorderColor", Gray.xDF);
        }

        
        public static Color hoverSeparatorColor() {
            return JBColor.namedColor("ActionButton.hoverSeparatorColor", new JBColor(Gray.xB3, Gray.x6B));
        }
    }

    public static final class Tooltip {
        
        public static Color shortcutForeground() {
            return JBColor.namedColor("ToolTip.shortcutForeground", new JBColor(0x787878, 0x999999));
        }
    }

    public static final class NotificationInfo {
        
        public static Color backgroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.informativeBackground", new JBColor(0xbaeeba, 0x33412E));
        }

        
        public static Color foregroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.informativeForeground", UIUtil.getToolTipForeground());
        }

        
        public static Color borderColor() {
            return JBColor.namedColor("Notification.ToolWindow.informativeBorderColor", new JBColor(0xa0bf9d, 0x85997a));
        }
    }

    public static final class NotificationWarning {
        
        public static Color backgroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.warningBackground", new JBColor(0xf9f78e, 0x5a5221));
        }

        
        public static Color foregroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.warningForeground", UIUtil.getToolTipForeground());
        }

        
        public static Color borderColor() {
            return JBColor.namedColor("Notification.ToolWindow.warningBorderColor", new JBColor(0xbab824, 0xa69f63));
        }
    }

    public static final class NotificationError {
        
        public static Color backgroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.errorBackground", new JBColor(0xffcccc, 0x704745));
        }

        
        public static Color foregroundColor() {
            return JBColor.namedColor("Notification.ToolWindow.errorForeground", UIUtil.getToolTipForeground());
        }

        
        public static Color borderColor() {
            return JBColor.namedColor("Notification.ToolWindow.errorBorderColor", new JBColor(0xd69696, 0x998a8a));
        }
    }

    public static class CustomFrameDecorations {
        
        public static Color separatorForeground() {
            return JBColor.namedColor("Separator.separatorColor", new JBColor(0xcdcdcd, 0x515151));
        }

        
        public static Color titlePaneButtonHoverBackground() {
            return JBColor.namedColor(
                "TitlePane.Button.hoverBackground",
                new JBColor(ColorUtil.withAlpha(Color.BLACK, .1),
                    ColorUtil.withAlpha(Color.WHITE, .1))
            );
        }

        
        public static Color titlePaneButtonPressBackground() {
            return titlePaneButtonHoverBackground();
        }

        
        public static Color titlePaneInactiveBackground() {
            return JBColor.namedColor("TitlePane.inactiveBackground", titlePaneBackground());
        }

        
        public static Color titlePaneBackground(boolean active) {
            return active ? titlePaneBackground() : titlePaneInactiveBackground();
        }

        
        public static Color titlePaneBackground() {
            return JBColor.namedColor("TitlePane.background", paneBackground());
        }

        
        public static Color titlePaneInfoForeground() {
            return JBColor.namedColor("TitlePane.infoForeground", new JBColor(0x616161, 0x919191));
        }

        
        public static Color titlePaneInactiveInfoForeground() {
            return JBColor.namedColor("TitlePane.inactiveInfoForeground", new JBColor(0xA6A6A6, 0x737373));
        }

        
        public static Color paneBackground() {
            return JBColor.namedColor("Panel.background", Gray.xCD);
        }
    }

    public static class Link {
        public interface Foreground {
            Color DISABLED = JBColor.namedColor("Link.disabledForeground", Label.disabledForeground());
            Color ENABLED = JBColor.namedColor("Link.activeForeground", JBColor.namedColor("link.foreground", 0x589DF6));
            Color HOVERED = JBColor.namedColor("Link.hoverForeground", JBColor.namedColor("link.hover.foreground", ENABLED));
            Color PRESSED = JBColor.namedColor("Link.pressedForeground", JBColor.namedColor("link.pressed.foreground", 0xF00000, 0xBA6F25));
            Color VISITED = JBColor.namedColor("Link.visitedForeground", JBColor.namedColor("link.visited.foreground", 0x800080, 0x9776A9));
            Color SECONDARY = JBColor.namedColor("Link.secondaryForeground", 0x779DBD, 0x5676A0);
        }

        
        public static Color linkColor() {
            return Foreground.ENABLED;
        }

        
        public static Color linkHoverColor() {
            return Foreground.HOVERED;
        }

        
        public static Color linkPressedColor() {
            return Foreground.PRESSED;
        }

        
        public static Color linkVisitedColor() {
            return Foreground.VISITED;
        }
    }

    public static final class Label {
        
        public static Color foreground(boolean selected) {
            return selected ? JBColor.namedColor("Label.selectedForeground", 0xFFFFFF) : JBColor.namedColor("Label.foreground", 0x000000);
        }

        
        public static Color foreground() {
            return foreground(false);
        }

        
        public static Color disabledForeground(boolean selected) {
            return selected
                ? JBColor.namedColor("Label.selectedDisabledForeground", 0x999999)
                : JBColor.namedColor("Label.disabledForeground", JBColor.namedColor("Label.disabledText", 0x999999));
        }

        
        public static Color disabledForeground() {
            return disabledForeground(false);
        }
    }

    private static final Color DEFAULT_RENDERER_BACKGROUND = new JBColor(0xFFFFFF, 0x3C3F41);
    private static final Color DEFAULT_RENDERER_SELECTION_BACKGROUND = new JBColor(0x3875D6, 0x2F65CA);
    private static final Color DEFAULT_RENDERER_SELECTION_INACTIVE_BACKGROUND = new JBColor(0xD4D4D4, 0x0D293E);
    private static final Color DEFAULT_RENDERER_HOVER_BACKGROUND = new JBColor(0xEDF5FC, 0x464A4D);
    private static final Color DEFAULT_RENDERER_HOVER_INACTIVE_BACKGROUND = new JBColor(0xF5F5F5, 0x464A4D);

    public interface List {
        Color BACKGROUND = JBColor.namedColor("List.background", DEFAULT_RENDERER_BACKGROUND);
        Color FOREGROUND = JBColor.namedColor("List.foreground", Label.foreground(false));

        static int buttonLeftRightInsets() {
            return JBUI.getInt("List.Button.leftRightInset", 8);
        }

        
        static Color background(boolean selected, boolean focused) {
            return selected ? List.Selection.background(focused) : BACKGROUND;
        }

        
        static Color foreground(boolean selected, boolean focused) {
            return selected ? List.Selection.foreground(focused) : FOREGROUND;
        }

        final class Selection {
            private static final Color BACKGROUND = JBColor.namedColor("List.selectionBackground", DEFAULT_RENDERER_SELECTION_BACKGROUND);
            private static final Color FOREGROUND = JBColor.namedColor("List.selectionForeground", Label.foreground(true));

            
            public static Color background(boolean focused) {
                return focused ? BACKGROUND : List.Selection.Inactive.BACKGROUND;
            }

            
            public static Color foreground(boolean focused) {
                return focused ? FOREGROUND : List.Selection.Inactive.FOREGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("List.selectionInactiveBackground", DEFAULT_RENDERER_SELECTION_INACTIVE_BACKGROUND);
                Color FOREGROUND = JBColor.namedColor("List.selectionInactiveForeground", List.FOREGROUND);
            }
        }

        final class Hover {
            private static final Color BACKGROUND = JBColor.namedColor("List.hoverBackground", DEFAULT_RENDERER_HOVER_BACKGROUND);

            
            public static Color background(boolean focused) {
                return focused ? BACKGROUND : List.Hover.Inactive.BACKGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("List.hoverInactiveBackground", DEFAULT_RENDERER_HOVER_INACTIVE_BACKGROUND);
            }
        }
    }

    public interface Table {
        Color BACKGROUND = JBColor.namedColor("Table.background", DEFAULT_RENDERER_BACKGROUND);
        Color FOREGROUND = JBColor.namedColor("Table.foreground", Label.foreground(false));

        static
        
        Color background(boolean selected, boolean focused) {
            return selected ? Table.Selection.background(focused) : BACKGROUND;
        }

        static
        
        Color foreground(boolean selected, boolean focused) {
            return selected ? Table.Selection.foreground(focused) : FOREGROUND;
        }

        final class Selection {
            private static final Color BACKGROUND = JBColor.namedColor("Table.selectionBackground", DEFAULT_RENDERER_SELECTION_BACKGROUND);
            private static final Color FOREGROUND = JBColor.namedColor("Table.selectionForeground", Label.foreground(true));

            public static
            
            Color background(boolean focused) {
                return focused ? BACKGROUND : Table.Selection.Inactive.BACKGROUND;
            }

            public static
            
            Color foreground(boolean focused) {
                return focused ? FOREGROUND : Table.Selection.Inactive.FOREGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("Table.selectionInactiveBackground", DEFAULT_RENDERER_SELECTION_INACTIVE_BACKGROUND);
                Color FOREGROUND = JBColor.namedColor("Table.selectionInactiveForeground", Table.FOREGROUND);
            }
        }

        final class Hover {
            private static final Color BACKGROUND = JBColor.namedColor("Table.hoverBackground", DEFAULT_RENDERER_HOVER_BACKGROUND);

            public static
            
            Color background(boolean focused) {
                return focused ? BACKGROUND : Table.Hover.Inactive.BACKGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("Table.hoverInactiveBackground", DEFAULT_RENDERER_HOVER_INACTIVE_BACKGROUND);
            }
        }
    }

    public interface Tree {
        Color BACKGROUND = JBColor.namedColor("Tree.background", DEFAULT_RENDERER_BACKGROUND);
        Color FOREGROUND = JBColor.namedColor("Tree.foreground", Label.foreground(false));

        static
        
        Color background(boolean selected, boolean focused) {
            return selected ? Tree.Selection.background(focused) : BACKGROUND;
        }

        static
        
        Color foreground(boolean selected, boolean focused) {
            return selected ? Tree.Selection.foreground(focused) : FOREGROUND;
        }

        final class Selection {
            private static final Color BACKGROUND = JBColor.namedColor("Tree.selectionBackground", DEFAULT_RENDERER_SELECTION_BACKGROUND);
            private static final Color FOREGROUND = JBColor.namedColor("Tree.selectionForeground", Label.foreground(true));

            
            public static Color background(boolean focused) {
                return focused ? BACKGROUND : Tree.Selection.Inactive.BACKGROUND;
            }

            
            public static Color foreground(boolean focused) {
                return focused ? FOREGROUND : Tree.Selection.Inactive.FOREGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("Tree.selectionInactiveBackground", DEFAULT_RENDERER_SELECTION_INACTIVE_BACKGROUND);
                Color FOREGROUND = JBColor.namedColor("Tree.selectionInactiveForeground", Tree.FOREGROUND);
            }
        }

        final class Hover {
            private static final Color BACKGROUND = JBColor.namedColor("Tree.hoverBackground", DEFAULT_RENDERER_HOVER_BACKGROUND);

            public static
            
            Color background(boolean focused) {
                return focused ? BACKGROUND : Tree.Hover.Inactive.BACKGROUND;
            }

            private interface Inactive {
                Color BACKGROUND = JBColor.namedColor("Tree.hoverInactiveBackground", DEFAULT_RENDERER_HOVER_INACTIVE_BACKGROUND);
            }
        }
    }

    public static class BigPopup {
        
        public static Insets tabInsets() {
            return JBInsets.create(0, 12);
        }

        
        @Deprecated
        public static Color searchFieldBorderColor() {
            return JBColor.border();
        }

        public static int maxListHeight() {
            return JBUIScale.scale(600);
        }

        
        public static Color listTitleLabelForeground() {
            return JBColor.namedColor("SearchEverywhere.List.separatorForeground", UIUtil.getLabelDisabledForeground());
        }

        
        public static Color advertiserForeground() {
            return JBColor.namedColor("SearchEverywhere.Advertiser.foreground", JBColor.GRAY);
        }

        
        public static Border advertiserBorder() {
            return new JBEmptyBorder(JBUI.insets("SearchEverywhere.Advertiser.foreground", JBUI.insetsLeft(8)));
        }

        
        public static Color advertiserBackground() {
            return JBColor.namedColor("SearchEverywhere.Advertiser.background", 0xf2f2f2);
        }
    }

    public static class Popup {
        public static final float DEFAULT_HINT_OPACITY = 0.55f;

        public static Color borderColor(boolean active) {
            return JBColor.border();
        }

        @Deprecated
        public static Color separatorColor() {
            return JBColor.border();
        }

        public static Color separatorTextColor() {
            return JBColor.namedColor("Popup.separatorForeground", Color.gray);
        }

        public static int toolbarHeight() {
            return JBUI.scale(28);
        }

        public static Color toolbarBorderColor() {
            return UIUtil.getBorderColor();
        }

        public static Color toolbarPanelColor() {
            return UIUtil.getPanelBackground();
        }
    }

    
    public static Border listCellBorder() {
        return JBUI.Borders.empty(2, 4);
    }

    
    public static Border listCellBorderSemi() {
        return JBUI.Borders.empty(3, 4);
    }

    
    public static Border listCellBorderFull() {
        return JBUI.Borders.empty(4);
    }

    public static Border comboBoxSubBorder(boolean editable) {
        if (editable) {
            return JBUI.Borders.emptyRight(8);
        }
        return textFieldSubBorder(false);
    }

    public static Border textFieldSubBorder(boolean left) {
        if (left) {
            return JBUI.Borders.emptyLeft(4);
        }
        else {
            return JBUI.Borders.emptyRight(4);
        }
    }
}
