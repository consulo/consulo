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
package consulo.desktop.editor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 26/02/2021
 *
 * TODO use ComponentUI impl
 */
public class DesktopEditorFloatPanel extends JPanel {
  public DesktopEditorFloatPanel() {
    super(new BorderLayout(0, 0));
    setBorder(JBUI.Borders.empty(1, 2));
    setOpaque(false);
  }

  @Override
  protected void paintChildren(final Graphics g) {
    Graphics2D graphics = (Graphics2D)g.create();
    try {
      graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, getChildrenOpacity()));
      super.paintChildren(graphics);
    }
    finally {
      graphics.dispose();
    }
  }

  @Override
  public void paint(Graphics g) {
    paintComponent(g);
    super.paint(g);
  }

  @Override
  public void paintComponent(final Graphics g) {
    Graphics2D graphics = (Graphics2D)g.create();
    try {
      Rectangle r = getBounds();

      graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getBackgroundOpacity()));
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(getBackground());

      graphics.fillRoundRect(JBUI.scale(1), JBUI.scale(1), r.width - JBUI.scale(1), r.height - JBUI.scale(1), JBUI.scale(6), JBUI.scale(6));
    }
    finally {
      graphics.dispose();
    }
  }

  @Override
  public Color getBackground() {
    return JBColor.GRAY;
  }

  protected float getChildrenOpacity() {
    return 1f;
  }

  protected float getBackgroundOpacity() {
    return getChildrenOpacity();
  }
}
