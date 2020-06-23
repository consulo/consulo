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
package consulo.ide.ui.laf.mac;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.laf.JBEditorTabsUI;
import consulo.ide.ui.laf.intellij.IntelliJEditorTabsUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-17
 */
public class MacEditorTabsUI extends IntelliJEditorTabsUI {
  public static JBEditorTabsUI createUI(JComponent c) {
    return new MacEditorTabsUI();
  }

  @Override
  public void doPaintInactiveImpl(Graphics2D g2d,
                                  Rectangle effectiveBounds,
                                  int x,
                                  int y,
                                  int w,
                                  int h,
                                  Color tabColor,
                                  int row,
                                  int column,
                                  boolean vertical) {
    g2d.setColor(Gray._255);
    g2d.fillRect(x, y, w, h);

    if (tabColor != null) {
      g2d.setColor(ColorUtil.toAlpha(tabColor, 200));
      g2d.fillRect(x, y, w, h);
    }

    g2d.setColor(Gray._150.withAlpha(100));
    g2d.fillRect(x, y, w, h);

    // Push top row under the navbar or toolbar and have a blink over previous row shadow for 2nd and subsequent rows.
    if (row == 0) {
      g2d.setColor(Gray._200.withAlpha(200));
    }
    else {
      g2d.setColor(Gray._255.withAlpha(100));
    }

    g2d.drawLine(x, y, x + w - 1, y);
  }

  @Override
  public void doPaintBackground(Graphics2D g, Rectangle clip, boolean vertical, Rectangle rectangle) {
    g.setColor(UIUtil.getPanelBackground());
    g.fill(clip);

    g.setColor(new Color(0, 0, 0, 80));
    g.fill(clip);

    final int x = rectangle.x;
    final int y = rectangle.y;
    g.setPaint(new Color(255, 255, 255, 160));
    g.fillRect(x, rectangle.y, rectangle.width, rectangle.height + (vertical ? 1 : 0));

    if (!vertical) {
      g.setColor(Gray._210);
      g.drawLine(x, rectangle.y, x + rectangle.width, rectangle.y);
    }
  }

  protected static Color multiplyColor(Color c) {
    return new Color(c.getRed() * c.getRed() / 255, c.getGreen() * c.getGreen() / 255, c.getBlue() * c.getBlue() / 255);
  }

  @Override
  public void paintSelectionAndBorderImpl(Graphics2D g2d,
                                          Rectangle rect,
                                          IntelliJEditorTabsUI.ShapeInfo selectedShape,
                                          Insets insets,
                                          Color tabColor,
                                          boolean horizontalTabs) {
    Insets i = selectedShape.path.transformInsets(insets);

    if (!horizontalTabs) {
      g2d.setColor(new Color(0, 0, 0, 45));
      g2d.draw(selectedShape.labelPath
                       .transformLine(i.left, selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(4), selectedShape.path.getMaxX(),
                                      selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(4)));

      g2d.setColor(new Color(0, 0, 0, 15));
      g2d.draw(selectedShape.labelPath
                       .transformLine(i.left, selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(5), selectedShape.path.getMaxX(),
                                      selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(5)));
    }

    tabColor = tabColor != null ? tabColor : Gray._255;
    g2d.setColor(multiplyColor(tabColor));
    g2d.fill(selectedShape.fillPath.getShape());

    g2d.setColor(Gray._255.withAlpha(180));
    g2d.draw(selectedShape.fillPath.getShape());

    // fix right side due to swing stupidity (fill & draw will occupy different shapes)
    g2d.draw(selectedShape.labelPath.transformLine(selectedShape.labelPath.getMaxX() - selectedShape.labelPath.deltaX(1),
                                                   selectedShape.labelPath.getY() + selectedShape.labelPath.deltaY(1),
                                                   selectedShape.labelPath.getMaxX() - selectedShape.labelPath.deltaX(1),
                                                   selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(4)));

    if (!horizontalTabs) {
      // side shadow
      g2d.setColor(Gray._0.withAlpha(30));
      g2d.draw(selectedShape.labelPath.transformLine(selectedShape.labelPath.getMaxX() + selectedShape.labelPath.deltaX(1),
                                                     selectedShape.labelPath.getY() + selectedShape.labelPath.deltaY(1),
                                                     selectedShape.labelPath.getMaxX() + selectedShape.labelPath.deltaX(1),
                                                     selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(4)));


      g2d.draw(selectedShape.labelPath.transformLine(selectedShape.labelPath.getX() - selectedShape.labelPath.deltaX(horizontalTabs ? 2 : 1),
                                                     selectedShape.labelPath.getY() + selectedShape.labelPath.deltaY(1),
                                                     selectedShape.labelPath.getX() - selectedShape.labelPath.deltaX(horizontalTabs ? 2 : 1),
                                                     selectedShape.labelPath.getMaxY() - selectedShape.labelPath.deltaY(4)));
    }

    g2d.setColor(new Color(0, 0, 0, 50));
    g2d.draw(selectedShape.labelPath.transformLine(i.left, selectedShape.labelPath.getMaxY(), selectedShape.path.getMaxX(), selectedShape.labelPath.getMaxY()));
  }

  @Override
  public Color getBackground() {
    return Gray._142;
  }
}
