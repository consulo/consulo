// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.ui.ex.IdeGlassPane.TopComponent;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.util.dataholder.Key;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Our implementation of a scroll bar with the custom UI.
 * Also it provides a method to create custom UI for our custom L&Fs.
 *
 * @see #createUI(JComponent)
 */
@Deprecated
public class JBScrollBar extends JScrollBar implements TopComponent {
    public static final Key<Component> LEADING_KEY = Key.create("JB_SCROLL_BAR_LEADING_COMPONENT");
    public static final Key<Component> TRAILING_KEY = Key.create("JB_SCROLL_BAR_TRAILING_COMPONENT");

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

    private boolean isUnitIncrementSet;
    private boolean isBlockIncrementSet;

    public JBScrollBar() {
        this(VERTICAL);
    }

    public JBScrollBar(@AWTConstants.AdjustableOrientation int orientation) {
        this(orientation, 0, 10, 0, 100);
    }

    public JBScrollBar(@AWTConstants.AdjustableOrientation int orientation, int value, int extent, int min, int max) {
        super(orientation, value, extent, min, max);
        setModel(new DefaultBoundedRangeModel(value, extent, min, max));
    }

    @Override
    protected void addImpl(Component component, Object name, int index) {
        Key<Component> key = LEADING.equals(name) ? LEADING_KEY : TRAILING.equals(name) ? TRAILING_KEY : null;
        if (key != null) {
            Component old = ComponentUtil.getClientProperty(this, key);
            ComponentUtil.putClientProperty(this, key, component);
            if (old != null) {
                remove(old);
            }
        }
        super.addImpl(component, name, index);
    }

    /**
     * Computes the unit increment for scrolling if the viewport's view.
     * Otherwise returns {@code super.getUnitIncrement}.
     *
     * @param direction less than zero to scroll up/left, greater than zero for down/right
     * @return an integer, in pixels, containing the unit increment
     * @see Scrollable#getScrollableUnitIncrement
     */
    @Override
    public int getUnitIncrement(int direction) {
        JViewport viewport = getViewport();
        if (viewport != null) {
            Scrollable scrollable = getScrollableViewToCalculateIncrement(viewport.getView());
            if (scrollable != null) {
                return scrollable.getScrollableUnitIncrement(viewport.getViewRect(), orientation, direction);
            }
            if (!isUnitIncrementSet) {
                Font font = getViewFont(viewport);
                if (font != null) {
                    return font.getSize();
                }
            }
        }
        return super.getUnitIncrement(direction);
    }

    @Override
    public void setUnitIncrement(int increment) {
        isUnitIncrementSet = true;
        super.setUnitIncrement(increment);
    }

    /**
     * Computes the block increment for scrolling if the viewport's view.
     * Otherwise returns {@code super.getBlockIncrement}.
     *
     * @param direction less than zero to scroll up/left, greater than zero for down/right
     * @return an integer, in pixels, containing the block increment
     * @see Scrollable#getScrollableBlockIncrement
     */
    @Override
    public int getBlockIncrement(int direction) {
        JViewport viewport = getViewport();
        if (viewport != null) {
            Scrollable scrollable = getScrollableViewToCalculateIncrement(viewport.getView());
            if (scrollable != null) {
                return scrollable.getScrollableBlockIncrement(viewport.getViewRect(), orientation, direction);
            }
            if (!isBlockIncrementSet) {
                Dimension size = viewport.getExtentSize();
                return orientation == HORIZONTAL ? size.width : size.height;
            }
        }
        return super.getBlockIncrement(direction);
    }

    @Override
    public void setBlockIncrement(int increment) {
        isBlockIncrementSet = true;
        super.setBlockIncrement(increment);
    }

    /**
     * Notifies glass pane that it should not process mouse event above the scrollbar's thumb.
     *
     * @param event the mouse event
     * @return {@code true} if glass pane can process the specified event, {@code false} otherwise
     */
    @Override
    public boolean canBePreprocessed(MouseEvent event) {
        return JBScrollPane.canBePreprocessed(event, this);
    }

    private JViewport getViewport() {
        Component parent = getParent();
        if (parent instanceof JScrollPane pane) {
            return pane.getViewport();
        }
        return null;
    }

    private static Font getViewFont(JViewport viewport) {
        if (viewport == null) {
            return null;
        }
        Component view = viewport.getView();
        return view == null ? null : view.getFont();
    }

    private Scrollable getScrollableViewToCalculateIncrement(Component view) {
        return view instanceof JTable || (view instanceof Scrollable && orientation == VERTICAL) ? (Scrollable) view : null;
    }
}
