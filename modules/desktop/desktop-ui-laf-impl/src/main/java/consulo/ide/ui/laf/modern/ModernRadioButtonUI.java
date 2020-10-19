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

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.text.View;
import java.awt.*;

/**
 * @author VISTALL
 * @since 02.08.14
 *
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaRadioButtonUI}
 */
public class ModernRadioButtonUI extends BasicRadioButtonUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernRadioButtonUI(c);
  }

  private final MouseEnterHandler myMouseEnterHandler;

  public ModernRadioButtonUI(final JComponent c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    myMouseEnterHandler.replace(null, c);
  }

  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
    myMouseEnterHandler.replace(c, null);
  }

  @Override
  public synchronized void paint(Graphics g2d, JComponent c) {
    Graphics2D g = (Graphics2D)g2d;
    AbstractButton b = (AbstractButton) c;
    ButtonModel model = b.getModel();

    Dimension size = c.getSize();
    Font f = c.getFont();
    g.setFont(f);
    FontMetrics fm = c.getFontMetrics(f);

    Rectangle viewRect = new Rectangle(size);
    Rectangle iconRect = new Rectangle();
    Rectangle textRect = new Rectangle();

    Insets i = c.getInsets();
    viewRect.x += i.left;
    viewRect.y += i.top;
    viewRect.width -= (i.right + viewRect.x);
    viewRect.height -= (i.bottom + viewRect.y);


    String text = SwingUtilities.layoutCompoundLabel(
            c, fm, b.getText(), getDefaultIcon(),
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect, b.getIconTextGap());

    // fill background
    if(c.isOpaque()) {
      g.setColor(b.getBackground());
      g.fillRect(0,0, size.width, size.height);
    }

    int rad = JBUI.scale(5);

    // Paint the radio button
    final int x = iconRect.x + (rad - JBUI.scale(1)) / 2;
    final int y = iconRect.y + (rad - JBUI.scale(1)) / 2;
    final int w = iconRect.width - rad;
    final int h = iconRect.height - rad;

    g.translate(x, y);

    //setup AA for lines
    final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    g.setColor(c.getBackground());
    g.fillOval(0, JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1));

    if (myMouseEnterHandler.isMouseEntered()) {
      g.setColor(ModernUIUtil.getSelectionBackground());
      g.drawOval(0, JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1));
    }
    else {
      g.setPaint(ModernUIUtil.getBorderColor(c));
      g.drawOval(0, JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1));
    }

    if (b.isSelected()) {
      final boolean enabled = b.isEnabled();
      g.setColor(UIManager.getColor(enabled ? "RadioButton.darcula.selectionEnabledColor" : "RadioButton.darcula.selectionDisabledColor")); //Gray._170 : Gray._120);
      g.fillOval(w/2 - rad/2, h / 2 - Math.round(JBUI.scale(1f)), rad, rad);
    }
    config.restore();
    g.translate(-x, -y);

    // Draw the Text
    if(text != null) {
      View v = (View) c.getClientProperty(BasicHTML.propertyKey);
      if (v != null) {
        v.paint(g, textRect);
      } else {
        int mnemIndex = b.getDisplayedMnemonicIndex();
        if(model.isEnabled()) {
          // *** paint the text normally
          g.setColor(b.getForeground());
        } else {
          // *** paint the text disabled
          g.setColor(UIManager.getColor("RadioButton.disabledText"));
        }
        BasicGraphicsUtils.drawStringUnderlineCharAt(c, g, text, mnemIndex, textRect.x, textRect.y + fm.getAscent());
      }
    }
  }

  @Override
  public Icon getDefaultIcon() {
    return new IconUIResource(JBUI.emptyIcon(20));
  }
}
