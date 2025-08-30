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
package consulo.versionControlSystem.log.impl.internal.ui.render;

import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RectanglePainter {
  protected static final int TEXT_PADDING_X = JBUI.scale(5);
  public static final int TOP_TEXT_PADDING = JBUI.scale(2);
  public static final int BOTTOM_TEXT_PADDING = JBUI.scale(1);
  public static final int LABEL_ARC = JBUI.scale(6);

  private final boolean mySquare;

  public RectanglePainter(boolean square) {
    mySquare = square;
  }

  public static Font getFont() {
    return UIUtil.getLabelFont();
  }

  protected Font getLabelFont() {
    return getFont();
  }

  public void paint(@Nonnull Graphics2D g2, @Nonnull String text, int paddingX, int paddingY, @Nonnull Color color) {
    GraphicsConfig config = GraphicsUtil.setupAAPainting(g2);
    g2.setFont(getLabelFont());
    g2.setStroke(new BasicStroke(1.5f));

    FontMetrics fontMetrics = g2.getFontMetrics();
    int width = fontMetrics.stringWidth(text) + 2 * TEXT_PADDING_X;
    int height = fontMetrics.getHeight() + TOP_TEXT_PADDING + BOTTOM_TEXT_PADDING;

    g2.setColor(color);
    if (mySquare) {
      g2.fillRect(paddingX, paddingY, width, height);
    }
    else {
      g2.fill(new RoundRectangle2D.Double(paddingX, paddingY, width, height, LABEL_ARC, LABEL_ARC));
    }

    g2.setColor(JBColor.BLACK);
    int x = paddingX + TEXT_PADDING_X;
    int y = paddingY + SimpleColoredComponent.getTextBaseLine(fontMetrics, height);
    g2.drawString(text, x, y);

    config.restore();
  }

  public Dimension calculateSize(@Nonnull String text, @Nonnull FontMetrics metrics) {
    int width = metrics.stringWidth(text) + 2 * TEXT_PADDING_X;
    int height = metrics.getHeight() + TOP_TEXT_PADDING + BOTTOM_TEXT_PADDING;
    return new Dimension(width, height);
  }
}
