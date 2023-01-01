/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionButtonImpl;
import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBInsets;
import consulo.desktop.awt.ui.plaf.intellij.ActionButtonUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * @author VISTALL
 * @since 2019-10-30
 */
public class DarculaActionButtonUI extends ActionButtonUI {
  public static ActionButtonUI createUI(JComponent c) {
    return new DarculaActionButtonUI();
  }

  @Override
  public void paintBackground(ActionButtonImpl button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.NORMAL && !button.isBackgroundSet()) return;

    Rectangle rect = new Rectangle(button.getSize());
    JBInsets.removeFrom(rect, button.getInsets());

    Color color = state == ActionButtonComponent.PUSHED ? JBCurrentTheme.ActionButton.pressedBackground() : JBCurrentTheme.ActionButton.hoverBackground();
    Graphics2D g2 = (Graphics2D)g.create();

    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
      g2.setColor(color);

      float arc = DarculaUIUtil.BUTTON_ARC.getFloat();
      g2.fill(new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, arc, arc));
    }
    finally {
      g2.dispose();
    }
  }

  @Override
  public void paintBorder(ActionButtonImpl button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.NORMAL && !button.isBackgroundSet()) return;

    Rectangle rect = new Rectangle(button.getSize());
    JBInsets.removeFrom(rect, button.getInsets());

    Graphics2D g2 = (Graphics2D)g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

    try {
      Color color = state == ActionButtonComponent.PUSHED ? JBCurrentTheme.ActionButton.pressedBorder() : JBCurrentTheme.ActionButton.hoverBorder();

      g2.setColor(color);

      float arc = DarculaUIUtil.BUTTON_ARC.getFloat();
      float lw = DarculaUIUtil.LW.getFloat();
      Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
      border.append(new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, arc, arc), false);
      border.append(new RoundRectangle2D.Float(rect.x + lw, rect.y + lw, rect.width - lw * 2, rect.height - lw * 2, arc - lw, arc - lw), false);

      g2.fill(border);
    }
    finally {
      g2.dispose();
    }
  }
}
