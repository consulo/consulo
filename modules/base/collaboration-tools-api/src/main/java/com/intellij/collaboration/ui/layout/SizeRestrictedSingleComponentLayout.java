// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.layout;

import com.intellij.collaboration.ui.util.DimensionRestrictions;
import consulo.ui.ex.awt.JBInsets;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * Wraps a single component limiting its size to {@link #minSize} - {@link #maxSize} and overriding the preferred size with {@link #prefSize}
 */
public final class SizeRestrictedSingleComponentLayout implements LayoutManager2 {
    private @Nonnull DimensionRestrictions minSize = DimensionRestrictions.None;
    private @Nonnull DimensionRestrictions maxSize = DimensionRestrictions.None;
    private @Nonnull DimensionRestrictions prefSize = DimensionRestrictions.None;
    private @Nullable Component component;

    public @Nonnull DimensionRestrictions getMinSize() {
        return minSize;
    }

    public void setMinSize(@Nonnull DimensionRestrictions value) {
        minSize = value;
        if (component != null) {
            component.revalidate();
        }
    }

    public @Nonnull DimensionRestrictions getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(@Nonnull DimensionRestrictions value) {
        maxSize = value;
        if (component != null) {
            component.revalidate();
        }
    }

    public @Nonnull DimensionRestrictions getPrefSize() {
        return prefSize;
    }

    public void setPrefSize(@Nonnull DimensionRestrictions value) {
        prefSize = value;
        if (component != null) {
            component.revalidate();
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        component = comp;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        component = comp;
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        if (comp == component) {
            component = null;
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Component c = component;
        if (c == null || !c.isVisible()) {
            return new Dimension(0, 0);
        }
        Dimension size = c.getMinimumSize();
        Integer w = minSize.getWidth();
        Integer h = minSize.getHeight();
        if (w != null) {
            size.width = w;
        }
        if (h != null) {
            size.height = h;
        }
        size = limitMax(maxSize, size);
        JBInsets.addTo(size, parent.getInsets());
        return size;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Component c = component;
        if (c == null || !c.isVisible()) {
            return new Dimension(0, 0);
        }
        Dimension size = c.getPreferredSize();
        Integer w = prefSize.getWidth();
        Integer h = prefSize.getHeight();
        if (w != null) {
            size.width = w;
        }
        if (h != null) {
            size.height = h;
        }
        size = limitMax(maxSize, size);
        JBInsets.addTo(size, parent.getInsets());
        return size;
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        Component c = component;
        if (c == null || !c.isVisible()) {
            return new Dimension(0, 0);
        }
        Dimension size = limitMax(maxSize, c.getMaximumSize());
        JBInsets.addTo(size, target.getInsets());
        return size;
    }

    @Override
    public void layoutContainer(Container parent) {
        Component c = component;
        if (c == null || !c.isVisible()) {
            return;
        }
        Rectangle bounds = new Rectangle(0, 0, parent.getWidth(), parent.getHeight());
        JBInsets.removeFrom(bounds, parent.getInsets());
        Dimension limited = limitMax(maxSize, bounds.getSize());
        bounds.setSize(limited);
        c.setBounds(bounds);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0f;
    }

    @Override
    public void invalidateLayout(Container target) { /* TODO: cache */ }

    public static @Nonnull SizeRestrictedSingleComponentLayout constant(@Nullable Integer maxWidth, @Nullable Integer maxHeight) {
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setMaxSize(new DimensionRestrictions.ScalingConstant(maxWidth, maxHeight));
        return layout;
    }

    private static @Nonnull Dimension limitMax(@Nonnull DimensionRestrictions restrictions, @Nonnull Dimension size) {
        Integer rw = restrictions.getWidth();
        Integer rh = restrictions.getHeight();
        int width = Math.min(size.width, rw != null ? rw : Integer.MAX_VALUE);
        int height = Math.min(size.height, rh != null ? rh : Integer.MAX_VALUE);
        return new Dimension(width, height);
    }
}
