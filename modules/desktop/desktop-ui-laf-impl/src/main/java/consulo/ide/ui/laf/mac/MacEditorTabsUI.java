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
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.laf.JBEditorTabsUI;
import consulo.ide.ui.laf.intellij.IntelliJEditorTabsUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  }

  @Override
  public void doPaintBackground(Graphics2D g, Rectangle clip, boolean vertical, Rectangle rectangle) {
    g.setColor(JBColor.border());
    g.fill(clip);

    final int x = rectangle.x;
    final int y = rectangle.y;
    g.setPaint(UIUtil.getPanelBackground());
    g.fillRect(x, rectangle.y, rectangle.width, rectangle.height + (vertical ? 1 : 0));
  }

  @Nonnull
  protected Color prepareColorForTab(@Nullable Color c) {
    if(c == null) {
      return Gray._255;
    }
    return new Color(c.getRed() * c.getRed() / 255, c.getGreen() * c.getGreen() / 255, c.getBlue() * c.getBlue() / 255);
  }

  @Override
  public void paintSelectionAndBorderImpl(Graphics2D g2d,
                                          Rectangle rect,
                                          IntelliJEditorTabsUI.ShapeInfo selectedShape,
                                          Insets insets,
                                          Color tabColor,
                                          boolean horizontalTabs) {
    g2d.setColor(prepareColorForTab(tabColor));
    g2d.fill(selectedShape.fillPath.getShape());
  }

  @Override
  protected ShapeInfo _computeSelectedLabelShape(JBTabsImpl tabs) {
    final ShapeInfo shape = new ShapeInfo();

    shape.path = tabs.getEffectiveLayout().createShapeTransform(tabs.getSize());
    shape.insets = shape.path.transformInsets(tabs.getLayoutInsets());
    shape.labelPath = shape.path.createTransform(tabs.getSelectedLabel().getBounds());

    shape.labelBottomY = shape.labelPath.getMaxY();
    shape.labelTopY = shape.labelPath.getY() + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? shape.labelPath.deltaY(1) : 0);
    shape.labelLeftX = shape.labelPath.getX() + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(1));
    shape.labelRightX = shape.labelPath.getMaxX();

    int leftX = shape.insets.left + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(1));

    shape.path.moveTo(leftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelBottomY);

    int lastX = shape.path.getWidth() - shape.path.deltaX(shape.insets.right);

    shape.path.lineTo(lastX, shape.labelBottomY);
    shape.path.lineTo(lastX, shape.labelBottomY);
    shape.path.lineTo(leftX, shape.labelBottomY );

    shape.path.closePath();
    shape.fillPath = shape.path.copy();

    return shape;
  }

  @Override
  public Color getBackground() {
    return Gray._142;
  }
}
