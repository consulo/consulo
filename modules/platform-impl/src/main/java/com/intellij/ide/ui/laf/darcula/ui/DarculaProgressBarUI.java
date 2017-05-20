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
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.ui.Gray;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.geom.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaProgressBarUI extends BasicProgressBarUI {

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    c.setBorder(JBUI.Borders.empty().asUIResource());
    return new DarculaProgressBarUI();
  }

  protected volatile int offset = 0;

  @Override
  protected void paintIndeterminate(Graphics g, JComponent c) {
    if (!(g instanceof Graphics2D)) {
      return;
    }

    Insets b = progressBar.getInsets(); // area for border
    int barRectWidth = progressBar.getWidth() - (b.right + b.left);
    int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);

    if (barRectWidth <= 0 || barRectHeight <= 0) {
      return;
    }
    //boxRect = getBox(boxRect);
    g.setColor(c.getForeground());
    int w = c.getWidth();
    int h = c.getHeight();
    g.fillRect(0, 0, w, h);
    g.setColor(UIManager.getColor("ProgressBar.stepColor2"));
    GraphicsUtil.setupAAPainting(g);
    Path2D.Double path = new Path2D.Double();
    int ww = getPeriodLength() / 2;
    path.moveTo(0, 0);
    path.lineTo(ww, 0);
    path.lineTo(ww - h / 2, h);
    path.lineTo(-h / 2, h);
    path.lineTo(0, 0);
    path.closePath();
    int x = -offset;
    while (x < Math.max(c.getWidth(), c.getHeight())) {
      g.translate(x, 0);
      ((Graphics2D)g).fill(path);
      g.translate(-x, 0);
      x += getPeriodLength();
    }
    offset = (offset + JBUI.scale(1)) % getPeriodLength();
    Area area = new Area(new Rectangle2D.Float(0, 0, w, h));
    area.subtract(new Area(new RoundRectangle2D.Float(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(2), h - JBUI.scale(2), JBUI.scale(8), JBUI.scale(8))));
    ((Graphics2D)g).setPaint(Gray._128);
    ((Graphics2D)g).fill(area);
    area.subtract(new Area(new RoundRectangle2D.Float(0, 0, w, h, JBUI.scale(9), JBUI.scale(9))));
    ((Graphics2D)g).setPaint(c.getParent().getBackground());
    ((Graphics2D)g).fill(area);
    g.drawRoundRect(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(3), h - JBUI.scale(3), JBUI.scale(8), JBUI.scale(8));

    // Deal with possible text painting
    if (progressBar.isStringPainted()) {
      if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
        paintString(g, b.left, b.top, barRectWidth, barRectHeight, boxRect.x, boxRect.width);
      }
      else {
        paintString(g, b.left, b.top, barRectWidth, barRectHeight, boxRect.y, boxRect.height);
      }
    }
  }

  @Override
  protected void paintDeterminate(Graphics g, JComponent c) {
    if (!(g instanceof Graphics2D)) {
      return;
    }

    if (progressBar.getOrientation() != SwingConstants.HORIZONTAL || !c.getComponentOrientation().isLeftToRight()) {
      super.paintDeterminate(g, c);
      return;
    }
    GraphicsUtil.setupAAPainting(g);
    Insets b = progressBar.getInsets(); // area for border
    final int w = progressBar.getWidth();
    final int h = progressBar.getHeight();
    int barRectWidth = w - (b.right + b.left);
    int barRectHeight = h - (b.top + b.bottom);

    if (barRectWidth <= 0 || barRectHeight <= 0) {
      return;
    }

    int amountFull = getAmountFull(b, barRectWidth, barRectHeight);

    g.setColor(c.getParent().getBackground());
    Graphics2D g2 = (Graphics2D)g;
    g.fillRect(0, 0, w, h);

    g2.setColor(progressBar.getForeground());
    g2.fill(new RoundRectangle2D.Float(0, 0, w - JBUI.scale(1), h - JBUI.scale(1), JBUI.scale(9), JBUI.scale(9)));
    g2.setColor(c.getParent().getBackground());
    g2.fill(new RoundRectangle2D.Float(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(3), h - JBUI.scale(3), JBUI.scale(8), JBUI.scale(8)));
    g2.setColor(progressBar.getForeground());
    g2.fill(new RoundRectangle2D.Float(JBUI.scale(2), JBUI.scale(2), amountFull - JBUI.scale(5), h - JBUI.scale(5), JBUI.scale(7), JBUI.scale(7)));


    // Deal with possible text painting
    if (progressBar.isStringPainted()) {
      paintString(g, b.left, b.top, barRectWidth, barRectHeight, amountFull, b);
    }

  }

  private void paintString(Graphics g, int x, int y, int w, int h, int fillStart, int amountFull) {
    if (!(g instanceof Graphics2D)) {
      return;
    }

    Graphics2D g2 = (Graphics2D)g;
    String progressString = progressBar.getString();
    g2.setFont(progressBar.getFont());
    Point renderLocation = getStringPlacement(g2, progressString, x, y, w, h);
    Rectangle oldClip = g2.getClipBounds();

    if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
      g2.setColor(getSelectionBackground());
      SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);
      g2.setColor(getSelectionForeground());
      g2.clipRect(fillStart, y, amountFull, h);
      SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);
    }
    else { // VERTICAL
      g2.setColor(getSelectionBackground());
      AffineTransform rotate = AffineTransform.getRotateInstance(Math.PI / 2);
      g2.setFont(progressBar.getFont().deriveFont(rotate));
      renderLocation = getStringPlacement(g2, progressString, x, y, w, h);
      SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);
      g2.setColor(getSelectionForeground());
      g2.clipRect(x, fillStart, w, amountFull);
      SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);
    }
    g2.setClip(oldClip);
  }

  @Override
  protected int getBoxLength(int availableLength, int otherDimension) {
    return availableLength;
  }

  @Override
  protected Dimension getPreferredInnerHorizontal() {
    return JBUI.size(146, 12).asUIResource();
  }

  @Override
  protected Dimension getPreferredInnerVertical() {
    return JBUI.size(12, 146).asUIResource();
  }

  protected int getPeriodLength() {
    return JBUI.scale(16);
  }
}
