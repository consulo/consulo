/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide.ui.laf.modern;

import com.intellij.ide.ui.UISettings;
import com.intellij.ui.plaf.beg.BegResources;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicMenuUI;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public class ModernMenuUI extends BasicMenuUI {
  public static ComponentUI createUI(JComponent component) {
    return new ModernMenuUI();
  }

  private final Rectangle ourZeroRect = new Rectangle(0, 0, 0, 0);
  private final Rectangle ourTextRect = new Rectangle();
  private final Rectangle ourArrowIconRect = new Rectangle();
  private int myMaxGutterIconWidth;
  private int a;
  private Rectangle ourPreferredSizeRect = new Rectangle();
  private int k;
  private int e;
  private final Rectangle ourAcceleratorRect = new Rectangle();
  private final Rectangle ourCheckIconRect = new Rectangle();
  private Rectangle ourIconRect = new Rectangle();
  private final Rectangle ourViewRect = new Rectangle(32767, 32767);

  @Override
  protected void installDefaults() {
    super.installDefaults();
    Integer integer = UIUtil.getPropertyMaxGutterIconWidth(getPropertyPrefix());
    if (integer != null) {
      myMaxGutterIconWidth = JBUI.scale(integer.intValue());
    }
    arrowIcon = EmptyIcon.create(myMaxGutterIconWidth);
  }

  @Override
  public void paint(Graphics g, JComponent comp) {
    UISettings.setupAntialiasing(g);
    JMenu jMenu = (JMenu)comp;
    ButtonModel buttonmodel = jMenu.getModel();
    int mnemonicIndex = jMenu.getDisplayedMnemonicIndex();
    Icon icon = getIcon();
    Icon allowedIcon = getAllowedIcon();
    Insets insets = comp.getInsets();
    resetRects();
    ourViewRect.setBounds(0, 0, jMenu.getWidth(), jMenu.getHeight());
    ourViewRect.x += insets.left;
    ourViewRect.y += insets.top;
    ourViewRect.width -= insets.right + ourViewRect.x;
    ourViewRect.height -= insets.bottom + ourViewRect.y;
    Font font = g.getFont();
    Font font1 = comp.getFont();
    g.setFont(font1);
    FontMetrics fontmetrics = g.getFontMetrics(font1);
    String s1 = layoutMenuItem(fontmetrics, jMenu.getText(), icon, allowedIcon, arrowIcon, jMenu.getVerticalAlignment(), jMenu.getHorizontalAlignment(),
                               jMenu.getVerticalTextPosition(), jMenu.getHorizontalTextPosition(), ourViewRect, ourIconRect, ourTextRect, ourAcceleratorRect,
                               ourCheckIconRect, ourArrowIconRect, jMenu.getText() != null ? defaultTextIconGap : 0, defaultTextIconGap);
    Color color2 = g.getColor();
    if (comp.isOpaque()) {
      g.setColor(jMenu.getBackground());
      g.fillRect(0, 0, jMenu.getWidth(), jMenu.getHeight());
      if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
        g.setColor(selectionBackground);
        if (allowedIcon != null) {
          g.fillRect(k, 0, jMenu.getWidth() - k, jMenu.getHeight());
        }
        else {
          g.fillRect(0, 0, jMenu.getWidth(), jMenu.getHeight());
          g.setColor(selectionBackground);
        }
      }
      g.setColor(color2);
    }
    if (allowedIcon != null) {
      if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
        g.setColor(selectionForeground);
      }
      else {
        g.setColor(jMenu.getForeground());
      }
      if (isUseCheckAndArrow()) {
        allowedIcon.paintIcon(comp, g, ourCheckIconRect.x, ourCheckIconRect.y);
      }
      g.setColor(color2);
      if (menuItem.isArmed()) {
        drawIconBorder(g);
      }
    }
    if (icon != null) {
      if (!buttonmodel.isEnabled()) {
        icon = jMenu.getDisabledIcon();
      }
      else if (buttonmodel.isPressed() && buttonmodel.isArmed()) {
        icon = jMenu.getPressedIcon();
        if (icon == null) {
          icon = jMenu.getIcon();
        }
      }
      if (icon != null) {
        icon.paintIcon(comp, g, ourIconRect.x, ourIconRect.y);
      }
    }
    if (s1 != null && s1.length() > 0) {
      if (buttonmodel.isEnabled()) {
        if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
          g.setColor(selectionForeground);
        }
        else {
          g.setColor(jMenu.getForeground());
        }
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
      }
      else {
        final Object disabledForeground = UIUtil.getMenuItemDisabledForeground();
        if (disabledForeground instanceof Color) {
          g.setColor((Color)disabledForeground);
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
        }
        else {
          g.setColor(jMenu.getBackground().brighter());
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
          g.setColor(jMenu.getBackground().darker());
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x - 1, (ourTextRect.y + fontmetrics.getAscent()) - 1);
        }
      }
    }

    if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
      g.setColor(selectionForeground);
    }

    if (isUseCheckAndArrow()) {
      GraphicsUtil.setupAAPainting(g);
      int x = ourArrowIconRect.x;
      int y = ourArrowIconRect.y;

      g.drawLine(x + JBUI.scale(5), y + JBUI.scale(5), x + JBUI.scale(10), y + JBUI.scale(9));
      g.drawLine(x + JBUI.scale(10), y + JBUI.scale(9), x + JBUI.scale(5), y + JBUI.scale(13));
    }

    g.setColor(color2);
    g.setFont(font);
  }

  private boolean isUseCheckAndArrow() {
    return !((JMenu)menuItem).isTopLevelMenu();
  }

  @Override
  public MenuElement[] getPath() {
    MenuSelectionManager menuselectionmanager = MenuSelectionManager.defaultManager();
    MenuElement amenuelement[] = menuselectionmanager.getSelectedPath();
    int i1 = amenuelement.length;
    if (i1 == 0) {
      return new MenuElement[0];
    }
    Container container = menuItem.getParent();
    MenuElement amenuelement1[];
    if (amenuelement[i1 - 1].getComponent() == container) {
      amenuelement1 = new MenuElement[i1 + 1];
      System.arraycopy(amenuelement, 0, amenuelement1, 0, i1);
      amenuelement1[i1] = menuItem;
    }
    else {
      int j1;
      for (j1 = amenuelement.length - 1; j1 >= 0; j1--) {
        if (amenuelement[j1].getComponent() == container) {
          break;
        }
      }
      amenuelement1 = new MenuElement[j1 + 2];
      System.arraycopy(amenuelement, 0, amenuelement1, 0, j1 + 1);
      amenuelement1[j1 + 1] = menuItem;
    }
    return amenuelement1;
  }

  private String layoutMenuItem(FontMetrics fontmetrics,
                                String text,
                                Icon icon,
                                Icon checkIcon,
                                Icon arrowIcon,
                                int verticalAlignment,
                                int horizontalAlignment,
                                int verticalTextPosition,
                                int horizontalTextPosition,
                                Rectangle viewRect,
                                Rectangle iconRect,
                                Rectangle textRect,
                                Rectangle acceleratorRect,
                                Rectangle checkIconRect,
                                Rectangle arrowIconRect,
                                int textIconGap,
                                int menuItemGap) {
    SwingUtilities.layoutCompoundLabel(menuItem, fontmetrics, text, icon, verticalAlignment, horizontalAlignment, verticalTextPosition, horizontalTextPosition,
                                       viewRect, iconRect, textRect, textIconGap);
    acceleratorRect.width = acceleratorRect.height = 0;

    /* Initialize the checkIcon bounds rectangle's width & height.
     */
    if (isUseCheckAndArrow()) {
      if (checkIcon != null) {
        checkIconRect.width = checkIcon.getIconWidth();
        checkIconRect.height = checkIcon.getIconHeight();
      }
      else {
        checkIconRect.width = checkIconRect.height = 0;
      }

      /* Initialize the arrowIcon bounds rectangle width & height.
       */
      if (arrowIcon != null) {
        arrowIconRect.width = arrowIcon.getIconWidth();
        arrowIconRect.height = arrowIcon.getIconHeight();
      }
      else {
        arrowIconRect.width = arrowIconRect.height = 0;
      }
      textRect.x += myMaxGutterIconWidth;
      iconRect.x += myMaxGutterIconWidth;
    }
    textRect.x += menuItemGap;
    iconRect.x += menuItemGap;
    Rectangle labelRect = iconRect.union(textRect);

    // Position the Accelerator text rect

    acceleratorRect.x += viewRect.width - arrowIconRect.width - menuItemGap - acceleratorRect.width;
    acceleratorRect.y = (viewRect.y + viewRect.height / 2) - acceleratorRect.height / 2;

    // Position the Check and Arrow Icons

    if (isUseCheckAndArrow()) {
      arrowIconRect.x += viewRect.width - arrowIconRect.width;
      arrowIconRect.y = (viewRect.y + labelRect.height / 2) - arrowIconRect.height / 2;
      if (checkIcon != null) {
        checkIconRect.y = (viewRect.y + labelRect.height / 2) - checkIconRect.height / 2;
        checkIconRect.x += (viewRect.x + myMaxGutterIconWidth / 2) - checkIcon.getIconWidth() / 2;
        a = viewRect.x;
        e = (viewRect.y + labelRect.height / 2) - myMaxGutterIconWidth / 2;
        k = viewRect.x + myMaxGutterIconWidth + JBUI.scale(2);
      }
      else {
        checkIconRect.x = checkIconRect.y = 0;
      }
    }
    return text;
  }

  private Icon getIcon() {
    Icon icon = menuItem.getIcon();
    if (icon != null && getAllowedIcon() != null) {
      icon = null;
    }
    return icon;
  }

  @Override
  protected Dimension getPreferredMenuItemSize(JComponent comp, Icon checkIcon, Icon arrowIcon, int defaultTextIconGap) {
    JMenu jMenu = (JMenu)comp;
    Icon icon1 = getIcon();
    Icon icon2 = getAllowedIcon();
    String text = jMenu.getText();
    Font font = jMenu.getFont();
    FontMetrics fontmetrics = jMenu.getToolkit().getFontMetrics(font);
    resetRects();
    layoutMenuItem(fontmetrics, text, icon1, icon2, arrowIcon, jMenu.getVerticalAlignment(), jMenu.getHorizontalAlignment(), jMenu.getVerticalTextPosition(),
                   jMenu.getHorizontalTextPosition(), ourViewRect, ourIconRect, ourTextRect, ourAcceleratorRect, ourCheckIconRect, ourArrowIconRect,
                   text != null ? defaultTextIconGap : 0, defaultTextIconGap);
    ourPreferredSizeRect.setBounds(ourTextRect);
    ourPreferredSizeRect = SwingUtilities.computeUnion(ourIconRect.x, ourIconRect.y, ourIconRect.width, ourIconRect.height, ourPreferredSizeRect);
    if (isUseCheckAndArrow()) {
      ourPreferredSizeRect.width += myMaxGutterIconWidth;
      ourPreferredSizeRect.width += defaultTextIconGap;
      ourPreferredSizeRect.width += defaultTextIconGap;
      ourPreferredSizeRect.width += ourArrowIconRect.width;
    }
    ourPreferredSizeRect.width += 2 * defaultTextIconGap;
    Insets insets = jMenu.getInsets();
    if (insets != null) {
      ourPreferredSizeRect.width += insets.left + insets.right;
      ourPreferredSizeRect.height += insets.top + insets.bottom;
    }
    if (ourPreferredSizeRect.width % 2 == 0) {
      ourPreferredSizeRect.width++;
    }
    if (ourPreferredSizeRect.height % 2 == 0) {
      ourPreferredSizeRect.height++;
    }
    return ourPreferredSizeRect.getSize();
  }

  private void drawIconBorder(Graphics g) {
    int i1 = a - JBUI.scale(1);
    int j1 = e - JBUI.scale(2);
    int k1 = i1 + myMaxGutterIconWidth + JBUI.scale(1);
    int l1 = j1 + myMaxGutterIconWidth + JBUI.scale(4);
    g.setColor(BegResources.m);
    UIUtil.drawLine(g, i1, j1, i1, l1);
    UIUtil.drawLine(g, i1, j1, k1, j1);
    g.setColor(BegResources.j);
    UIUtil.drawLine(g, k1, j1, k1, l1);
    UIUtil.drawLine(g, i1, l1, k1, l1);
  }

  private void resetRects() {
    ourIconRect.setBounds(ourZeroRect);
    ourTextRect.setBounds(ourZeroRect);
    ourAcceleratorRect.setBounds(ourZeroRect);
    ourCheckIconRect.setBounds(ourZeroRect);
    ourArrowIconRect.setBounds(ourZeroRect);
    ourViewRect.setBounds(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
    ourPreferredSizeRect.setBounds(ourZeroRect);
  }

  private Icon getAllowedIcon() {
    Icon icon = menuItem.isEnabled() ? menuItem.getIcon() : menuItem.getDisabledIcon();
    if (icon != null && icon.getIconWidth() > myMaxGutterIconWidth) {
      icon = null;
    }
    return icon;
  }

  @Override
  public void update(Graphics g, JComponent comp) {
    paint(g, comp);
  }
}
