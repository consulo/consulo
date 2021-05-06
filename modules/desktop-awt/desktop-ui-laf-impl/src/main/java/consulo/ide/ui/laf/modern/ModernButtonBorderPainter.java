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

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author VISTALL
 * @since 02.08.14
 * <p/>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaButtonPainter}
 */
public class ModernButtonBorderPainter implements Border, UIResource {
  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    if (isDefaultButton(c) || UIUtil.isHelpButton(c)) {
      return;
    }

    final Insets ins = getBorderInsets(c);
    final int yOff = (ins.top + ins.bottom) / 4;
    final boolean square = DarculaButtonUI.isSquare(c);
    int offset = JBUI.scale(square ? 1 : 4);

    ModernButtonUI ui = ModernUIUtil.getUI(c);
    MouseEnterHandler mouseEnterHandler = ui.getMouseEnterHandler();
    if (mouseEnterHandler.isMousePressed()) {
      g.setColor(ModernUIUtil.getActiveBorderColor());
    }
    else {
      if (isDefaultButton(c)) {
        g.setColor(c.isEnabled() ? ModernUIUtil.getSelectionBackground() : ModernUIUtil.getDisabledBorderColor());
      }
      else {
        if (c.isEnabled()) {
          g.setColor(mouseEnterHandler.isMouseEntered() ? ModernUIUtil.getSelectionBackground() : ModernUIUtil.getActiveBorderColor());
        }
        else {
          g.setColor(ModernUIUtil.getDisabledBorderColor());
        }
      }
    }

    g.drawRect(x + offset, y + yOff, width - 2 * offset, height - 2 * yOff);
  }

  public static boolean isDefaultButton(Object component) {
    return component instanceof JButton && ((JButton)component).isDefaultButton();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    if (DarculaButtonUI.isSquare(c)) {
      return JBUI.insets(2, 0).asUIResource();
    }
    return JBUI.insets(8, 16).asUIResource();
  }


  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}
