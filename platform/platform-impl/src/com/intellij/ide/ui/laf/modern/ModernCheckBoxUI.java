/*
 * Copyright 2013-2014 must-be.org
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

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.Gray;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;

/**
 * @author VISTALL
 * @since 02.08.14
 * <p/>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaCheckBoxUI}
 */
public class ModernCheckBoxUI extends BasicCheckBoxUI {
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(JComponent c) {
    if (UIUtil.getParentOfType(CellRendererPane.class, c) != null) {
      c.setBorder(null);
    }
    return new ModernCheckBoxUI(c);
  }

  private MouseEnterHandler myMouseEnterHandler;

  public ModernCheckBoxUI(final JComponent c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  public synchronized void paint(Graphics g2d, JComponent c) {
    Graphics2D g = (Graphics2D)g2d;
    JCheckBox b = (JCheckBox)c;
    final ButtonModel model = b.getModel();
    final Dimension size = c.getSize();
    final Font font = c.getFont();

    g.setFont(font);
    FontMetrics fm = SwingUtilities2.getFontMetrics(c, g, font);

    Rectangle viewRect = new Rectangle(size);
    Rectangle iconRect = new Rectangle();
    Rectangle textRect = new Rectangle();

    Insets i = c.getInsets();
    viewRect.x += i.left;
    viewRect.y += i.top;
    viewRect.width -= (i.right + viewRect.x);
    viewRect.height -= (i.bottom + viewRect.y);

    String text = SwingUtilities
            .layoutCompoundLabel(c, fm, b.getText(), getDefaultIcon(), b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                                 b.getHorizontalTextPosition(), viewRect, iconRect, textRect, b.getIconTextGap());

    //background
    if (c.isOpaque()) {
      g.setColor(b.getBackground());
      g.fillRect(0, 0, size.width, size.height);
    }

    if (b.isSelected() && b.getSelectedIcon() != null) {
      b.getSelectedIcon().paintIcon(b, g, iconRect.x + 4, iconRect.y + 2);
    }
    else if (!b.isSelected() && b.getIcon() != null) {
      b.getIcon().paintIcon(b, g, iconRect.x + 4, iconRect.y + 2);
    }
    else {
      final int x = iconRect.x + 3;
      final int y = iconRect.y + 3;
      final int w = iconRect.width - 6;
      final int h = iconRect.height - 6;

      g.translate(x, y);
      g.setColor(b.getBackground());
      g.fillRect(1, 1, w - 2, h - 2);

      //setup AA for lines
      final GraphicsConfig config = new GraphicsConfig(g);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

      final boolean armed = b.getModel().isArmed();

      if (myMouseEnterHandler.isMouseEntered()) {
        g.setColor(getFocusedBackgroundColor1(armed));
        g.fillRect(0, 0, w, h);

        g.setColor(ModernUIUtil.getSelectionBackground());
        g.drawRect(0, 0, w, h - 1);
      }
      else {
        g.setPaint(ModernUIUtil.getBorderColor(c));
        g.drawRect(0, 0, w, h - 1);

        g.setPaint(getInactiveFillColor());
        g.drawRect(0, 0, w, h - 1);
      }

      if (b.getModel().isSelected()) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.setPaint(getCheckSignColor(b.isEnabled()));
        g.drawLine(4, 7, 7, 10);
        g.drawLine(7, 10, 11, 3);
      }
      g.translate(-x, -y);
      config.restore();
    }

    //text
    if (text != null) {
      View view = (View)c.getClientProperty(BasicHTML.propertyKey);
      if (view != null) {
        view.paint(g, textRect);
      }
      else {
        g.setColor(model.isEnabled() ? b.getForeground() : UIManager.getColor("CheckBox.disabledText"));
        SwingUtilities2.drawStringUnderlineCharAt(c, g, text, b.getDisplayedMnemonicIndex(), textRect.x, textRect.y + fm.getAscent());
      }
    }
  }

  @Override
  protected void paintFocus(Graphics g, Rectangle t, Dimension d) {
    g.setColor(UIManager.getColor("CheckBox.focus"));
    g.drawRect(t.x - 1, t.y - 1, t.width + 1, t.height + 1);
  }

  protected Color getInactiveFillColor() {
    return getColor("inactiveFillColor", Gray._40.withAlpha(180));
  }

  protected Color getBorderColor1(boolean enabled) {
    return enabled ? getColor("borderColor1", Gray._120.withAlpha(0x5a)) : getColor("disabledBorderColor1", Gray._120.withAlpha(90));
  }

  protected Color getBackgroundColor1() {
    return getColor("backgroundColor1", Gray._110);
  }

  protected Color getCheckSignColor(boolean enabled) {
    return enabled ? getColor("checkSignColor", Gray._170) : getColor("checkSignColorDisabled", Gray._120);
  }

  protected Color getShadowColor(boolean enabled) {
    return enabled ? getColor("shadowColor", Gray._30) : getColor("shadowColorDisabled", Gray._60);
  }

  protected Color getFocusedBackgroundColor1(boolean armed) {
    return armed ? getColor("focusedArmed.backgroundColor1", Gray._100) : getColor("focused.backgroundColor1", Gray._120);
  }

  protected static Color getColor(String shortPropertyName, Color defaultValue) {
    final Color color = UIManager.getColor("CheckBox.darcula." + shortPropertyName);
    return color == null ? defaultValue : color;
  }

  @Override
  public Icon getDefaultIcon() {
    return new IconUIResource(EmptyIcon.create(20));
  }
}
