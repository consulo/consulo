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
package consulo.desktop.ui.laf.ui.basic;

import com.intellij.ui.CaptionPanel;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-11-05
 */
public class BasicCaptionPanelUI extends BasicPanelUI {
  public static ComponentUI createUI(JComponent c) {
    return new BasicCaptionPanelUI();
  }

  private static final Color CNT_COLOR = new JBColor(Gray._240, Gray._90);
  private static final Color BND_COLOR = new JBColor(Gray._240, Gray._90);

  public static final Color CNT_ACTIVE_COLOR = new JBColor(Gray._202, Gray._55);
  public static final Color CNT_ACTIVE_BORDER_COLOR = new JBColor(CNT_ACTIVE_COLOR, UIUtil.getBorderColor());
  public static final Color BND_ACTIVE_COLOR = new JBColor(Gray._239, Gray._90);

  private static final JBColor TOP_FLICK_ACTIVE = new JBColor(Color.white, Gray._110);
  private static final JBColor TOP_FLICK_PASSIVE = new JBColor(Color.white, BND_COLOR);

  private static final JBColor BOTTOM_FLICK_ACTIVE = new JBColor(Color.gray, Gray._35);
  private static final JBColor BOTTOM_FLICK_PASSIVE = new JBColor(Color.lightGray, Gray._75);

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    final Graphics2D g2d = (Graphics2D)g;

    CaptionPanel panel = (CaptionPanel)c;
    if (panel.isActive()) {
      g.setColor(TOP_FLICK_ACTIVE);
      g.drawLine(0, 0, panel.getWidth(), 0);
      g.setColor(BOTTOM_FLICK_ACTIVE);
      g.drawLine(0, panel.getHeight() - 1, panel.getWidth(), panel.getHeight() - 1);
      g2d.setPaint(UIUtil.getGradientPaint(0, 0, BND_ACTIVE_COLOR, 0, panel.getHeight(), CNT_ACTIVE_COLOR));
    }
    else {
      g.setColor(TOP_FLICK_PASSIVE);
      g.drawLine(0, 0, panel.getWidth(), 0);
      g.setColor(BOTTOM_FLICK_PASSIVE);
      g.drawLine(0, panel.getHeight() - 1, panel.getWidth(), panel.getHeight() - 1);
      g2d.setPaint(UIUtil.getGradientPaint(0, 0, BND_COLOR, 0, panel.getHeight(), CNT_COLOR));
    }

    g2d.fillRect(0, 1, panel.getWidth(), panel.getHeight() - 2);
  }
}
