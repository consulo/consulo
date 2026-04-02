// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.timeline.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.function.IntSupplier;

/**
 * Lays out the field with an icon on the left.
 * Icon is aligned to the top of its column except when min height of the field is less than that of an icon,
 * in this case avatar is centered along that min height.
 * Same thing the other way around.
 */
final class CommentFieldWithIconLayout implements LayoutManager {
    static final String ICON = "ICON";
    static final String ITEM = "ITEM";

    private final int gap;
    private final IntSupplier minimalItemHeightCalculator;

    private Component iconComponent;
    private Component itemComponent;

    CommentFieldWithIconLayout(int gap, @Nonnull IntSupplier minimalItemHeightCalculator) {
        this.gap = gap;
        this.minimalItemHeightCalculator = minimalItemHeightCalculator;
    }

    @Override
    public void addLayoutComponent(@Nonnull String name, @Nullable Component comp) {
        switch (name) {
            case ICON -> iconComponent = comp;
            case ITEM -> itemComponent = comp;
            default -> throw new IllegalStateException("Incorrect name " + name);
        }
    }

    @Override
    public void removeLayoutComponent(@Nonnull Component comp) {
        if (iconComponent == comp) {
            iconComponent = null;
        }
        if (itemComponent == comp) {
            itemComponent = null;
        }
    }

    @Override
    public @Nonnull Dimension preferredLayoutSize(@Nonnull Container parent) {
        return getSize(parent, Component::getPreferredSize);
    }

    @Override
    public @Nonnull Dimension minimumLayoutSize(@Nonnull Container parent) {
        return getSize(parent, Component::getMinimumSize);
    }

    private @Nonnull Dimension getSize(
        @Nonnull Container parent,
        @Nonnull java.util.function.Function<Component, Dimension> sizeGetter
    ) {
        Dimension iconSize = iconComponent != null && iconComponent.isVisible()
            ? sizeGetter.apply(iconComponent) : new Dimension(0, 0);
        Dimension itemSize = itemComponent != null && itemComponent.isVisible()
            ? sizeGetter.apply(itemComponent) : new Dimension(0, 0);

        int scaledGap = JBUIScale.scale(gap);
        Insets i = parent.getInsets();

        return new Dimension(
            i.left + iconSize.width + scaledGap + itemSize.width + i.right,
            i.top + Math.max(iconSize.height, itemSize.height) + i.bottom
        );
    }

    @Override
    public void layoutContainer(@Nonnull Container parent) {
        Rectangle bounds = new Rectangle(new Point(0, 0), parent.getSize());
        JBInsets.removeFrom(bounds, parent.getInsets());
        int x = bounds.x;
        int y = bounds.y;
        int contentWidth = bounds.width;
        int contentHeight = bounds.height;

        int iconHeight = iconComponent != null && iconComponent.isVisible()
            ? iconComponent.getPreferredSize().height : 0;
        int itemMinHeight = minimalItemHeightCalculator.getAsInt();

        if (iconComponent != null && iconComponent.isVisible()) {
            Dimension prefSize = iconComponent.getPreferredSize();
            int width = Math.min(contentWidth, prefSize.width);
            iconComponent.setBounds(x, y + Math.max(0, (itemMinHeight - iconHeight) / 2),
                width, Math.min(contentHeight, prefSize.height)
            );
            x += prefSize.width;
            x += JBUIScale.scale(gap);

            contentWidth -= width;
            contentWidth -= JBUIScale.scale(gap);
        }

        if (itemComponent != null && itemComponent.isVisible()) {
            Dimension maxSize = itemComponent.getMaximumSize();
            Dimension minSize = itemComponent.getMinimumSize();

            int width;
            if (contentWidth >= maxSize.width) {
                width = maxSize.width;
            }
            else if (contentWidth >= minSize.width) {
                width = contentWidth;
            }
            else {
                width = minSize.width;
            }

            int height;
            if (contentHeight >= maxSize.height) {
                height = maxSize.height;
            }
            else if (contentHeight >= minSize.height) {
                height = contentHeight;
            }
            else {
                height = minSize.height;
            }

            itemComponent.setBounds(x, y + Math.max(0, (iconHeight - itemMinHeight) / 2), width, height);
        }
    }
}
