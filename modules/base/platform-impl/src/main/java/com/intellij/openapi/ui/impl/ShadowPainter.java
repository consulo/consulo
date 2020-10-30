// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.ui.impl;

import com.intellij.ide.ui.LafManager;
import com.intellij.ui.JreHiDpiUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBUI.ScaleContext;
import com.intellij.util.ui.JBUI.ScaleContextAware;
import com.intellij.util.ui.JBUI.ScaleContextSupport;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author Konstantin Bulenkov
 */
public class ShadowPainter extends ScaleContextSupport<ScaleContext> {
  private final Image myTop;
  private final Image myTopRight;
  private final Image myRight;
  private final Image myBottomRight;
  private final Image myBottom;
  private final Image myBottomLeft;
  private final Image myLeft;
  private final Image myTopLeft;

  private Icon myCroppedTop = null;
  private Icon myCroppedRight = null;
  private Icon myCroppedBottom = null;
  private Icon myCroppedLeft = null;

  @Nullable
  private Color myBorderColor;

  public ShadowPainter(Image top, Image topRight, Image right, Image bottomRight, Image bottom, Image bottomLeft, Image left, Image topLeft) {
    super(ScaleContext.create());
    myTop = top;
    myTopRight = topRight;
    myRight = right;
    myBottomRight = bottomRight;
    myBottom = bottom;
    myBottomLeft = bottomLeft;
    myLeft = left;
    myTopLeft = topLeft;

    updateIcons(null);
    LafManager.getInstance().addLafManagerListener(source -> updateIcons(null));
  }

  public ShadowPainter(Image top, Image topRight, Image right, Image bottomRight, Image bottom, Image bottomLeft, Image left, Image topLeft, @Nullable Color borderColor) {
    this(top, topRight, right, bottomRight, bottom, bottomLeft, left, topLeft);
    myBorderColor = borderColor;
  }

  public void setBorderColor(@Nullable Color borderColor) {
    myBorderColor = borderColor;
  }

  public BufferedImage createShadow(final JComponent c, final int width, final int height) {
    final BufferedImage image = c.getGraphicsConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    final Graphics2D g = image.createGraphics();

    paintShadow(c, g, 0, 0, width, height);

    g.dispose();
    return image;
  }

  private void updateIcons(ScaleContext ctx) {
    updateIcon(TargetAWT.to(myTop), ctx, () -> myCroppedTop = IconUtil.cropIcon(TargetAWT.to(myTop), 1, Integer.MAX_VALUE));
    updateIcon(TargetAWT.to(myTopRight), ctx, null);
    updateIcon(TargetAWT.to(myRight), ctx, () -> myCroppedRight = IconUtil.cropIcon(TargetAWT.to(myRight), Integer.MAX_VALUE, 1));
    updateIcon(TargetAWT.to(myBottomRight), ctx, null);
    updateIcon(TargetAWT.to(myBottom), ctx, () -> myCroppedBottom = IconUtil.cropIcon(TargetAWT.to(myBottom), 1, Integer.MAX_VALUE));
    updateIcon(TargetAWT.to(myBottomLeft), ctx, null);
    updateIcon(TargetAWT.to(myLeft), ctx, () -> myCroppedLeft = IconUtil.cropIcon(TargetAWT.to(myLeft), Integer.MAX_VALUE, 1));
    updateIcon(TargetAWT.to(myTopLeft), ctx, null);
  }

  private static void updateIcon(Icon icon, ScaleContext ctx, Runnable r) {
    if (icon instanceof ScaleContextAware) ((ScaleContextAware)icon).updateScaleContext(ctx);
    if (r != null) r.run();
  }

