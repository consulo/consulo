// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.*;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

@SuppressWarnings("unused")
public class JBUI_New {
  public static class CurrentTheme {
    public static class ActionButton {
      @Nonnull
      public static Color pressedBackground() {
        return JBColor.namedColor("ActionButton.pressedBackground", Gray.xCF);
      }

      @Nonnull
      public static Color pressedBorder() {
        return JBColor.namedColor("ActionButton.pressedBorderColor", Gray.xCF);
      }

      @Nonnull
      public static Color hoverBackground() {
        return JBColor.namedColor("ActionButton.hoverBackground", Gray.xDF);
      }

      @Nonnull
      public static Color hoverBorder() {
        return JBColor.namedColor("ActionButton.hoverBorderColor", Gray.xDF);
      }

      @Nonnull
      public static Color hoverSeparatorColor() {
        return JBColor.namedColor("ActionButton.hoverSeparatorColor", new JBColor(Gray.xB3, Gray.x6B));
      }
    }

    public static class CustomFrameDecorations {
      @Nonnull
      public static Color separatorForeground() {
        return JBColor.namedColor("Separator.separatorColor", new JBColor(0xcdcdcd, 0x515151));
      }

      @Nonnull
      public static Color titlePaneBackground() {
        return JBColor.namedColor("TitlePane.background", paneBackground());
      }

      @Nonnull
      public static Color paneBackground() {
        return JBColor.namedColor("Panel.background", 0xcdcdcd);
      }
    }

    public static class DefaultTabs {
      @Nonnull
      public static Color underlineColor() {
        return JBColor.namedColor("DefaultTabs.underlineColor", new JBColor(0x4083C9, 0x4A88C7));
      }

      public static int underlineHeight() {
        return JBUI.getInt("DefaultTabs.underlineHeight", JBUI.scale(2));
      }

      @Nonnull
      public static Color inactiveUnderlineColor() {
        return JBColor.namedColor("DefaultTabs.inactiveUnderlineColor", new JBColor(0xABABAB, 0x7A7A7A));
      }

      @Nonnull
      public static Color borderColor() {
        return JBColor.namedColor("DefaultTabs.borderColor", ToolWindow.headerBorderBackground());
      }

      @Nonnull
      public static Color background() {
        return JBColor.namedColor("DefaultTabs.background", ToolWindow.headerBackground());
      }

      @Nonnull
      public static Color hoverMaskColor() {
        return JBColor.namedColor("DefaultTabs.hoverMaskColor", new JBColor(ColorUtil.withAlpha(Color.BLACK, .10), ColorUtil.withAlpha(Color.BLACK, .35)));
      }

      @Nonnull
      public static Color uncoloredTabSelectedColor() {
        return JBColor.namedColor("DefaultTabs.uncoloredTabSelectedColor", DefaultTabs.background());
      }

      @Nonnull
      public static Color hoverColor() {
        return JBColor.namedColor("DefaultTabs.hoverColor", new JBColor(0xD9D9D9, 0x2E3133));
      }

      @Nonnull
      public static Color inactiveMaskColor() {
        return JBColor.namedColor("DefaultTabs.inactiveMaskColor", new JBColor(ColorUtil.withAlpha(Color.BLACK, .07), ColorUtil.withAlpha(Color.BLACK, .13)));

      }
    }

    public static class EditorTabs {
      @Nonnull
      public static Color underlineColor() {
        return JBColor.namedColor("EditorTabs.underlineColor", DefaultTabs.underlineColor());
      }

      public static int underlineHeight() {
        return JBUI.getInt("EditorTabs.underlineHeight", JBUI.scale(3));
      }

      @Nonnull
      public static Color inactiveUnderlineColor() {
        return JBColor.namedColor("EditorTabs.inactiveUnderlineColor", DefaultTabs.inactiveUnderlineColor());
      }

      @Nonnull
      public static Color uncoloredTabSelectedColor() {
        return JBColor.namedColor("EditorTabs.uncoloredTabSelectedColor", DefaultTabs.uncoloredTabSelectedColor());
      }

