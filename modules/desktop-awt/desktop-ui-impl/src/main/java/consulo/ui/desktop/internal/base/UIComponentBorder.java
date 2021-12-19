/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.desktop.internal.base;

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.impl.BorderInfo;

import javax.swing.border.Border;
import java.awt.*;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
class UIComponentBorder implements Border {
  private final Map<BorderPosition, BorderInfo> myBorders;

  UIComponentBorder(Map<BorderPosition, BorderInfo> borders) {
    myBorders = borders;
  }

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Color oldColor = g.getColor();
    paintBorder(BorderPosition.LEFT, g, (thickness) -> g.fillRect(x, y, thickness, height));
    paintBorder(BorderPosition.TOP, g, (thickness) -> g.fillRect(x, y, width, thickness));
    paintBorder(BorderPosition.RIGHT, g, (thickness) -> g.fillRect(x + width - thickness, y, thickness, height));
    paintBorder(BorderPosition.BOTTOM, g, (thickness) -> g.fillRect(x, y + height - thickness, width, thickness));
    g.setColor(oldColor);
  }

  private void paintBorder(BorderPosition position, Graphics g, IntConsumer consumer) {
    BorderInfo borderInfo = myBorders.get(position);
    if (borderInfo == null) {
      return;
    }

    BorderStyle borderStyle = borderInfo.getBorderStyle();
    if (borderStyle != BorderStyle.LINE) {
      return;
    }

    g.setColor(TargetAWT.to(borderInfo.getColorValue()));

    consumer.accept(JBUI.scale(borderInfo.getWidth()));
  }

  @Override
  public Insets getBorderInsets(Component component) {
    //noinspection UseDPIAwareInsets
    Insets insets = new Insets(0, 0, 0, 0);
    insets.top = getBorderSize(myBorders, BorderPosition.TOP);
    insets.left = getBorderSize(myBorders, BorderPosition.LEFT);
    insets.bottom = getBorderSize(myBorders, BorderPosition.BOTTOM);
    insets.right = getBorderSize(myBorders, BorderPosition.RIGHT);
    return insets;
  }

  static int getBorderSize(Map<BorderPosition, BorderInfo> map, BorderPosition position) {
    BorderInfo borderInfo = map.get(position);
    if (borderInfo == null) {
      return 0;
    }
    return JBUI.scale(borderInfo.getWidth());
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}
