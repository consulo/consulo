/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.status;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBValue;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author spLeaner
 */
public class BasicStatusBarUI extends ComponentUI {
  public static ComponentUI createUI(JComponent c) {
    return new BasicStatusBarUI();
  }

  private static final JBValue BW = new JBValue.Float(1);

  @Override
  public void paint(final Graphics g, final JComponent c) {
    Graphics2D g2d = (Graphics2D)g;
    Rectangle r = new Rectangle(c.getSize());
    try {
      g2d.setColor(UIUtil.getPanelBackground());
      g2d.fill(r);

      g2d.setColor(JBColor.border());
      g2d.fill(new Rectangle(r.x, r.y, r.width, BW.get()));
    }
    finally {
      g2d.dispose();
    }
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    return JBUI.size(100, 23);
  }

  @Override
  public Dimension getMaximumSize(JComponent c) {
    return JBUI.size(Integer.MAX_VALUE, 23);
  }
}