      @Nonnull
      public static Color borderColor() {
        return JBColor.namedColor("EditorTabs.borderColor", DefaultTabs.borderColor());
      }

      @Nonnull
      public static Color background() {
        return JBColor.namedColor("EditorTabs.background", DefaultTabs.background());
      }

      @Nonnull
      public static Color hoverMaskColor() {
        return JBColor.namedColor("EditorTabs.hoverMaskColor", DefaultTabs.hoverMaskColor());
      }

      @Nonnull
      public static Color hoverColor() {
        return JBColor.namedColor("EditorTabs.hoverColor", DefaultTabs.hoverColor());
      }

      @Nonnull
      public static Color inactiveMaskColor() {
        return JBColor.namedColor("EditorTabs.inactiveMaskColor", DefaultTabs.inactiveMaskColor());
      }

    }

    public static class ToolWindow {

      @Nonnull
      public static Color tabSelectedBackground() {
        return JBColor.namedColor("ToolWindow.HeaderTab.selectedInactiveBackground", JBColor.namedColor("ToolWindow.header.tab.selected.background", 0xDEDEDE));
      }

      @Nonnull
      public static Color tabSelectedActiveBackground() {
        return JBColor.namedColor("ToolWindow.HeaderTab.selectedBackground", JBColor.namedColor("ToolWindow.header.tab.selected.active.background", 0xD0D4D8));
      }

      @Nonnull
      public static Color tabHoveredBackground() {
        return JBColor.namedColor("ToolWindow.HeaderTab.hoverInactiveBackground", JBColor.namedColor("ToolWindow.header.tab.hovered.background", tabSelectedBackground()));
      }

      @Nonnull
      public static Color tabHoveredActiveBackground() {
        return JBColor.namedColor("ToolWindow.HeaderTab.hoverBackground", JBColor.namedColor("ToolWindow.header.tab.hovered.active.background", tabSelectedActiveBackground()));
      }

      @Nonnull
      public static Color tabSelectedBackground(boolean active) {
        return active ? tabSelectedActiveBackground() : tabSelectedBackground();
      }

      @Nonnull
      public static Color tabHoveredBackground(boolean active) {
        return active ? tabHoveredActiveBackground() : tabHoveredBackground();
      }

      @Nonnull
      public static Color headerBackground(boolean active) {
        return active ? headerActiveBackground() : headerBackground();
      }

      @Nonnull
      public static Color headerBackground() {
        return JBColor.namedColor("ToolWindow.Header.inactiveBackground", JBColor.namedColor("ToolWindow.header.background", 0xECECEC));
      }

      @Nonnull
      public static Color headerBorderBackground() {
        return JBColor.namedColor("ToolWindow.Header.borderColor", JBColor.namedColor("ToolWindow.header.border.background", 0xC9C9C9));
      }

      @Nonnull
      public static Color headerActiveBackground() {
        return JBColor.namedColor("ToolWindow.Header.background", JBColor.namedColor("ToolWindow.header.active.background", 0xE2E6EC));
      }

      public static int tabVerticalPaddingOld() {
        return JBUI.getInt("ToolWindow.tab.verticalPadding", 0);
      }

      public static int tabVerticalPadding() {
        return JBUI.getInt("ToolWindow.HeaderTab.verticalPadding", JBUI.scale(6));
      }

      @Nonnull
      @Deprecated
      public static Border tabBorder() {
        return JBUI.getBorder("ToolWindow.tabBorder", JBUI.Borders.empty(1));
      }

      @Nonnull
      public static Border tabHeaderBorder() {
        return JBUI.getBorder("ToolWindow.HeaderTab.tabHeaderBorder", JBUI.Borders.empty(1, 0));
      }

      public static int underlineHeight() {
        return JBUI.getInt("ToolWindow.HeaderTab.underlineHeight", JBUI.scale(3));
      }


      @Nonnull
      public static Font headerFont() {
        JBFont font = JBUI.Fonts.label();
        Object size = UIManager.get("ToolWindow.header.font.size");
        if (size instanceof Integer) {
          return font.deriveFont(((Integer)size).floatValue());
        }
        return font;
      }

