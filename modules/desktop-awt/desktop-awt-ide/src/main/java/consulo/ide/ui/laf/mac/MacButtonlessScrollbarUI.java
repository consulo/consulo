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

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.RegionPainter;
import com.intellij.util.ui.UIUtil;
import consulo.ui.plaf.ScrollBarUIConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 5/10/17
 */
public class MacButtonlessScrollbarUI extends BasicScrollBarUI {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new MacButtonlessScrollbarUI();
  }

  /**
   * @return color for thumb. return JBColor but for now dark variant is not supported
   */
  private static JBColor getThumbColor() {
    return new JBColor(Gray._200, Gray._80);
  }

  private static JBColor getTrackBackground() {
    return new JBColor(LightColors.SLIGHTLY_GRAY, UIUtil.getListBackground());
  }

  private static JBColor getTrackBorderColor() {
    return new JBColor(Gray._230, UIUtil.getListBackground());
  }

  private static final BasicStroke ourBorderStroke = new BasicStroke();

  private final MouseMotionAdapter myMouseMotionListener;
  private final MouseAdapter myMouseListener;

  private boolean myMouseIsOverThumb = false;

  protected MacButtonlessScrollbarUI() {
    myMouseMotionListener = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        boolean inside = isOverThumb(e.getPoint());
        if (inside != myMouseIsOverThumb) {
          myMouseIsOverThumb = inside;
          e.getComponent().repaint();
        }
      }
    };

    myMouseListener = new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (myMouseIsOverThumb) {
          myMouseIsOverThumb = false;
          e.getComponent().repaint();
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

  public int getDecrementButtonHeight() {
    return decrButton.getHeight();
  }

  public int getIncrementButtonHeight() {
    return incrButton.getHeight();
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
    scrollbar.addMouseListener(myMouseListener);
    scrollbar.addMouseMotionListener(myMouseMotionListener);
  }

  private boolean isOverThumb(Point p) {
    final Rectangle bounds = getThumbBounds();
    return bounds != null && bounds.contains(p);
  }

  @Override
  public Rectangle getThumbBounds() {
    return super.getThumbBounds();
  }

  @Override
  protected void uninstallListeners() {
    if (scrollTimer != null) {
      // it is already called otherwise
      super.uninstallListeners();
    }
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
    return isVertical() ? JBUI.size(thickness, thickness * 2) : JBUI.size(thickness * 2, thickness);
  }

  protected int getThickness() {
    return 12;
  }

  @Override
  public Dimension getMaximumSize(JComponent c) {
    int thickness = getThickness();
    return JBUI.size(thickness, thickness);
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
    int hGap = JBUI.scale(vertical ? 2 : 1);
    int vGap = JBUI.scale(vertical ? 1 : 2);

    int w = adjustThumbWidth(thumbBounds.width - hGap * 2);
    int h = thumbBounds.height - vGap * 2;

    GraphicsUtil.setupAAPainting(g);
    final Stroke stroke = g.getStroke();
    g.setStroke(ourBorderStroke);
    g.setColor(adjustColor(getThumbColor()));
    g.fillRoundRect(hGap, vGap, w, h, JBUI.scale(8), JBUI.scale(8));
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
    if (!myMouseIsOverThumb) {
      return c;
    }
    final int sign = UIUtil.isUnderDarkTheme() ? -1 : 1;
    int shift = UIUtil.isUnderDarkTheme() ? 20 : 40;
    return Gray.get(Math.max(0, Math.min(255, c.getRed() - sign * shift)));
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

