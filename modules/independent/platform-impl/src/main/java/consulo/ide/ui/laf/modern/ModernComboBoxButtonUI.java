/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.ui.laf.modern;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.actionSystem.ex.ComboBoxButton;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.actionSystem.ex.ComboBoxButtonUI;
import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18-Nov-16.
 */
public class ModernComboBoxButtonUI extends ComboBoxButtonUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernComboBoxButtonUI((ComboBoxButton)c);
  }

  public ModernComboBoxButtonUI(ComboBoxButton comboBoxButton) {
    super(comboBoxButton);
  }

  @Override
  protected void setupBorder(ComboBoxButton comboBoxButton) {
    comboBoxButton.setBorder(new AbstractBorder() {
      @Override
      public Insets getBorderInsets(Component c) {
        final Insets insets = super.getBorderInsets(c);
        //noinspection UseDPIAwareInsets
        return new Insets(insets.top, insets.left + JBUI.scale(4), insets.bottom, insets.right + JBUI.scale(20));
      }
    });
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    ComboBoxButton comboBoxButton = (ComboBoxButton)c;
    ComboBoxAction comboBoxAction = comboBoxButton.getComboBoxAction();

    final boolean isEmpty = comboBoxButton.getIcon() == null && StringUtil.isEmpty(comboBoxButton.getText());
    int width = isEmpty ? JBUI.scale(18) : super.getPreferredSize(c).width;
    if (comboBoxAction.isSmallVariant()) width += JBUI.scale(4);
    return new Dimension(width, comboBoxAction.isSmallVariant() ? JBUI.scale(21) : super.getPreferredSize(c).height);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    ComboBoxButton comboBoxButton = (ComboBoxButton)c;
    ComboBoxAction comboBoxAction = comboBoxButton.getComboBoxAction();
    Color borderColor = ModernUIUtil.getBorderColor(comboBoxButton);

    UISettings.setupAntialiasing(g);
    final Dimension size = comboBoxButton.getSize();
    final boolean isEmpty = comboBoxButton.getIcon() == null && StringUtil.isEmpty(comboBoxButton.getText());

    final Color textColor = comboBoxButton.isEnabled() ? UIManager.getColor("Panel.foreground") : UIUtil.getInactiveTextColor();
    if (comboBoxButton.isForceTransparent()) {
      final Icon icon = comboBoxButton.getIcon();
      int x = JBUI.scale(7);
      if (icon != null) {
        icon.paintIcon(c, g, x, (size.height - icon.getIconHeight()) / 2);
        x += icon.getIconWidth() + JBUI.scale(3);
      }
      if (!StringUtil.isEmpty(comboBoxButton.getText())) {
        final Font font = comboBoxButton.getFont();
        g.setFont(font);
        g.setColor(textColor);
        g.drawString(comboBoxButton.getText(), x, (size.height + font.getSize()) / 2 - JBUI.scale(1));
      }
    }
    else {
      if (comboBoxAction.isSmallVariant()) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setColor(UIUtil.getControlColor());
        final int w = comboBoxButton.getWidth();
        final int h = comboBoxButton.getHeight();
        if (comboBoxButton.isEnabled()) {
          g.setColor(DefaultLookup.getColor(comboBoxButton, this, "ComboBox.background", null));
        }
        else {
          g.setColor(DefaultLookup.getColor(comboBoxButton, this, "ComboBox.disabledBackground", null));
        }
        g2.fillRect(JBUI.scale(2), 0, w - JBUI.scale(2), h);

        g2.setColor(myMouseInside ? ModernUIUtil.getSelectionBackground() : borderColor);
        g2.drawRect(0, 0, w - JBUI.scale(2), h - JBUI.scale(1));

        final Icon icon = comboBoxButton.getIcon();
        int x = JBUI.scale(7);
        if (icon != null) {
          icon.paintIcon(c, g, x, (size.height - icon.getIconHeight()) / 2);
          x += icon.getIconWidth() + JBUI.scale(3);
        }
        if (!StringUtil.isEmpty(comboBoxButton.getText())) {
          final Font font = comboBoxButton.getFont();
          g2.setFont(font);
          g2.setColor(textColor);
          g2.drawString(comboBoxButton.getText(), x, (size.height + font.getSize()) / 2 - JBUI.scale(1));
        }
      }
      else {
        super.paint(g, c);
      }
    }

    int width = JBUI.scale(18);
    int x;
    if (isEmpty) {
      x = (size.width - width) / 2;
    }
    else {
      if (comboBoxAction.isSmallVariant()) {
        x = size.width - width;
      }
      else {
        x = size.width - width;
      }
    }

    GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    g.setColor(comboBoxButton.isEnabled() ? comboBoxButton.getForeground() : borderColor);
    g.drawLine(x + JBUI.scale(3), JBUI.scale(8), x + JBUI.scale(7), JBUI.scale(12));
    g.drawLine(x + JBUI.scale(7), JBUI.scale(12), x + JBUI.scale(11), JBUI.scale(8));
    config.restore();
    g.setPaintMode();
  }
}
