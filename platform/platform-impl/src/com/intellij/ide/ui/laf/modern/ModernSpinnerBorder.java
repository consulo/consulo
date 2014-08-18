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
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18.08.14
 * <p/>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaSpinnerBorder}
 */
public class ModernSpinnerBorder implements Border, UIResource {
  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    final JSpinner spinner = (JSpinner)c;
    final JFormattedTextField editor = UIUtil.findComponentOfType(spinner, JFormattedTextField.class);
    final int x1 = x + 3;
    final int y1 = y + 3;
    final int width1 = width - 8;
    final int height1 = height - 6;
    final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);

    if (c.isOpaque()) {
      g.setColor(UIUtil.getPanelBackground());
      g.fillRect(x, y, width, height);
    }

    g.setColor(UIUtil.getTextFieldBackground());
    g.fillRoundRect(x1, y1, width1, height1, 5, 5);
    g.setColor(UIUtil.getPanelBackground());
    if (editor != null) {
      final int off = editor.getBounds().x + editor.getWidth() + ((JSpinner)c).getInsets().left + 1;
      g.fillRect(off, y1, 17, height1);
      g.setColor(Gray._100);
      g.drawLine(off, y1, off, height1 + 2);
    }

    if (!c.isEnabled()) {
      ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
    }

    ComponentUI componentUI = ModernUIUtil.getUI(c);
    if (componentUI instanceof ModernTextBorder.ModernTextUI) {
      if (((ModernTextBorder.ModernTextUI)componentUI).isFocused()) {
        g.setColor(ModernUIUtil.getSelectionBackground());
      }
      else if (((ModernTextBorder.ModernTextUI)componentUI).getMouseEnterHandler().isMouseEntered()) {
        g.setColor(ModernUIUtil.getActiveBorderColor());
      }
      else {
        g.setColor(ModernUIUtil.getDisabledBorderColor());
      }
    }
    else {
      g.setColor(ModernUIUtil.getActiveBorderColor());
    }
    g.drawRect(x1, y1, width1, height1);
    config.restore();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return new InsetsUIResource(6, 7, 6, 7);
  }

  @Override
  public boolean isBorderOpaque() {
    return true;
  }
}
