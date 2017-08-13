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

import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import consulo.ide.ui.laf.intellij.ActionButtonUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 27-Nov-16.
 */
public class ModernActionButtonUI extends ActionButtonUI {
  public static ModernActionButtonUI createUI(JComponent c) {
    return new ModernActionButtonUI();
  }

  @Override
  protected void paintBorder(ActionButton button, Graphics g, Dimension size, int state) {
    final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    g.setColor(state == ActionButtonComponent.POPPED || state == ActionButtonComponent.PUSHED
               ? ModernUIUtil.getSelectionBackground()
               : ModernUIUtil.getBorderColor(button));
    g.drawRect(0, 0, size.width - JBUI.scale(1), size.height - JBUI.scale(1));
    config.restore();
  }

  @Override
  protected void paintBackground(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.PUSHED) {
      g.setColor(ColorUtil.toAlpha(ModernUIUtil.getSelectionBackground(), 100));
      g.fillRect(JBUI.scale(1), JBUI.scale(1), size.width - JBUI.scale(2), size.height - JBUI.scale(2));
    }
  }
}
