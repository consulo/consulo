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
package com.intellij.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

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
  
  private String myOnText;
  private String myOffText;

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

  public String getOnText() {
    return myOnText == null ? "ON" : myOnText;
  }

  public void setOnText(String onText) {
    myOnText = onText;
  }

  public String getOffText() {
    return myOffText == null ? "OFF" : myOffText;
  }

  public void setOffText(String offText) {
    myOffText = offText;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
  }

  public static class OnOffButtonUI extends BasicToggleButtonUI {
    public OnOffButtonUI(OnOffButton checkBox) {
    }

    @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
    public static ComponentUI createUI(JComponent c) {
      return new OnOffButtonUI((OnOffButton)c);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
      final OnOffButton button = (OnOffButton)c;
      String text = button.getOffText().length() > button.getOnText().length() ? button.getOffText() : button.getOnText();
      text = text.toUpperCase();
      final FontMetrics fm = c.getFontMetrics(c.getFont());
      int w = fm.stringWidth(text);
      int h = fm.getHeight();
      h += 2 * 4;
      w += 3 * h / 2;
      return new Dimension(w, h);
    }

    @Override
    public void paint(Graphics gr, JComponent c) {
      final OnOffButton button = (OnOffButton)c;
      final Dimension size = button.getSize();
      int w = size.width - 8;
      int h = size.height - 6;
      if (h % 2 == 1) {
        h--;
      }

      int ovalSize = h - JBUI.scale(4);

      Graphics2D g = ((Graphics2D)gr);
      GraphicsUtil.setupAAPainting(g);
      g.translate(1, 1);
      if (button.isSelected()) {
        Color color = UIManager.getColor("Hyperlink.linkColor");
        if (color == null) {
          color = new JBColor(new Color(57, 113, 238), new Color(13, 41, 62));
        }

        g.setColor(color);
        g.fillRoundRect(0, 0, w, h, h, h);
        g.setColor(UIUtil.getBorderColor());
        g.drawRoundRect(0, 0, w, h, h, h);

        g.setColor(UIUtil.getListForeground(true));
        g.drawString(button.getOnText(), h / 2, h - 4);
        
        g.setColor(UIUtil.getBorderColor());
        g.fillOval(w - h + JBUI.scale(1), JBUI.scale(2), ovalSize, ovalSize);
      }
      else {
        g.setColor(UIUtil.getPanelBackground());
        g.fillRoundRect(0, 0, w, h, h, h);
        g.setColor(UIUtil.getBorderColor());
        g.drawRoundRect(0, 0, w, h, h, h);
        g.setColor(UIUtil.getLabelDisabledForeground());
        g.drawString(button.getOffText(), h + 4, h - 4);
        
        g.setColor(UIUtil.getBorderColor());
        g.fillOval(JBUI.scale(2), JBUI.scale(2), ovalSize, ovalSize);
      }
      g.translate(-1, -1);
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
