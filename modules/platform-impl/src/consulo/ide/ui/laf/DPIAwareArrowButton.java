/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.ui.laf;

import com.intellij.util.ui.JBUI;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;

/**
 * @author VISTALL
 * @since 6/23/17
 */
public class DPIAwareArrowButton extends BasicArrowButton {
  private Color myShadow;
  private Color myDarkShadow;
  private Color myHighlight;

  public DPIAwareArrowButton(@MagicConstant(intValues = {SwingConstants.NORTH, SwingConstants.SOUTH, SwingConstants.EAST, SwingConstants.WEST}) int i,
                             Color background,
                             Color shadow,
                             Color darkShadow,
                             Color highlight) {
    super(i, background, shadow, darkShadow, highlight);
    this.myShadow = shadow;
    this.myDarkShadow = darkShadow;
    this.myHighlight = highlight;
  }

  public DPIAwareArrowButton(@MagicConstant(intValues = {SwingConstants.NORTH, SwingConstants.SOUTH, SwingConstants.EAST, SwingConstants.WEST}) int i) {
    super(i);
  }

  @Override
  public void paintTriangle(Graphics g, int x, int y, int size, int direction, boolean isEnabled) {
    Color oldColor = g.getColor();
    int mid, i, j;

    j = 0;
    size = Math.max(size, 2);
    mid = (size / 2) - 1;

    g.translate(x, y);
    if (isEnabled) {
      g.setColor(myDarkShadow);
    }
    else {
      g.setColor(myShadow);
    }

    switch (direction) {
      case NORTH:
        for (i = 0; i < size; i++) {
          drawLine(g, mid - i, i, mid + i, i);
        }
        if (!isEnabled) {
          g.setColor(myHighlight);
          drawLine(g, mid - i + 2, i, mid + i, i);
        }
        break;
      case SOUTH:
        if (!isEnabled) {
          g.translate(JBUI.scale(1), JBUI.scale(1));
          g.setColor(myHighlight);
          for (i = size - 1; i >= 0; i--) {
            drawLine(g, mid - i, j, mid + i, j);
            j++;
          }
          g.translate(-1, -1);
          g.setColor(myShadow);
        }

        j = 0;
        for (i = size - 1; i >= 0; i--) {
          drawLine(g, mid - i, j, mid + i, j);
          j++;
        }
        break;
      case WEST:
        for (i = 0; i < size; i++) {
          drawLine(g, i, mid - i, i, mid + i);
        }
        if (!isEnabled) {
          g.setColor(myHighlight);
          drawLine(g, i, mid - i + 2, i, mid + i);
        }
        break;
      case EAST:
        if (!isEnabled) {
          g.translate(JBUI.scale(1), JBUI.scale(1));
          g.setColor(myHighlight);
          for (i = size - 1; i >= 0; i--) {
            drawLine(g, j, mid - i, j, mid + i);
            j++;
          }
          g.translate(-1, -1);
          g.setColor(myShadow);
        }

        j = 0;
        for (i = size - 1; i >= 0; i--) {
          drawLine(g, j, mid - i, j, mid + i);
          j++;
        }
        break;
    }
    g.translate(-x, -y);
    g.setColor(oldColor);
  }
  
  private static void drawLine(Graphics g, int x, int y, int height, int width) {
    drawLine(g, JBUI.scale(x), JBUI.scale(y), JBUI.scale(height), JBUI.scale(width));
  }
  @Override
  public Dimension getPreferredSize() {
    return JBUI.size(super.getPreferredSize());
  }

  @Override
  public Dimension getMinimumSize() {
    return JBUI.size(super.getMinimumSize());
  }
}
