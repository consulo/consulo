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
package consulo.desktop.ui.laf.idea.windows;

import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.ui.paint.RectanglePainter2D;
import com.intellij.util.ui.JBUI;
import consulo.ide.ui.laf.intellij.ActionButtonUI;

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
  public void paintBorder(ActionButton button, Graphics g, Dimension size, int state) {
  }

  @Override
  public void paintBackground(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.PUSHED) {
      g.setColor(JBUI.CurrentTheme.ActionButton.pressedBackground());
      RectanglePainter2D.FILL.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    }
    else if(state == ActionButtonComponent.POPPED) {
      g.setColor(JBUI.CurrentTheme.ActionButton.hoverBackground());
      RectanglePainter2D.FILL.paint((Graphics2D)g, 0, 0, size.getWidth(), size.getHeight());
    }
  }
}