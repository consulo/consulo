/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.icons.AllIcons.Ide.Notification.Shadow;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.ImageKey;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * @author Alexander Lobas
 */
public class NotificationBalloonShadowBorderProvider implements BalloonImpl.ShadowBorderProvider {
  private static final Insets INSETS = new JBInsets(4, 6, 8, 6);
  private final Color myFillColor;
  private final Color myBorderColor;

  public NotificationBalloonShadowBorderProvider(@Nonnull Color fillColor, @Nonnull Color borderColor) {
    myFillColor = fillColor;
    myBorderColor = borderColor;
  }

  @Nonnull
  @Override
  public Insets getInsets() {
    return INSETS;
  }

  @Override
  public void paintShadow(@Nonnull JComponent component, @Nonnull Graphics g) {
    int width = component.getWidth();
    int height = component.getHeight();

    int topLeftWidth = Shadow.Top_left.getWidth();
    int topLeftHeight = Shadow.Top_left.getHeight();

    int topRightWidth = Shadow.Top_right.getWidth();
    int topRightHeight = Shadow.Top_right.getHeight();

    int bottomLeftWidth = Shadow.Bottom_left.getWidth();
    int bottomLeftHeight = Shadow.Bottom_left.getHeight();

    int bottomRightWidth = Shadow.Bottom_right.getWidth();
    int bottomRightHeight = Shadow.Bottom_right.getHeight();

    int topWidth = Shadow.Top.getWidth();

    int bottomWidth = Shadow.Bottom.getWidth();
    int bottomHeight = Shadow.Bottom.getHeight();

    int leftHeight = Shadow.Left.getHeight();

    int rightWidth = Shadow.Right.getWidth();
    int rightHeight = Shadow.Right.getHeight();

    drawLine(component, g, PlatformIconGroup.ideNotificationShadowTop(), width, topLeftWidth, topRightWidth, topWidth, 0, true);
    drawLine(component, g, PlatformIconGroup.ideNotificationShadowBottom(), width, bottomLeftWidth, bottomRightWidth, bottomWidth, height - bottomHeight, true);

    drawLine(component, g, PlatformIconGroup.ideNotificationShadowLeft(), height, topLeftHeight, bottomLeftHeight, leftHeight, 0, false);
    drawLine(component, g, PlatformIconGroup.ideNotificationShadowRight(), height, topRightHeight, bottomRightHeight, rightHeight, width - rightWidth, false);

    TargetAWT.to(Shadow.Top_left).paintIcon(component, g, 0, 0);
    TargetAWT.to(Shadow.Top_right).paintIcon(component, g, width - topRightWidth, 0);
    TargetAWT.to(Shadow.Bottom_right).paintIcon(component, g, width - bottomRightWidth, height - bottomRightHeight);
    TargetAWT.to(Shadow.Bottom_left).paintIcon(component, g, 0, height - bottomLeftHeight);
  }

  private static void drawLine(@Nonnull JComponent component, @Nonnull Graphics g, @Nonnull consulo.ui.image.Image icon, int fullLength, int start, int end, int step, int start2, boolean horizontal) {
    int length = fullLength - start - end;
    int count = length / step;
    int calcLength = step * count;
    int lastValue = start + calcLength;

    if (horizontal) {
      for (int i = start; i < lastValue; i += step) {
        TargetAWT.to(icon).paintIcon(component, g, i, start2);
      }
    }
    else {
      for (int i = start; i < lastValue; i += step) {
        TargetAWT.to(icon).paintIcon(component, g, start2, i);
      }
    }

    if (calcLength < length) {
      Image image = TargetAWT.toImage((ImageKey)icon);
      if (horizontal) {
        UIUtil.drawImage(g, image, new Rectangle(lastValue, start2, length - calcLength, icon.getHeight()), new Rectangle(0, 0, length - calcLength, icon.getHeight()),
                         component);
      }
      else {
        UIUtil.drawImage(g, image, new Rectangle(start2, lastValue, icon.getWidth(), length - calcLength), new Rectangle(0, 0, icon.getWidth(), length - calcLength),
                         component);
      }
    }
  }

  @Override
  public void paintBorder(@Nonnull Rectangle bounds, @Nonnull Graphics2D g) {
    g.setColor(myFillColor);
    g.fill(new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
    g.setColor(myBorderColor);
    g.draw(new RoundRectangle2D.Double(bounds.x + 0.5, bounds.y + 0.5, bounds.width - 1, bounds.height - 1, 3, 3));
  }

  @Override
  public void paintPointingShape(@Nonnull Rectangle bounds, @Nonnull Point pointTarget, @Nonnull Balloon.Position position, @Nonnull Graphics2D g) {
    int x, y, length;

    if (position == Balloon.Position.above) {
      length = INSETS.bottom;
      x = pointTarget.x;
      y = bounds.y + bounds.height + length;
    }
    else if (position == Balloon.Position.below) {
      length = INSETS.top;
      x = pointTarget.x;
      y = bounds.y - length;
    }
    else if (position == Balloon.Position.atRight) {
      length = INSETS.left;
      x = bounds.x - length;
      y = pointTarget.y;
    }
    else {
      length = INSETS.right;
      x = bounds.x + bounds.width + length;
      y = pointTarget.y;
    }

    Polygon p = new Polygon();
    p.addPoint(x, y);

    length += 2;
    if (position == Balloon.Position.above) {
      p.addPoint(x - length, y - length);
      p.addPoint(x + length, y - length);
    }
    else if (position == Balloon.Position.below) {
      p.addPoint(x - length, y + length);
      p.addPoint(x + length, y + length);
    }
    else if (position == Balloon.Position.atRight) {
      p.addPoint(x + length, y - length);
      p.addPoint(x + length, y + length);
    }
    else {
      p.addPoint(x - length, y - length);
      p.addPoint(x - length, y + length);
    }

    g.setColor(myFillColor);
    g.fillPolygon(p);

    g.setColor(myBorderColor);

    length -= 2;
    if (position == Balloon.Position.above) {
      g.drawLine(x, y, x - length, y - length);
      g.drawLine(x, y, x + length, y - length);
    }
    else if (position == Balloon.Position.below) {
      g.drawLine(x, y, x - length, y + length);
      g.drawLine(x, y, x + length, y + length);
    }
    else if (position == Balloon.Position.atRight) {
      g.drawLine(x, y, x + length, y - length);
      g.drawLine(x, y, x + length, y + length);
    }
    else {
      g.drawLine(x, y, x - length, y - length);
      g.drawLine(x, y, x - length, y + length);
    }
  }
}