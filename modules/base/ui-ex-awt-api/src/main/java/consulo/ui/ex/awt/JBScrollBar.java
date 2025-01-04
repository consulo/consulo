// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.ui.ex.IdeGlassPane.TopComponent;
import consulo.ui.ex.awt.internal.ScrollSettings;
import consulo.ui.ex.awt.internal.laf.DefaultScrollBarUI;
import consulo.ui.ex.awt.scroll.TouchScrollUtil;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Our implementation of a scroll bar with the custom UI.
 * Also it provides a method to create custom UI for our custom L&Fs.
 *
 * @see #createUI(JComponent)
 */
public class JBScrollBar extends JScrollBar implements TopComponent, Interpolable {
  /**
   * This key defines a region painter, which is used by the custom ScrollBarUI
   * to draw additional paintings (i.e. error stripes) on the scrollbar's track.
   *
   * @see UIUtil#putClientProperty
   */
  public static final Key<RegionPainter<Object>> TRACK = ScrollBarUIConstants.TRACK;
  /**
   * This constraint should be used to add a component that will be shown before the scrollbar's track.
   * Note that the previously added leading component will be removed.
   *
   * @see #addImpl(Component, Object, int)
   */
  public static final String LEADING = "JB_SCROLL_BAR_LEADING_COMPONENT";
  /**
   * This constraint should be used to add a component that will be shown after the scrollbar's track.
   * Note that the previously added trailing component will be removed.
   *
   * @see #addImpl(Component, Object, int)
   */
  public static final String TRAILING = "JB_SCROLL_BAR_TRAILING_COMPONENT";

  public JBScrollBar() {
    this(VERTICAL);
  }

  public JBScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
    this(orientation, 0, 10, 0, 100);
  }

  public JBScrollBar(@JdkConstants.AdjustableOrientation int orientation, int value, int extent, int min, int max) {
    super(orientation, value, extent, min, max);
    setModel(new Model(value, extent, min, max));
    putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE); // fast scrolling for JDK 6
  }

  @Override
  protected void addImpl(Component component, Object name, int index) {
    Key<Component> key = LEADING.equals(name) ? DefaultScrollBarUI.LEADING : TRAILING.equals(name) ? DefaultScrollBarUI.TRAILING : null;
    if (key != null) {
      Component old = ComponentUtil.getClientProperty(this, key);
      ComponentUtil.putClientProperty(this, key, component);
      if (old != null) remove(old);
    }
    super.addImpl(component, name, index);
  }

  /**
   * Notifies glass pane that it should not process mouse event above the scrollbar's thumb.
   *
   * @param event the mouse event
   * @return {@code true} if glass pane can process the specified event, {@code false} otherwise
   */
  @Override
  public boolean canBePreprocessed(@Nonnull MouseEvent event) {
    return JBScrollPane.canBePreprocessed(event, this);
  }

  @Override
  public void setCurrentValue(int value) {
    super.setValue(value);
  }

  private JViewport getViewport() {
    Component parent = getParent();
    if (parent instanceof JScrollPane) {
      JScrollPane pane = (JScrollPane)parent;
      return pane.getViewport();
    }
    return null;
  }

  private static Font getViewFont(JViewport viewport) {
    if (viewport == null) return null;
    Component view = viewport.getView();
    return view == null ? null : view.getFont();
  }

  private Scrollable getScrollableViewToCalculateIncrement(Component view) {
    return view instanceof JTable || (view instanceof Scrollable && orientation == VERTICAL) ? (Scrollable)view : null;
  }


  private static final class Model extends DefaultBoundedRangeModel {
    private Model(int value, int extent, int min, int max) {
      super(value, extent, min, max);
    }
  }
}
