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
import com.intellij.ui.paint.RectanglePainter2D;
import com.intellij.util.ui.GraphicsUtil;
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
  public void paintBorder(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.NORMAL && !button.isBackgroundSet()) return;

    final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    g.setColor(state == ActionButtonComponent.POPPED || state == ActionButtonComponent.PUSHED
               ? ModernUIUtil.getSelectionBackground()
               : ModernUIUtil.getBorderColor(button));
    RectanglePainter2D.DRAW.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    config.restore();
  }

  @Override
  public void paintBackground(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.PUSHED) {
      g.setColor(ColorUtil.toAlpha(ModernUIUtil.getSelectionBackground(), 100));
      RectanglePainter2D.FILL.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    }
  }
}
