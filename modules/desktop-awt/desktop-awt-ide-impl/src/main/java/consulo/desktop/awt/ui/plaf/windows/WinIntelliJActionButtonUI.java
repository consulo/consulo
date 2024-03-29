/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.ui.plaf.windows;

import consulo.desktop.awt.action.ActionButtonImpl;
import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.desktop.awt.ui.plaf.intellij.ActionButtonUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 04/05/2021
 */
public class WinIntelliJActionButtonUI extends ActionButtonUI {
  public static ActionButtonUI createUI(JComponent c) {
    return new WinIntelliJActionButtonUI();
  }

  @Override
  public void paintBorder(ActionButtonImpl button, Graphics g, Dimension size, int state) {
  }

  @Override
  public void paintBackground(ActionButtonImpl button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.PUSHED) {
      g.setColor(JBCurrentTheme.ActionButton.pressedBackground());
      RectanglePainter2D.FILL.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    }
    else if(state == ActionButtonComponent.POPPED) {
      g.setColor(JBCurrentTheme.ActionButton.hoverBackground());
      RectanglePainter2D.FILL.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    }
  }
}