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
package com.intellij.util.ui;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import consulo.annotation.DeprecationInfo;
import consulo.ui.plaf.ScrollBarUIConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Supplier;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo("User cant create ButtonlessScrollBarUI. Implementation of UI is stored in Laf")
public class ButtonlessScrollBarUI extends BasicScrollBarUI {
  public static JBColor getGradientLightColor() {
    return new JBColor(Gray._251, Gray._95);
  }

  public static JBColor getGradientDarkColor() {
    return new JBColor(Gray._251, Gray._80);
  }

  private static JBColor getGradientThumbBorderColor() {
    return new JBColor(Gray._201, Gray._85);
  }

  public static JBColor getTrackBackground() {
    return new JBColor(LightColors.SLIGHTLY_GRAY, UIUtil.getListBackground());
  }

  public static JBColor getTrackBorderColor() {
    return new JBColor(Gray._230, UIUtil.getListBackground());
  }

  private static final BasicStroke BORDER_STROKE = new BasicStroke();

  private static int getAnimationColorShift() {
    return 20;
  }

  private final AdjustmentListener myAdjustmentListener;
  private final MouseMotionAdapter myMouseMotionListener;
  private final MouseAdapter myMouseListener;

  private boolean myMouseIsOverThumb = false;

  protected ButtonlessScrollBarUI() {
    myAdjustmentListener = e -> repaint();

    myMouseMotionListener = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        boolean inside = isOverThumb(e.getPoint());
        if (inside != myMouseIsOverThumb) {
          myMouseIsOverThumb = inside;
          repaint();
        }
      }
    };

    myMouseListener = new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (myMouseIsOverThumb) {
          myMouseIsOverThumb = false;
          repaint();
        }
      }
    };
  }

  @Override
  public void layoutContainer(Container scrollbarContainer) {
    try {
      super.layoutContainer(scrollbarContainer);
    }
    catch (NullPointerException ignore) {
      //installUI is not performed yet or uninstallUI has set almost every field to null. Just ignore it //IDEA-89674
    }
  }

  @Override
  protected ModelListener createModelListener() {
    return new ModelListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (scrollbar != null) {
          super.stateChanged(e);
        }
      }
    };
  }

  private void repaint() {
    scrollbar.repaint(getThumbBounds());
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    scrollbar.setFocusable(false);
  }

  @Override
  protected void installDefaults() {
    final int incGap = UIManager.getInt("ScrollBar.incrementButtonGap");
    final int decGap = UIManager.getInt("ScrollBar.decrementButtonGap");
    try {
      UIManager.put("ScrollBar.incrementButtonGap", 0);
      UIManager.put("ScrollBar.decrementButtonGap", 0);
      super.installDefaults();
    }
    finally {
      UIManager.put("ScrollBar.incrementButtonGap", incGap);
      UIManager.put("ScrollBar.decrementButtonGap", decGap);
    }
  }

  @Override
  protected void installListeners() {
    super.installListeners();
    scrollbar.addAdjustmentListener(myAdjustmentListener);
    scrollbar.addMouseListener(myMouseListener);
    scrollbar.addMouseMotionListener(myMouseMotionListener);
  }

  private boolean isOverThumb(Point p) {
    final Rectangle bounds = getThumbBounds();
    return bounds != null && bounds.contains(p);
  }

  @Override
  protected void uninstallListeners() {
    if (scrollTimer != null) {
      // it is already called otherwise
      super.uninstallListeners();
    }
    scrollbar.removeAdjustmentListener(myAdjustmentListener);
    scrollbar.removeMouseListener(myMouseListener);
    scrollbar.removeMouseMotionListener(myMouseMotionListener);
  }

  @Override
  protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
    g.setColor(getTrackBackground());
    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

    g.setColor(getTrackBorderColor());
    if (isVertical()) {
      g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height);
    }
    else {
      g.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y);
    }

    RegionPainter<Object> painter = UIUtil.getClientProperty(c, ScrollBarUIConstants.TRACK);
    if (painter != null) {
      painter.paint((Graphics2D)g, bounds.x, bounds.y, bounds.width, bounds.height, null);
    }
  }

  @Override
  protected Dimension getMinimumThumbSize() {
    final int thickness = getThickness();
    return isVertical() ? new Dimension(thickness, thickness * 2) : new Dimension(thickness * 2, thickness);
  }

  protected int getThickness() {
    return JBUI.scale(13);
  }

  @Override
  public Dimension getMaximumSize(JComponent c) {
    int thickness = getThickness();
    return new Dimension(thickness, thickness);
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    return getMaximumSize(c);
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    return getMaximumSize(c);
  }

  @Override
  protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
      return;
    }

    g.translate(thumbBounds.x, thumbBounds.y);
    paintMaxiThumb((Graphics2D)g, thumbBounds);
    g.translate(-thumbBounds.x, -thumbBounds.y);
  }

  private void paintMaxiThumb(Graphics2D g, Rectangle thumbBounds) {
    final boolean vertical = isVertical();
    int hGap = 0;
    int vGap = 0;

    int w = adjustThumbWidth(thumbBounds.width - hGap * 2);
    int h = thumbBounds.height - vGap * 2;

    final Paint paint;
    final Color start = adjustColor(getGradientLightColor());
    final Color end = adjustColor(getGradientDarkColor());

    if (vertical) {
      paint = UIUtil.getGradientPaint(1, 0, start, w + 1, 0, end);
    }
    else {
      paint = UIUtil.getGradientPaint(0, 1, start, 0, h + 1, end);
    }

    g.setPaint(paint);
    g.fillRect(hGap + 1, vGap + 1, w - 1, h - 1);

    final Stroke stroke = g.getStroke();
    g.setStroke(BORDER_STROKE);
    g.setColor(getGradientThumbBorderColor());
    g.drawRoundRect(hGap, vGap, w, h, 0, 0);
    g.setStroke(stroke);
  }

  @Override
  public boolean getSupportsAbsolutePositioning() {
    return true;
  }

  protected int adjustThumbWidth(int width) {
    return width;
  }

  protected Color adjustColor(Color c) {
    if (!myMouseIsOverThumb) return c;
    final int sign = UIUtil.isUnderDarcula() ? -1 : 1;
    return Gray.get(Math.max(0, Math.min(255, c.getRed() - sign * getAnimationColorShift())));
  }

  private boolean isVertical() {
    return scrollbar.getOrientation() == Adjustable.VERTICAL;
  }

  @Override
  protected JButton createIncreaseButton(int orientation) {
    Supplier<? extends JButton> property = UIUtil.getClientProperty(scrollbar, ScrollBarUIConstants.INCREASE_BUTTON_FACTORY);
    return property == null ? new EmptyButton() : property.get();
  }

  @Override
  protected JButton createDecreaseButton(int orientation) {
    return new EmptyButton();
  }

  private static class EmptyButton extends JButton {
    private EmptyButton() {
      setFocusable(false);
      setRequestFocusEnabled(false);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(0, 0);
    }

    @Override
    public Dimension getPreferredSize() {
      return getMaximumSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return getMaximumSize();
    }
  }
}
