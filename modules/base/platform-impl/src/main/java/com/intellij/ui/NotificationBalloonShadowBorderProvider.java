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

import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.util.ui.JBInsets;

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