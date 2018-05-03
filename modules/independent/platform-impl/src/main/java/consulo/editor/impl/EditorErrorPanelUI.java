/*
 * Copyright 2013-2018 consulo.io
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
package consulo.editor.impl;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-04-28
 */
public class EditorErrorPanelUI extends PanelUI {
  public static ComponentUI createUI(JComponent c) {
    return new EditorErrorPanelUI((EditorErrorPanel)c);
  }

  private final EditorErrorPanel myPanel;

  private EditorErrorPanelUI(EditorErrorPanel panel) {
    myPanel = panel;
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    return new Dimension(JBUI.scale(1) + HighlightDisplayLevel.getEmptyIconDim(), 0);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    g.setColor(UIUtil.getPanelBackground());
    g.fillRect(0, 0, c.getWidth(), c.getHeight());

    //g.setColor(JBColor.border());
    //g.drawLine(0, 0, c.getWidth(), 0);

    ErrorStripeRenderer errorStripeRenderer = myPanel.getMarkupModel().getErrorStripeRenderer();
    if (errorStripeRenderer != null) {
      errorStripeRenderer.paint(c, g, new Point(JBUI.scale(1), 0));
    }
  }
}