      public static float overrideHeaderFontSizeOffset() {
        Object offset = UIManager.get("ToolWindow.overridden.header.font.size.offset");
        if (offset instanceof Integer) {
          return ((Integer)offset).floatValue();
        }

        return 0;
      }

      @Nonnull
      public static Color hoveredIconBackground() {
        return JBColor.namedColor("ToolWindow.HeaderCloseButton.background", JBColor.namedColor("ToolWindow.header.closeButton.background", 0xB9B9B9));
      }

      @Nonnull
      public static Icon closeTabIcon(boolean hovered) {
        return hovered ? JBUI.getIcon("ToolWindow.header.closeButton.hovered.icon", AllIcons.Actions.CloseHovered) : JBUI.getIcon("ToolWindow.header.closeButton.icon", AllIcons.Actions.Close);
      }

      @Nonnull
      public static Icon comboTabIcon(boolean hovered) {
        return hovered ? JBUI.getIcon("ToolWindow.header.comboButton.hovered.icon", AllIcons.General.ArrowDown) : JBUI.getIcon("ToolWindow.header.comboButton.icon", AllIcons.General.ArrowDown);
      }
    }

    public static class Label {
      @Nonnull
      public static Color foreground(boolean selected) {
        return selected ? JBColor.namedColor("Label.selectedForeground", 0xFFFFFF) : JBColor.namedColor("Label.foreground", 0x000000);
      }

      @Nonnull
      public static Color foreground() {
        return foreground(false);
      }

      @Nonnull
      public static Color disabledForeground(boolean selected) {
        return selected ? JBColor.namedColor("Label.selectedDisabledForeground", 0x999999) : JBColor.namedColor("Label.disabledForeground", JBColor.namedColor("Label.disabledText", 0x999999));
      }

      @Nonnull
      public static Color disabledForeground() {
        return disabledForeground(false);
      }
    }

    public static class Popup {
      public static Color headerBackground(boolean active) {
        return active ? JBColor.namedColor("Popup.Header.activeBackground", 0xe6e6e6) : JBColor.namedColor("Popup.Header.inactiveBackground", 0xededed);
      }

      public static int headerHeight(boolean hasControls) {
        return hasControls ? JBUI.scale(28) : JBUI.scale(24);
      }

      public static Color borderColor(boolean active) {
        return active
               ? JBColor.namedColor("Popup.borderColor", JBColor.namedColor("Popup.Border.color", 0x808080))
               : JBColor.namedColor("Popup.inactiveBorderColor", JBColor.namedColor("Popup.inactiveBorderColor", 0xaaaaaa));
      }

      public static Color toolbarPanelColor() {
        return JBColor.namedColor("Popup.Toolbar.background", 0xf7f7f7);
      }

      public static Color toolbarBorderColor() {
        return JBColor.namedColor("Popup.Toolbar.borderColor", JBColor.namedColor("Popup.Toolbar.Border.color", 0xf7f7f7));
      }

      public static int toolbarHeight() {
        return JBUI.scale(28);
      }

      public static Color separatorColor() {
        return JBColor.namedColor("Popup.separatorColor", new JBColor(Color.gray.brighter(), Gray.x51));
      }

      public static Color separatorTextColor() {
        return JBColor.namedColor("Popup.separatorForeground", Color.gray);
      }
    }

    public static class Focus {
      private static final Color GRAPHITE_COLOR = new JBColor(new Color(0x8099979d, true), new Color(0x676869));

      @Nonnull
      public static Color focusColor() {
        return UIUtil.isGraphite() ? GRAPHITE_COLOR : JBColor.namedColor("Component.focusColor", JBColor.namedColor("Focus.borderColor", 0x8ab2eb));
      }

      @Nonnull
      public static Color defaultButtonColor() {
        return UIUtil.isUnderDarcula() ? JBColor.namedColor("Button.default.focusColor", JBColor.namedColor("Focus.defaultButtonBorderColor", 0x97c3f3)) : focusColor();
      }

      @Nonnull
      public static Color errorColor(boolean active) {
        return active
               ? JBColor.namedColor("Component.errorFocusColor", JBColor.namedColor("Focus.activeErrorBorderColor", 0xe53e4d))
               : JBColor.namedColor("Component.inactiveErrorFocusColor", JBColor.namedColor("Focus.inactiveErrorBorderColor", 0xebbcbc));
      }

