/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.action;

import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.Gray;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;

import javax.swing.*;
import java.awt.*;

/**
 * A wrapper for an icon which paints it like a selected toggleable action in toolbar
 *
 * @author Konstantin Bulenkov
 *
 * FIXME [VISTALL] extract paint code from this class, or we need create pushed icon as background
 */
public class PoppedIcon implements Icon, Image {
  private static final Color ALPHA_20 = Gray._0.withAlpha(20);
  private static final Color ALPHA_30 = Gray._0.withAlpha(30);
  private static final Color ALPHA_40 = Gray._0.withAlpha(40);
  private static final Color ALPHA_120 = Gray._0.withAlpha(120);
  private static final BasicStroke BASIC_STROKE = new BasicStroke();

  private final Image myIcon;
  private final int myWidth;
  private final int myHeight;

  public PoppedIcon(Image icon, int width, int height) {
    myIcon = icon;
    myWidth = width;
    myHeight = height;
  }

  public PoppedIcon(Image icon) {
    this(icon, icon.getWidth(), icon.getHeight());
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    final Dimension size = new Dimension(getIconWidth() + 2 * x, getIconHeight() + 2 * x);
    paintBackground(g, size, ActionButtonComponent.POPPED);
    paintBorder(g, size, ActionButtonComponent.POPPED);

    Icon icon = TargetAWT.to(myIcon);
    icon.paintIcon(c, g, x + (getIconWidth() - icon.getIconWidth()) / 2, y + (getIconHeight() - icon.getIconHeight()) / 2);
  }

  private static void paintBackground(Graphics g, Dimension size, int state) {
    if (UIUtil.isUnderAquaLookAndFeel()) {
      if (state == ActionButtonComponent.PUSHED) {
        ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, ALPHA_40, size.width, size.height, ALPHA_20));
        g.fillRect(0, 0, size.width - 1, size.height - 1);

        g.setColor(ALPHA_120);
        g.drawLine(0, 0, 0, size.height - 2);
        g.drawLine(1, 0, size.width - 2, 0);

        g.setColor(ALPHA_30);
        g.drawRect(1, 1, size.width - 3, size.height - 3);
      }
      else if (state == ActionButtonComponent.POPPED) {
        ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, Gray._235, 0, size.height, Gray._200));
        g.fillRect(1, 1, size.width - 3, size.height - 3);
      }
    }
    else {
      final Color bg = UIUtil.getPanelBackground();
      final boolean dark = StyleManager.get().getCurrentStyle().isDark();
      g.setColor(state == ActionButtonComponent.PUSHED ? ColorUtil.shift(bg, dark ? 1d / 0.7d : 0.7d) : dark ? Gray._255.withAlpha(40) : ALPHA_40);
      g.fillRect(JBUI.scale(1), JBUI.scale(1), size.width - JBUI.scale(2), size.height - JBUI.scale(2));
    }
  }

  private static void paintBorder(Graphics g, Dimension size, int state) {
    if (UIUtil.isUnderAquaLookAndFeel()) {
      if (state == ActionButtonComponent.POPPED) {
        g.setColor(ALPHA_30);
        g.drawRoundRect(0, 0, size.width - 2, size.height - 2, 4, 4);
      }
    }
    else {
      final double shift = StyleManager.get().getCurrentStyle().isDark() ? 1 / 0.49 : 0.49;
      g.setColor(ColorUtil.shift(UIUtil.getPanelBackground(), shift));
      ((Graphics2D)g).setStroke(BASIC_STROKE);
      final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      g.drawRoundRect(0, 0, size.width - JBUI.scale(2), size.height - JBUI.scale(2), JBUI.scale(4), JBUI.scale(4));
      config.restore();
    }
  }

  @Override
  public int getIconWidth() {
    return myWidth;
  }

  @Override
  public int getIconHeight() {
    return myHeight;
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }
}
