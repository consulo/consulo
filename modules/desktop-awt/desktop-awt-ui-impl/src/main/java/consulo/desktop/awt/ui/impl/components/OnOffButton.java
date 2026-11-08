/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.impl.components;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class OnOffButton extends JToggleButton {
  private static final String uiClassID = "OnOffButtonUI";

  public OnOffButton() {
    setBorder(JBUI.Borders.empty());
    setOpaque(false);
    updateUI();
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  @Override
  public void updateUI() {
    ButtonUI componentUI = (ButtonUI)UIManager.getUI(this);
    setUI(componentUI == null ? OnOffButtonUI.createUI(this) : componentUI);
  }

  public static class OnOffButtonUI extends BasicToggleButtonUI {
    private static final int WIDTH = 44;
    private static final int HEIGHT = 22;
    private static final int MARGIN = 3;

    public OnOffButtonUI(OnOffButton checkBox) {
    }

    @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
    public static ComponentUI createUI(JComponent c) {
      return new OnOffButtonUI((OnOffButton)c);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
      return new Dimension(JBUI.scale(WIDTH), JBUI.scale(HEIGHT));
    }

    @Override
    public void paint(Graphics gr, JComponent c) {
      OnOffButton button = (OnOffButton)c;

      int w = JBUI.scale(WIDTH);
      int h = JBUI.scale(HEIGHT);
      int margin = JBUI.scale(MARGIN);
      int thumbSize = h - 2 * margin;

      Graphics2D g = (Graphics2D)gr.create();
      try {
        GraphicsUtil.setupAAPainting(g);
        g.translate((button.getWidth() - w) / 2, (button.getHeight() - h) / 2);

        if (button.isSelected()) {
          Color accent = UIManager.getColor("Component.accentColor");
          if (accent == null) {
            accent = new JBColor(new Color(53, 116, 240), new Color(53, 116, 240));
          }
          g.setColor(accent);
          g.fillRoundRect(0, 0, w, h, h, h);

          g.setColor(Color.WHITE);
          g.fillOval(w - margin - thumbSize, margin, thumbSize, thumbSize);
        }
        else {
          g.setColor(UIUtil.getBorderColor());
          g.fillRoundRect(0, 0, w, h, h, h);

          g.setColor(UIUtil.getPanelBackground());
          g.fillOval(margin, margin, thumbSize, thumbSize);
        }
      }
      finally {
        g.dispose();
      }
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
      return getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
      return getPreferredSize(c);
    }
  }
}