      @Nonnull
      public static Color warningColor(boolean active) {
        return active
               ? JBColor.namedColor("Component.warningFocusColor", JBColor.namedColor("Focus.activeWarningBorderColor", 0xe2a53a))
               : JBColor.namedColor("Component.inactiveWarningFocusColor", JBColor.namedColor("Focus.inactiveWarningBorderColor", 0xffd385));
      }
    }

    public static class TabbedPane {
      public static final Color ENABLED_SELECTED_COLOR = JBColor.namedColor("TabbedPane.underlineColor", JBColor.namedColor("TabbedPane.selectedColor", 0x4083C9));
      public static final Color DISABLED_SELECTED_COLOR = JBColor.namedColor("TabbedPane.disabledUnderlineColor", JBColor.namedColor("TabbedPane.selectedDisabledColor", Gray.xAB));
      public static final Color DISABLED_TEXT_COLOR = JBColor.namedColor("TabbedPane.disabledForeground", JBColor.namedColor("TabbedPane.disabledText", Gray.x99));
      public static final Color HOVER_COLOR = JBColor.namedColor("TabbedPane.hoverColor", Gray.xD9);
      public static final Color FOCUS_COLOR = JBColor.namedColor("TabbedPane.focusColor", 0xDAE4ED);
      public static final JBValue TAB_HEIGHT = new JBValue.UIInteger("TabbedPane.tabHeight", 32);
      public static final JBValue SELECTION_HEIGHT = new JBValue.UIInteger("TabbedPane.tabSelectionHeight", 3);
    }

    public static class BigPopup {
      @Nonnull
      public static Color headerBackground() {
        return JBColor.namedColor("SearchEverywhere.Header.background", 0xf2f2f2);
      }

      @Nonnull
      public static Insets tabInsets() {
        return JBUI.insets(0, 12);
      }

      @Nonnull
      public static Color selectedTabColor() {
        return JBColor.namedColor("SearchEverywhere.Tab.selectedBackground", 0xdedede);
      }

      @Nonnull
      public static Color selectedTabTextColor() {
        return JBColor.namedColor("SearchEverywhere.Tab.selectedForeground", 0x000000);
      }

      @Nonnull
      public static Color searchFieldBackground() {
        return JBColor.namedColor("SearchEverywhere.SearchField.background", 0xffffff);
      }

      @Nonnull
      public static Color searchFieldBorderColor() {
        return JBColor.namedColor("SearchEverywhere.SearchField.borderColor", 0xbdbdbd);
      }

      @Nonnull
      public static Insets searchFieldInsets() {
        return JBUI.insets(0, 6, 0, 5);
      }

      public static int maxListHeight() {
        return JBUI.scale(600);
      }

      @Nonnull
      public static Color listSeparatorColor() {
        return JBColor.namedColor("SearchEverywhere.List.separatorColor", Gray.xDC);
      }

      @Nonnull
      public static Color listTitleLabelForeground() {
        return JBColor.namedColor("SearchEverywhere.List.separatorForeground", UIUtil.getLabelDisabledForeground());
      }

      @Nonnull
      public static Color searchFieldGrayForeground() {
        return JBColor.namedColor("SearchEverywhere.SearchField.infoForeground", JBColor.GRAY);
      }

      @Nonnull
      public static Color advertiserForeground() {
        return JBColor.namedColor("SearchEverywhere.Advertiser.foreground", JBColor.GRAY);
      }

      @Nonnull
      public static Border advertiserBorder() {
        return new JBEmptyBorder(JBUI.insets("SearchEverywhere.Advertiser.foreground", JBUI.insetsLeft(8)));
      }

      @Nonnull
      public static Color advertiserBackground() {
        return JBColor.namedColor("SearchEverywhere.Advertiser.background", 0xf2f2f2);
      }
    }

    public static class Advertiser {
      private static final JBInsets DEFAULT_AD_INSETS = JBUI.insets(1, 5);

