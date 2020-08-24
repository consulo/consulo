/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui.components;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.TouchScrollUtil;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public class JBScrollBar extends JScrollBar implements Interpolable {
  /**
   * This constraint should be used to add a component that will be shown before the scrollbar's track.
   * Note that the previously added leading component will be removed.
   *
   * @see #addImpl(Component, Object, int)
   */
  public static final String LEADING = "JB_SCROLL_BAR_LEADING_COMPONENT";

  private static final double THRESHOLD = 1D + 1E-5D;
  private final Interpolator myInterpolator = new Interpolator(this::getValue, this::setCurrentValue);
  private double myFractionalRemainder;
  private boolean wasPositiveDelta;

  public JBScrollBar() {
    init();
  }

  public JBScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
    super(orientation);
    init();
  }

  public JBScrollBar(@JdkConstants.AdjustableOrientation int orientation, int value, int extent, int min, int max) {
    super(orientation, value, extent, min, max);
    init();
  }

  private void init() {
    putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE); // fast scrolling for JDK 6
  }

  @Override
  public void setValue(int value) {
    int delay = 0;
    Component parent = getParent();
    if (parent instanceof JBScrollPane) {
      JBScrollPane pane = (JBScrollPane)parent;
      JViewport viewport = pane.getViewport();
      if (viewport != null && ScrollSettings.isEligibleFor(viewport.getView()) && ScrollSettings.isInterpolationEligibleFor(this)) {
        delay = pane.getInitialDelay(getValueIsAdjusting());
      }
    }
    if (delay > 0) {
      myInterpolator.setTarget(value, delay);
    }
    else {
      super.setValue(value);
    }
  }

  @Override
  public void setCurrentValue(int value) {
    super.setValue(value);
    myFractionalRemainder = 0.0;
  }

  @Override
  public int getTargetValue() {
    return myInterpolator.getTarget();
  }

  /**
   * Handles the mouse wheel events to scroll the scrollbar.
   *
   * @param event the mouse wheel event
   * @return {@code true} if the specified event is handled and consumed, {@code false} otherwise
   */
  public boolean handleMouseWheelEvent(MouseWheelEvent event) {
    if (!isSupportedScrollType(event)) return false;
    if (event.isShiftDown() == (orientation == VERTICAL)) return false;
    if (!ScrollSettings.isEligibleFor(this)) return false;

    double delta = getPreciseDelta(event);
    if (!Double.isFinite(delta)) return false;

    int value = getTargetValue();
    double deltaAdjusted = getDeltaAdjusted(value, delta);
    if (deltaAdjusted != 0.0) {
      boolean isPositiveDelta = deltaAdjusted > 0.0;
      if (wasPositiveDelta != isPositiveDelta) {
        // reset accumulator if scrolling direction is changed
        wasPositiveDelta = isPositiveDelta;
        myFractionalRemainder = 0.0;
      }
      deltaAdjusted += myFractionalRemainder;
      int valueAdjusted = (int)deltaAdjusted;
      if (valueAdjusted == 0) {
        myFractionalRemainder = deltaAdjusted;
      }
      else {
        myFractionalRemainder = deltaAdjusted - (double)valueAdjusted;
        setValue(value + valueAdjusted);
      }
    }
    else if (delta != 0.0) {
      return true; // do not consume event if it can be processed by parent component
    }
    event.consume();
    return true;
  }

  private static boolean isSupportedScrollType(MouseWheelEvent e) {
    return e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL || TouchScrollUtil.isUpdate(e);
  }

  private static Font getViewFont(JViewport viewport) {
    if (viewport == null) return null;
    Component view = viewport.getView();
    return view == null ? null : view.getFont();
  }

  private Scrollable getScrollableViewToCalculateIncrement(Component view) {
    return view instanceof JTable || (view instanceof Scrollable && orientation == VERTICAL) ? (Scrollable)view : null;
  }

  private static double boundDelta(double minDelta, double maxDelta, double delta) {
    return Math.max(minDelta, Math.min(maxDelta, delta));
  }

  protected double getDeltaAdjusted(MouseWheelEvent event) {
    int value = getTargetValue();
    double delta = getPreciseDelta(event);
    return getDeltaAdjusted(value, delta);
  }

  /**
   * Calculates adjusted delta for the bar.
   *
   * @param value the target value for the bar
   * @param delta the supposed delta
   * @return the delta itself or an adjusted delta
   */
  private double getDeltaAdjusted(int value, double delta) {
    double minDelta = getMinimum() - value;
    double maxDelta = getMaximum() - getVisibleAmount() - value;
    return Math.max(minDelta, Math.min(maxDelta, delta));
  }

  private JViewport getViewport() {
    Component parent = getParent();
    if (parent instanceof JScrollPane) {
      JScrollPane pane = (JScrollPane)parent;
      return pane.getViewport();
    }
    return null;
  }

  /**
   * Calculates a scrolling delta from the specified event.
   *
   * @param event the mouse wheel event
   * @return a scrolling delta for this scrollbar
   */
  private double getPreciseDelta(MouseWheelEvent event) {
    if (TouchScrollUtil.isTouchScroll(event)) {
      return TouchScrollUtil.getDelta(event);
    }
    double rotation = event.getPreciseWheelRotation();
    if (ScrollSettings.isPixelPerfectEnabled()) {
      // calculate an absolute delta if possible
      if (SystemInfo.isMac) {
        // Native code in our JDK for Mac uses 0.1 to convert pixels to units,
        // so we use 10 to restore amount of pixels to scroll.
        return 10 * rotation;
      }
      JViewport viewport = getViewport();
      Font font = viewport == null ? null : getViewFont(viewport);
      int size = font == null ? JBUIScale.scale(10) : font.getSize(); // assume an unit size
      return size * rotation * event.getScrollAmount();
    }
    if (ScrollSettings.isHighPrecisionEnabled()) {
      // calculate a relative delta if possible
      int direction = rotation < 0 ? -1 : 1;
      int unitIncrement = getUnitIncrement(direction);
      double delta = unitIncrement * rotation * event.getScrollAmount();
      if (-THRESHOLD > delta && delta > THRESHOLD) return delta;
      // When the scrolling speed is set to maximum, it's possible to scroll by more units than will fit in the visible area.
      // To make for more accurate low-speed scrolling, we limit scrolling to the block increment
      // if the wheel was only rotated one click.
      double blockIncrement = getBlockIncrement(direction);
      return boundDelta(-blockIncrement, blockIncrement, delta);
    }
    return Double.NaN;
  }


  //@Override
  //protected void addImpl(Component component, Object name, int index) {
  //  Key<Component> key = LEADING.equals(name) ? DefaultScrollBarUI.LEADING : TRAILING.equals(name) ? DefaultScrollBarUI.TRAILING : null;
  //  if (key != null) {
  //    Component old = UIUtil.getClientProperty(this, key);
  //    UIUtil.putClientProperty(this, key, component);
  //    if (old != null) remove(old);
  //  }
  //  super.addImpl(component, name, index);
  //}
}
