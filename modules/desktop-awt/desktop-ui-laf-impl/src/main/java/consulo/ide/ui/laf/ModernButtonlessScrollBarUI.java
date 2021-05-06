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
package consulo.ide.ui.laf;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.RegionPainter;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.plaf.ScrollBarUIConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.ComponentUI;
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
 * @author VISTALL
 */
@Deprecated
@DeprecationInfo("Prefer consulo.ide.ui.laf.mac.MacButtonlessScrollbarUI")
public class ModernButtonlessScrollBarUI extends BasicScrollBarUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernButtonlessScrollBarUI();
  }

  private final AdjustmentListener myAdjustmentListener;
  private final MouseMotionAdapter myMouseMotionListener;
  private final MouseAdapter myMouseListener;
  private Supplier<JButton> myIncreaseButtonFactory = EmptyButton::new;

  private boolean myMouseIsOverThumb = false;

  protected ModernButtonlessScrollBarUI() {
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

  public int getDecrementButtonHeight() {
    return decrButton.getHeight();
  }

  public int getIncrementButtonHeight() {
    return incrButton.getHeight();
  }

  private void repaint() {
    scrollbar.repaint(((ModernButtonlessScrollBarUI)scrollbar.getUI()).getThumbBounds());
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
  public Rectangle getThumbBounds() {
    return super.getThumbBounds();
  }

  @Override
  protected void uninstallListeners() {
    if (scrollTimer != null) {
      // it is already called otherwise
      super.uninstallListeners();
    }
    scrollbar.removeAdjustmentListener(myAdjustmentListener);
    //  Disposer.dispose(myAnimator);
  }

  @Override
  protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
    g.setColor(new JBColor(LightColors.SLIGHTLY_GRAY, UIUtil.getListBackground()));
    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

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
    return JBUI.scale(10);
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
    paintMaxiThumb((Graphics2D)g, c, thumbBounds);
    g.translate(-thumbBounds.x, -thumbBounds.y);
  }

  @Override
  protected void setThumbBounds(int x, int y, int width, int height) {
    if ((thumbRect.x == x) &&
        (thumbRect.y == y) &&
        (thumbRect.width == width) &&
        (thumbRect.height == height)) {
      return;
    }

    int minX = Math.min(x, thumbRect.x);
    int minY = Math.min(y, thumbRect.y);
    int maxX = Math.max(x + width, thumbRect.x + thumbRect.width);
    int maxY = Math.max(y + height, thumbRect.y + thumbRect.height);

    thumbRect.setBounds(x, y, width, height);
    scrollbar.repaint(minX - JBUI.scale(1), minY - JBUI.scale(1), (maxX - minX) + JBUI.scale(2), (maxY - minY) + JBUI.scale(2));
  }

  private void paintMaxiThumb(Graphics2D g, JComponent c, Rectangle thumbBounds) {
    final boolean vertical = isVertical();
    int hGap = 0;
    int vGap = 0;

    int w = thumbBounds.width;
    int h = thumbBounds.height;

    if (vertical) {
      vGap -= JBUI.scale(1);
      h += JBUI.scale(1);
    }
    else {
      hGap -= JBUI.scale(1);
      w += JBUI.scale(1);
    }

    g.setColor(adjustColor(new JBColor(Gray._230, Gray._80)));
    g.fillRect(hGap, vGap, w, h);

    g.setColor(new JBColor(Gray._201, Gray._85));
    g.drawRect(hGap, vGap, w, h);
  }

  @Override
  public boolean getSupportsAbsolutePositioning() {
    return true;
  }

  protected Color adjustColor(Color c) {
    if (!myMouseIsOverThumb) return c;
    final int sign = UIUtil.isUnderDarkTheme() ? -1 : 1;
    return Gray.get(Math.max(0, Math.min(255, c.getRed() - sign * 20)));
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