      @Nonnull
      public static Color foreground() {
        Color foreground = CurrentTheme.BigPopup.advertiserForeground();
        return JBColor.namedColor("Popup.Advertiser.foreground", foreground);
      }

      @Nonnull
      public static Color background() {
        Color background = CurrentTheme.BigPopup.advertiserBackground();
        return JBColor.namedColor("Popup.Advertiser.background", background);
      }

      @Nonnull
      public static Border border() {
        return new JBEmptyBorder(JBUI.insets("Popup.Advertiser.borderInsets", DEFAULT_AD_INSETS));
      }

      @Nonnull
      public static Color borderColor() {
        return JBColor.namedColor("Popup.Advertiser.borderColor", Gray._135);
      }
    }

    public static class Validator {
      @Nonnull
      public static Color errorBorderColor() {
        return JBColor.namedColor("ValidationTooltip.errorBorderColor", 0xE0A8A9);
      }

      @Nonnull
      public static Color errorBackgroundColor() {
        return JBColor.namedColor("ValidationTooltip.errorBackground", JBColor.namedColor("ValidationTooltip.errorBackgroundColor", 0xF5E6E7));
      }

      @Nonnull
      public static Color warningBorderColor() {
        return JBColor.namedColor("ValidationTooltip.warningBorderColor", 0xE0CEA8);
      }

      @Nonnull
      public static Color warningBackgroundColor() {
        return JBColor.namedColor("ValidationTooltip.warningBackground", JBColor.namedColor("ValidationTooltip.warningBackgroundColor", 0xF5F0E6));
      }
    }

    public static class Link {
      @Nonnull
      public static Color linkColor() {
        return JBColor.namedColor("Link.activeForeground", JBColor.namedColor("link.foreground", 0x589df6));
      }

      @Nonnull
      public static Color linkHoverColor() {
        return JBColor.namedColor("Link.hoverForeground", JBColor.namedColor("link.hover.foreground", linkColor()));
      }

      @Nonnull
      public static Color linkPressedColor() {
        return JBColor.namedColor("Link.pressedForeground", JBColor.namedColor("link.pressed.foreground", new JBColor(0xf00000, 0xba6f25)));
      }

      @Nonnull
      public static Color linkVisitedColor() {
        return JBColor.namedColor("Link.visitedForeground", JBColor.namedColor("link.visited.foreground", new JBColor(0x800080, 0x9776a9)));
      }
    }

    public static class Arrow {
      @Nonnull
      public static Color foregroundColor(boolean enabled) {
        return enabled
               ? JBColor.namedColor("ComboBox.ArrowButton.iconColor", JBColor.namedColor("ComboBox.darcula.arrowButtonForeground", Gray.x66))
               : JBColor.namedColor("ComboBox.ArrowButton.disabledIconColor", JBColor.namedColor("ComboBox.darcula.arrowButtonDisabledForeground", Gray.xAB));

      }

      @Nonnull
      public static Color backgroundColor(boolean enabled, boolean editable) {
        return enabled ? editable
                         ? JBColor.namedColor("ComboBox.ArrowButton.background", JBColor.namedColor("ComboBox.darcula.editable.arrowButtonBackground", Gray.xFC))
                         : JBColor.namedColor("ComboBox.ArrowButton.nonEditableBackground", JBColor.namedColor("ComboBox.darcula.arrowButtonBackground", Gray.xFC)) : UIUtil.getPanelBackground();
      }
    }

    public static class NewClassDialog {
      @Nonnull
      public static Color searchFieldBackground() {
        return JBColor.namedColor("NewClass.SearchField.background", 0xffffff);
      }

      @Nonnull
      public static Color panelBackground() {
        return JBColor.namedColor("NewClass.Panel.background", 0xf2f2f2);
      }

      @Nonnull
      public static Color bordersColor() {
        return JBColor.namedColor("TextField.borderColor", JBColor.namedColor("Component.borderColor", new JBColor(0xbdbdbd, 0x646464)));
      }

      @Nonnull
      public static int fieldsSeparatorWidth() {
        return JBUI.getInt("NewClass.separatorWidth", JBUI.scale(10));
      }
    }
  }
}
