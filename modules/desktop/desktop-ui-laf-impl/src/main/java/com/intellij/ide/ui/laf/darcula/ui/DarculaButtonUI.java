/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import sun.swing.SwingUtilities2;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class
DarculaButtonUI extends BasicButtonUI {
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(JComponent c) {
    return new DarculaButtonUI();
  }

  public static boolean isSquare(Component c) {
    return c instanceof JButton && "square".equals(((JButton)c).getClientProperty("JButton.buttonType"));
  }

  public static boolean isDefaultButton(JComponent c) {
    return c instanceof JButton && ((JButton)c).isDefaultButton();
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    final Border border = c.getBorder();
    final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    final boolean square = isSquare(c);
    if (c.isEnabled() && border != null) {
      final Insets ins = border.getBorderInsets(c);
      final int yOff = (ins.top + ins.bottom) / 4;
      if (!square) {
        if (((JButton)c).isDefaultButton()) {
          ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, getSelectedButtonColor1(), 0, c.getHeight(), getSelectedButtonColor2()));
        }
        else {
          ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, getButtonColor1(), 0, c.getHeight(), getButtonColor2()));
        }
      }
      g.fillRoundRect(JBUI.scale(square ? 2 : 4), yOff, c.getWidth() - JBUI.scale(8), c.getHeight() - 2 * yOff, JBUI.scale(square ? 3 : 5),
                      JBUI.scale(square ? 3 : 5));
    }
    config.restore();
    super.paint(g, c);
  }

  @Override
  protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
    AbstractButton button = (AbstractButton)c;
    ButtonModel model = button.getModel();
    Color fg = button.getForeground();
    if (fg instanceof UIResource && button instanceof JButton && ((JButton)button).isDefaultButton()) {
      final Color selectedFg = UIManager.getColor("Button.darcula.selectedButtonForeground");
      if (selectedFg != null) {
        fg = selectedFg;
      }
    }
    g.setColor(fg);

    FontMetrics metrics = SwingUtilities2.getFontMetrics(c, g);
    int mnemonicIndex = button.getDisplayedMnemonicIndex();
    if (model.isEnabled()) {

      SwingUtilities2
              .drawStringUnderlineCharAt(c, g, text, mnemonicIndex, textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());
    }
    else {
      g.setColor(UIManager.getColor("Button.darcula.disabledText.shadow"));
      SwingUtilities2.drawStringUnderlineCharAt(c, g, text, -1, textRect.x + getTextShiftOffset() + JBUI.scale(1),
                                                textRect.y + metrics.getAscent() + getTextShiftOffset() + JBUI.scale(1));
      g.setColor(UIManager.getColor("Button.disabledText"));
      SwingUtilities2.drawStringUnderlineCharAt(c, g, text, -1, textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());


    }
  }

  @Override
  public void update(Graphics g, JComponent c) {
    super.update(g, c);
    if (((JButton)c).isDefaultButton() && !SystemInfo.isMac) {
      if (!c.getFont().isBold()) {
        c.setFont(c.getFont().deriveFont(Font.BOLD));
      }
    }
  }


  public static boolean isHelpButton(JComponent button) {
    return SystemInfo.isMac && button instanceof JButton && "help".equals(button.getClientProperty("JButton.buttonType"));
  }

  @Nonnull
  protected Color getButtonColor1() {
    return ObjectUtil.notNull(UIManager.getColor("Button.darcula.color1"), new ColorUIResource(0x555a5c));
  }

  @Nonnull
  protected Color getButtonColor2() {
    return ObjectUtil.notNull(UIManager.getColor("Button.darcula.color2"), new ColorUIResource(0x414648));
  }

  @Nonnull
  protected Color getSelectedButtonColor1() {
    return ObjectUtil.notNull(UIManager.getColor("Button.darcula.selection.color1"), new ColorUIResource(0x384f6b));
  }

  @Nonnull
  protected Color getSelectedButtonColor2() {
    return ObjectUtil.notNull(UIManager.getColor("Button.darcula.selection.color2"), new ColorUIResource(0x233143));
  }
}
