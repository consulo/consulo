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

import consulo.desktop.ui.laf.idea.darcula.DarculaUIUtil;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.util.ui.JBUI;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author VISTALL
 * @since 05.08.14
 * <p>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder}
 */
public class ModernTextBorder implements Border, UIResource {
  public static interface ModernTextUI {
    boolean isFocused();

    @Nonnull
    MouseEnterHandler getMouseEnterHandler();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    if(DarculaUIUtil.isTableCellEditor(c)) {
      return JBUI.insets(2, 7, 2, 7).asUIResource();
    }

    int vOffset = c instanceof JPasswordField ? 3 : c instanceof JSpinner ? 6 : 4;
    return JBUI.insets(vOffset, 7, vOffset, 7).asUIResource();
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }

  @Override
  public void paintBorder(Component c, Graphics g2, int x, int y, int width, int height) {
    if(DarculaUIUtil.isTableCellEditor(c)) {
      return;
    }

    if (c.getParent() instanceof JComboBox) {
      return;
    }

    Graphics2D g = ((Graphics2D)g2);
    final GraphicsConfig config = new GraphicsConfig(g);
    g.translate(x, y);

    ComponentUI componentUI = ModernUIUtil.getUI(c);
    if (componentUI instanceof ModernTextUI) {
      if (((ModernTextUI)componentUI).isFocused()) {
        g.setColor(ModernUIUtil.getSelectionBackground());
      }
      else if (((ModernTextUI)componentUI).getMouseEnterHandler().isMouseEntered()) {
        g.setColor(ModernUIUtil.getActiveBorderColor());
      }
      else {
        g.setColor(ModernUIUtil.getDisabledBorderColor());
      }
    }
    else {
      g.setColor(ModernUIUtil.getActiveBorderColor());
    }
    g.drawRect(0, 0, width, height);
    g.drawRect(1, 1, width - 2, height - 2);
    g.translate(-x, -y);
    config.restore();
  }
}