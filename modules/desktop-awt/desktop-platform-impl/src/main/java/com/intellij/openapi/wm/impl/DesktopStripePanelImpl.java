/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.wm.impl.ToolWindowManagerBase;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
final class DesktopStripePanelImpl extends JPanel {
  private final int myAnchor;
  private final ArrayList<DesktopStripeButton> myButtons = new ArrayList<>();
  private final MyKeymapManagerListener myWeakKeymapManagerListener;
  private final MyUISettingsListener myUISettingsListener;

  private Dimension myPrefSize;
  private DesktopStripeButton myDragButton;
  private Rectangle myDropRectangle;
  private final ToolWindowManagerBase myManager;
  private JComponent myDragButtonImage;
  private LayoutData myLastLayoutData;
  private boolean myFinishingDrop;
  static final int DROP_DISTANCE_SENSIVITY = 20;
  private final Disposable myDisposable = Disposable.newDisposable();

  DesktopStripePanelImpl(final int anchor, ToolWindowManagerBase manager) {
    super(new GridBagLayout());
    setOpaque(true);
    myManager = manager;
    myAnchor = anchor;
    myWeakKeymapManagerListener = new MyKeymapManagerListener();
    myUISettingsListener = new MyUISettingsListener();
    setBorder(new AdaptiveBorder());
  }

  private static class AdaptiveBorder implements Border {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Insets insets = ((JComponent)c).getInsets();
      g.setColor(UIUtil.CONTRAST_BORDER_COLOR);
      drawBorder((Graphics2D)g, x, y, width, height, insets);
    }

    private static void drawBorder(Graphics2D g, int x, int y, int width, int height, Insets insets) {
      if (insets.top == 1) {
        LinePainter2D.paint(g, x, y, x + width, y);
      }
      if (insets.right == 1) {
        LinePainter2D.paint(g, x + width - 1, y, x + width - 1, y + height);
      }
      if (insets.left == 1) {
        LinePainter2D.paint(g, x, y, x, y + height);
      }
      if (insets.bottom == 1) {
        LinePainter2D.paint(g, x, y + height - 1, x + width, y + height - 1);
      }

      if (!UIUtil.isUnderDarcula()) {
        return;
      }

      Color c = g.getColor();
      if (insets.top == 2) {
        g.setColor(c);
        LinePainter2D.paint(g, x, y, x + width, y);
        g.setColor(Gray._85);
        LinePainter2D.paint(g, x, y + 1, x + width, y + 1);
      }
      if (insets.right == 2) {
        g.setColor(Gray._85);
        LinePainter2D.paint(g, x + width - 1, y, x + width - 1, y + height);
        g.setColor(c);
        LinePainter2D.paint(g, x + width - 2, y, x + width - 2, y + height);
      }
      if (insets.left == 2) {
        g.setColor(Gray._85);
        LinePainter2D.paint(g, x + 1, y, x + 1, y + height);
        g.setColor(c);
        LinePainter2D.paint(g, x, y, x, y + height);
      }
    }

    @SuppressWarnings("UseDPIAwareInsets")
    @Override
    public Insets getBorderInsets(Component c) {
      DesktopStripePanelImpl stripe = (DesktopStripePanelImpl)c;
      ToolWindowAnchor anchor = stripe.getAnchor();
      if (anchor == ToolWindowAnchor.LEFT) {
        return new Insets(1, 0, 0, 1);
      }
      else if (anchor == ToolWindowAnchor.RIGHT) {
        return new Insets(1, 1, 0, 0);
      }
      else if (anchor == ToolWindowAnchor.TOP) {
        return new Insets(1, 0, 0, 0);
      }
      else {
        return new Insets(1, 0, 0, 0);
      }
    }

