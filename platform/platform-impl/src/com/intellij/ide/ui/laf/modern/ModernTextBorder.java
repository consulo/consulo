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
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author VISTALL
 * @since 05.08.14
 *
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder}
 */
public class ModernTextBorder implements Border, UIResource {
  public static interface ModernTextUI {
    boolean isFocused();

    @NotNull
    MouseEnterHandler getMouseEnterHandler();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    int vOffset = c instanceof JPasswordField ? 3  : c instanceof JSpinner ? 6 : 4;
    if (ModernTextFieldUI.isSearchFieldWithHistoryPopup(c)) {
      return JBUI.insets(vOffset, 7 + 16 + 3, vOffset, 7 + 16).asUIResource();
    }
    else if (ModernTextFieldUI.isSearchField(c)) {
      return JBUI.insets(vOffset, 4 + 16 + 3, vOffset, 7 + 16).asUIResource();
    }
    else {
      return JBUI.insets(vOffset, 7, vOffset, 7).asUIResource();
    }
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }

  @Override
  public void paintBorder(Component c, Graphics g2, int x, int y, int width, int height) {
    if (ModernTextFieldUI.isSearchField(c)) return;
    Graphics2D g = ((Graphics2D)g2);
    final GraphicsConfig config = new GraphicsConfig(g);
    g.translate(x, y);

    ComponentUI componentUI = ModernUIUtil.getUI(c);
    // TODO [VISTALL] find way for handle component of EditorTextField
    if(componentUI instanceof ModernTextUI) {
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
    g.drawRect(1, 1, width - JBUI.scale(2), height - JBUI.scale(2));
    g.translate(-x, -y);
    config.restore();
  }
}