  public void paintShadow(Component c, Graphics2D g, int x, int y, int width, int height) {
    ScaleContext ctx = ScaleContext.create(c, g);
    if (updateScaleContext(ctx)) {
      updateIcons(ctx);
    }
    final int leftSize = myCroppedLeft.getIconWidth();
    final int rightSize = myCroppedRight.getIconWidth();
    final int bottomSize = myCroppedBottom.getIconHeight();
    final int topSize = myCroppedTop.getIconHeight();

    int delta = myTopLeft.getHeight() + myBottomLeft.getHeight() - height;
    if (delta > 0) { // Corner icons are overlapping. Need to handle this
      Shape clip = g.getClip();

      int topHeight = myTopLeft.getHeight() - delta / 2;
      Area top = new Area(new Rectangle2D.Float(x, y, width, topHeight));
      if (clip != null) {
        top.intersect(new Area(clip));
      }
      g.setClip(top);

      TargetAWT.to(myTopLeft).paintIcon(c, g, x, y);
      TargetAWT.to(myTopRight).paintIcon(c, g, x + width - myTopRight.getWidth(), y);

      int bottomHeight = myBottomLeft.getHeight() - delta + delta / 2;
      Area bottom = new Area(new Rectangle2D.Float(x, y + topHeight, width, bottomHeight));
      if (clip != null) {
        bottom.intersect(new Area(clip));
      }
      g.setClip(bottom);

      TargetAWT.to(myBottomLeft).paintIcon(c, g, x, y + height - myBottomLeft.getHeight());
      TargetAWT.to(myBottomRight).paintIcon(c, g, x + width - myBottomRight.getWidth(), y + height - myBottomRight.getHeight());

      g.setClip(clip);
    }
    else {
      TargetAWT.to(myTopLeft).paintIcon(c, g, x, y);
      TargetAWT.to(myTopRight).paintIcon(c, g, x + width - myTopRight.getWidth(), y);
      TargetAWT.to(myBottomLeft).paintIcon(c, g, x, y + height - myBottomLeft.getHeight());
      TargetAWT.to(myBottomRight).paintIcon(c, g, x + width - myBottomRight.getWidth(), y + height - myBottomRight.getHeight());
    }

    fill(g, myCroppedTop, x, y, myTopLeft.getWidth(), width - myTopRight.getWidth(), true);
    fill(g, myCroppedBottom, x, y + height - bottomSize, myBottomLeft.getWidth(), width - myBottomRight.getWidth(), true);
    fill(g, myCroppedLeft, x, y, myTopLeft.getHeight(), height - myBottomLeft.getHeight(), false);
    fill(g, myCroppedRight, x + width - rightSize, y, myTopRight.getHeight(), height - myBottomRight.getHeight(), false);

    if (myBorderColor != null) {
      g.setColor(myBorderColor);
      g.drawRect(x + leftSize - 1, y + topSize - 1, width - leftSize - rightSize + 1, height - topSize - bottomSize + 1);
    }
  }

  private static void fill(Graphics g, Icon pattern, int x, int y, int from, int to, boolean horizontally) {
    double scale = JBUI.sysScale((Graphics2D)g);
    if (JreHiDpiUtil.isJreHiDPIEnabled() && Math.ceil(scale) > scale) {
      // Direct painting for fractional scale
      BufferedImage img = ImageUtil.toBufferedImage(IconUtil.toImage(pattern));
      int patternSize = horizontally ? img.getWidth() : img.getHeight();
      Graphics2D g2d = (Graphics2D)g.create();
      try {
        g2d.scale(1 / scale, 1 / scale);
        g2d.translate(x * scale, y * scale);
        for (int at = (int)Math.floor(from * scale); at < to * scale; at += patternSize) {
          if (horizontally) {
            g2d.drawImage(img, at, 0, null);
          }
          else {
            g2d.drawImage(img, 0, at, null);
          }
        }
      }
      finally {
        g2d.dispose();
      }
    }
    else {
      for (int at = from; at < to; at++) {
        if (horizontally) {
          pattern.paintIcon(null, g, x + at, y);
        }
        else {
          pattern.paintIcon(null, g, x, y + at);
        }
      }
    }
  }
}
