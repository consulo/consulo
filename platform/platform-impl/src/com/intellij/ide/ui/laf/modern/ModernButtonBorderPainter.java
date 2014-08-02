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

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 02.08.14
 *        <p/>
 *        Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaButtonPainter}
 */
public class ModernButtonBorderPainter implements Border, UIResource {
   private static NotNullLazyValue<Field> ourUiField = new NotNullLazyValue<Field>() {
     @NotNull
     @Override
     protected Field compute() {
       try {
         Field ui = JComponent.class.getDeclaredField("ui");
         ui.setAccessible(true);
         return ui;
       }
       catch (NoSuchFieldException e) {
         throw new Error(e);
       }
     }
   };

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    if (isDefaultButton(c)) {
      return;
    }

    final Insets ins = getBorderInsets(c);
    final int yOff = (ins.top + ins.bottom) / 4;
    final boolean square = DarculaButtonUI.isSquare(c);
    int offset = square ? 1 : 4;

    ModernButtonUI ui = getUI(c);
    MouseEnterHandler mouseEnterHandler = ui.getMouseEnterHandler();
    if(mouseEnterHandler.isMousePressed()) {
      g.setColor(ModernUIUtil.getActiveBorderColor());
    }
    else {
      if(isDefaultButton(c)) {
        g.setColor(c.isEnabled() ? ModernUIUtil.getSelectionBackground() : ModernUIUtil.getDisabledBorderColor());
      }
      else {
        if(c.isEnabled()) {
          g.setColor(mouseEnterHandler.isMouseEntered() ? ModernUIUtil.getSelectionBackground() : ModernUIUtil.getActiveBorderColor());
        }
        else {
          g.setColor(ModernUIUtil.getDisabledBorderColor());
        }
      }
    }

    g.drawRect(x + offset, y + yOff, width - 2 * offset, height - 2 * yOff);
  }

  public static ModernButtonUI getUI(Component component) {
    Field value = ourUiField.getValue();
    try {
      return (ModernButtonUI)value.get(component);
    }
    catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }

  public static boolean isDefaultButton(Object component) {
    return component instanceof JButton && ((JButton)component).isDefaultButton();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    if (DarculaButtonUI.isSquare(c)) {
      return new InsetsUIResource(2, 0, 2, 0);
    }
    return new InsetsUIResource(8, 16, 8, 14);
  }



  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}