    @Override
    public boolean isBorderOpaque() {
      return true;
    }
  }

  /**
   * Invoked when enclosed frame is being shown.
   */
  @Override
  public void addNotify() {
    super.addNotify();
    updatePresentation();
    KeymapManagerEx.getInstanceEx().addWeakListener(myWeakKeymapManagerListener);
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      UISettings.getInstance().addUISettingsListener(myUISettingsListener, myDisposable);
    }
  }

  /**
   * Invoked when enclosed frame is being disposed.
   */
  @Override
  public void removeNotify() {
    KeymapManagerEx.getInstanceEx().removeWeakListener(myWeakKeymapManagerListener);
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      Disposer.dispose(myDisposable);
    }
    super.removeNotify();
  }

  void addButton(final DesktopStripeButton button, final Comparator<ToolWindowStripeButton> comparator) {
    myPrefSize = null;
    myButtons.add(button);
    Collections.sort(myButtons, comparator);
    add(button);
    revalidate();
  }

  void removeButton(final DesktopStripeButton button) {
    myPrefSize = null;
    myButtons.remove(button);
    remove(button);
    revalidate();
  }

  public List<DesktopStripeButton> getButtons() {
    return Collections.unmodifiableList(myButtons);
  }

  @Override
  public void invalidate() {
    myPrefSize = null;
    super.invalidate();
  }

  @Override
  public void doLayout() {
    if (!myFinishingDrop) {
      myLastLayoutData = recomputeBounds(true, getSize());
    }
  }

  private LayoutData recomputeBounds(boolean setBounds, Dimension toFitWith) {
    return recomputeBounds(setBounds, toFitWith, false);
  }

  private LayoutData recomputeBounds(boolean setBounds, Dimension toFitWith, boolean noDrop) {
    final LayoutData data = new LayoutData();
    final int horizontaloffset = getHeight() - 2;

    data.eachY = 0;
    data.size = new Dimension();
    data.gap = 0;
    data.horizontal = isHorizontal();
    data.dragInsertPosition = -1;
    if (data.horizontal) {
      data.eachX = horizontaloffset - 1;
      data.eachY = 1;
    }
    else {
      data.eachX = 0;
    }

    data.fitSize = toFitWith != null ? toFitWith : new Dimension();

    final Rectangle stripeSensetiveRec =
            new Rectangle(-DROP_DISTANCE_SENSIVITY, -DROP_DISTANCE_SENSIVITY, getWidth() + DROP_DISTANCE_SENSIVITY * 2,
                          getHeight() + DROP_DISTANCE_SENSIVITY * 2);
    boolean processDrop = isDroppingButton() && stripeSensetiveRec.intersects(myDropRectangle) && !noDrop;

    if (toFitWith == null) {
      for (DesktopStripeButton eachButton : myButtons) {
        if (!isConsideredInLayout(eachButton)) continue;
        final Dimension eachSize = eachButton.getPreferredSize();
        data.fitSize.width = Math.max(eachSize.width, data.fitSize.width);
        data.fitSize.height = Math.max(eachSize.height, data.fitSize.height);
      }
    }

    int gap = 0;
    if (toFitWith != null) {
      LayoutData layoutData = recomputeBounds(false, null, true);
      if (data.horizontal) {
        gap = toFitWith.width - horizontaloffset - layoutData.size.width - data.eachX;
      }
      else {
        gap = toFitWith.height - layoutData.size.height - data.eachY;
      }

      if (processDrop) {
        if (data.horizontal) {
          gap -= myDropRectangle.width + data.gap;
        }
        else {
          gap -= myDropRectangle.height + data.gap;
        }
      }
      gap = Math.max(gap, 0);
    }

    int insertOrder = -1;
    boolean sidesStarted = false;

    for (DesktopStripeButton eachButton : getButtonsToLayOut()) {
      insertOrder = eachButton.getDecorator().getWindowInfo().getOrder();
      final Dimension eachSize = eachButton.getPreferredSize();

      if (!sidesStarted && eachButton.getWindowInfo().isSplit()) {
        if (processDrop) {
          tryDroppingOnGap(data, gap, eachButton.getWindowInfo().getOrder());
        }
        if (data.horizontal) {
          data.eachX += gap;
          data.size.width += gap;
        }
        else {
          data.eachY += gap;
          data.size.height += gap;
        }
        sidesStarted = true;
      }

      if (processDrop && !data.dragTargetChoosen) {
        if (data.horizontal) {
          int distance = myDropRectangle.x - data.eachX;
          if (distance < eachSize.width / 2 || (myDropRectangle.x + myDropRectangle.width) < eachSize.width / 2) {
            layoutButton(data, myDragButtonImage, false);
            data.dragInsertPosition = insertOrder;
            data.dragToSide = sidesStarted;
            data.dragTargetChoosen = true;
          }
        }
        else {
          int distance = myDropRectangle.y - data.eachY;
          if (distance < eachSize.height / 2 || (myDropRectangle.y + myDropRectangle.height) < eachSize.height / 2) {
            layoutButton(data, myDragButtonImage, false);
            data.dragInsertPosition = insertOrder;
            data.dragToSide = sidesStarted;
            data.dragTargetChoosen = true;
          }
        }
      }

      layoutButton(data, eachButton, setBounds);
    }

    if (!sidesStarted && processDrop) {
      tryDroppingOnGap(data, gap, -1);
    }


    if (isDroppingButton()) {
      final Dimension dragSize = myDragButton.getPreferredSize();
      if (getAnchor().isHorizontal() == myDragButton.getWindowInfo().getAnchor().isHorizontal()) {
        data.size.width = Math.max(data.size.width, dragSize.width);
        data.size.height = Math.max(data.size.height, dragSize.height);
      }
      else {
        data.size.width = Math.max(data.size.width, dragSize.height);
        data.size.height = Math.max(data.size.height, dragSize.width);
      }
    }

    if (processDrop && !data.dragTargetChoosen) {
      data.dragInsertPosition = -1;
      data.dragToSide = true;
      data.dragTargetChoosen = true;
    }

    return data;
  }

  private void tryDroppingOnGap(final LayoutData data, final int gap, final int insertOrder) {
    if (data.dragTargetChoosen) return;

    int nonSideDistance;
    int sideDistance;
    if (data.horizontal) {
      nonSideDistance = myDropRectangle.x - data.eachX;
      sideDistance = data.eachX + gap - myDropRectangle.x;
    }
    else {
      nonSideDistance = myDropRectangle.y - data.eachY;
      sideDistance = data.eachY + gap - myDropRectangle.y;
    }
    nonSideDistance = Math.max(0, nonSideDistance);

    if (sideDistance > 0) {
      if (nonSideDistance > sideDistance) {
        data.dragInsertPosition = insertOrder;
        data.dragToSide = true;
        data.dragTargetChoosen = true;
      }
      else {
        data.dragInsertPosition = -1;
        data.dragToSide = false;
        data.dragTargetChoosen = true;
      }

      layoutButton(data, myDragButtonImage, false);
    }
  }

  private List<DesktopStripeButton> getButtonsToLayOut() {
    List<DesktopStripeButton> result = new ArrayList<>();

    List<DesktopStripeButton> tools = new ArrayList<>();
    List<DesktopStripeButton> sideTools = new ArrayList<>();

    for (DesktopStripeButton b : myButtons) {
      if (!isConsideredInLayout(b)) continue;

      if (b.getWindowInfo().isSplit()) {
        sideTools.add(b);
      }
      else {
        tools.add(b);
      }
    }

    result.addAll(tools);
    result.addAll(sideTools);

    return result;
  }


  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.get(myAnchor);
  }

  private static void layoutButton(final LayoutData data, final JComponent eachButton, boolean setBounds) {
    final Dimension eachSize = eachButton.getPreferredSize();
    if (setBounds) {
      final int width = data.horizontal ? eachSize.width : data.fitSize.width;
      final int height = data.horizontal ? data.fitSize.height : eachSize.height;
      eachButton.setBounds(data.eachX, data.eachY, width, height);
    }
    if (data.horizontal) {
      final int deltaX = eachSize.width + data.gap;
      data.eachX += deltaX;
      data.size.width += deltaX;
      data.size.height = eachSize.height;
    }
    else {
      final int deltaY = eachSize.height + data.gap;
      data.eachY += deltaY;
      data.size.width = eachSize.width;
      data.size.height += deltaY;
    }
    data.processedComponents++;
  }

  public void startDrag() {
    revalidate();
    repaint();
  }

  public void stopDrag() {
    revalidate();
    repaint();
  }

  public DesktopStripeButton getButtonFor(final String toolWindowId) {
    for (DesktopStripeButton each : myButtons) {
      if (each.getWindowInfo().getId().equals(toolWindowId)) return each;
    }
    return null;
  }

  public void setOverlayed(boolean overlayed) {
    if (Registry.is("disable.toolwindow.overlay")) return;

    Color bg = UIUtil.getPanelBackground();
    if (UIUtil.isUnderAquaLookAndFeel()) {
      float[] result = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), new float[3]);
      bg = new Color(Color.HSBtoRGB(result[0], result[1], result[2] - 0.08f > 0 ? result[2] - 0.08f : result[2]));
    }
    if (overlayed) {
      setBackground(ColorUtil.toAlpha(bg, 190));
    }
    else {
      setBackground(bg);
    }
  }

  private static class LayoutData {
    int eachX;
    int eachY;
    int gap;
    Dimension size;
    Dimension fitSize;
    boolean horizontal;
    int processedComponents;

    boolean dragTargetChoosen;
    boolean dragToSide;
    int dragInsertPosition;
  }

  private boolean isHorizontal() {
    return myAnchor == SwingConstants.TOP || myAnchor == SwingConstants.BOTTOM;
  }

  @Override
  public Dimension getPreferredSize() {
    if (myPrefSize == null) {
      myPrefSize = recomputeBounds(false, null).size;
    }

    return myPrefSize;
  }

  void updatePresentation() {
    for (DesktopStripeButton button : myButtons) {
      button.updatePresentation();
    }
  }

  public boolean containsScreen(final Rectangle screenRec) {
    final Point point = screenRec.getLocation();
    SwingUtilities.convertPointFromScreen(point, this);
    return new Rectangle(point, screenRec.getSize()).intersects(
            new Rectangle(-DROP_DISTANCE_SENSIVITY,
                          -DROP_DISTANCE_SENSIVITY,
                          getWidth() + DROP_DISTANCE_SENSIVITY,
                          getHeight() + DROP_DISTANCE_SENSIVITY)
    );
  }

  public void finishDrop() {
    if (myLastLayoutData == null || !isDroppingButton()) return;

    final WindowInfoImpl info = myDragButton.getDecorator().getWindowInfo();
    myFinishingDrop = true;
    myManager.setSideToolAndAnchor(info.getId(), ToolWindowAnchor.get(myAnchor), myLastLayoutData.dragInsertPosition,
                                   myLastLayoutData.dragToSide);

    myManager.invokeLater(this::resetDrop);
  }

  public void resetDrop() {
    myDragButton = null;
    myDragButtonImage = null;
    myFinishingDrop = false;
    myPrefSize = null;
    revalidate();
    repaint();
  }

  public void processDropButton(final DesktopStripeButton button, JComponent buttonImage, Point screenPoint) {
    if (!isDroppingButton()) {
      final BufferedImage image = UIUtil.createImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_RGB);
      buttonImage.paint(image.getGraphics());
      myDragButton = button;
      myDragButtonImage = buttonImage;
      myPrefSize = null;
    }

    final Point point = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(point, this);

    myDropRectangle = new Rectangle(point, buttonImage.getSize());

    revalidate();
    repaint();
  }

  private boolean isDroppingButton() {
    return myDragButton != null;
  }

  private boolean isConsideredInLayout(final DesktopStripeButton each) {
    return each.isVisible();
  }

  private final class MyKeymapManagerListener implements KeymapManagerListener {
    @Override
    public void activeKeymapChanged(final Keymap keymap) {
      updatePresentation();
    }
  }

  private final class MyUISettingsListener implements UISettingsListener {
    @Override
    public void uiSettingsChanged(UISettings source) {
      updatePresentation();
    }
  }


  public String toString() {
    String anchor = null;
    switch (myAnchor) {
      case SwingConstants.TOP:
        anchor = "TOP";
        break;
      case SwingConstants.BOTTOM:
        anchor = "BOTTOM";
        break;
      case SwingConstants.LEFT:
        anchor = "LEFT";
        break;
      case SwingConstants.RIGHT:
        anchor = "RIGHT";
        break;
    }
    return getClass().getName() + " " + anchor;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    if (!myFinishingDrop && isDroppingButton() && myDragButton.getParent() != this) {
      g.setColor(getBackground().brighter());
      g.fillRect(0, 0, getWidth(), getHeight());
    }
    if (UIUtil.isUnderDarcula()) return;
    ToolWindowAnchor anchor = getAnchor();
    g.setColor(new Color(255, 255, 255, 40));
    Rectangle r = getBounds();
    if (anchor == ToolWindowAnchor.LEFT || anchor == ToolWindowAnchor.RIGHT) {
      g.drawLine(0, 0, 0, r.height);
      g.drawLine(r.width - 2, 0, r.width - 2, r.height);
    }
    else {
      g.drawLine(0, 1, r.width, 1);
      g.drawLine(0, r.height - 1, r.width, r.height - 1);
    }
  }
}
