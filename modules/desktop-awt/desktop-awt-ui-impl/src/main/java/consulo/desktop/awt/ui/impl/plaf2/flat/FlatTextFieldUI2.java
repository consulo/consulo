/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatTextFieldUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import java.awt.*;

import org.jspecify.annotations.Nullable;

/**
 * {@code FlatTextFieldLayout} places leading/trailing components at the focus width and ignores the border insets,
 * while the text rect of the same field honors them - so a field with a border of its own draws its icon flush and
 * its text inset.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
public class FlatTextFieldUI2 extends FlatTextFieldUI {
    public static ComponentUI createUI(JComponent c) {
        return new FlatTextFieldUI2();
    }

    @Override
    protected Rectangle getIconsRect() {
        Rectangle rectangle = super.getIconsRect();
        if (rectangle == null) {
            return rectangle;
        }

        boolean leftToRight = getComponent().getComponentOrientation().isLeftToRight();

        int gap = UIScale.scale(iconTextGap);

        if (isAnyVisible(leftToRight ? getLeadingComponents() : getTrailingComponents())) {
            rectangle.x += gap;
            rectangle.width -= gap;
        }

        if (isAnyVisible(leftToRight ? getTrailingComponents() : getLeadingComponents())) {
            rectangle.width -= gap;
        }

        rectangle.width = Math.max(rectangle.width, 0);
        return rectangle;
    }

    private static boolean isAnyVisible(JComponent[] components) {
        for (JComponent component : components) {
            if (component != null && component.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void installLayout() {
        JTextComponent component = getComponent();

        LayoutManager oldLayout = component.getLayout();
        if (!(oldLayout instanceof InsetsAwareLayout)) {
            component.setLayout(new InsetsAwareLayout(oldLayout));
        }
    }

    private class InsetsAwareLayout implements LayoutManager2, UIResource {
        private final @Nullable LayoutManager myDelegate;

        private InsetsAwareLayout(@Nullable LayoutManager delegate) {
            myDelegate = delegate;
        }

        @Override
        public void layoutContainer(Container parent) {
            if (myDelegate != null) {
                myDelegate.layoutContainer(parent);
            }

            JTextComponent component = getComponent();

            Insets insets = parent.getInsets();
            int focusWidth = FlatUIUtils.getBorderFocusAndLineWidth(component);

            int top = Math.max(insets.top, focusWidth);
            int height = parent.getHeight() - top - Math.max(insets.bottom, focusWidth);

            boolean leftToRight = component.getComponentOrientation().isLeftToRight();

            JComponent[] leftComponents = leftToRight ? getLeadingComponents() : getTrailingComponents();
            JComponent[] rightComponents = leftToRight ? getTrailingComponents() : getLeadingComponents();

            int x = Math.max(leftToRight ? insets.left : insets.right, focusWidth);
            for (JComponent leftComponent : leftComponents) {
                if (leftComponent != null && leftComponent.isVisible()) {
                    int width = leftComponent.getPreferredSize().width;
                    leftComponent.setBounds(x, top, width, height);
                    x += width;
                }
            }

            x = parent.getWidth() - Math.max(leftToRight ? insets.right : insets.left, focusWidth);
            for (JComponent rightComponent : rightComponents) {
                if (rightComponent != null && rightComponent.isVisible()) {
                    int width = rightComponent.getPreferredSize().width;
                    x -= width;
                    rightComponent.setBounds(x, top, width, height);
                }
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if (myDelegate != null) {
                myDelegate.addLayoutComponent(name, comp);
            }
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            if (myDelegate != null) {
                myDelegate.removeLayoutComponent(comp);
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return myDelegate != null ? myDelegate.preferredLayoutSize(parent) : new Dimension();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return myDelegate != null ? myDelegate.minimumLayoutSize(parent) : new Dimension();
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if (myDelegate instanceof LayoutManager2 layoutManager2) {
                layoutManager2.addLayoutComponent(comp, constraints);
            }
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return myDelegate instanceof LayoutManager2 layoutManager2
                ? layoutManager2.maximumLayoutSize(target)
                : new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return myDelegate instanceof LayoutManager2 layoutManager2 ? layoutManager2.getLayoutAlignmentX(target) : 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return myDelegate instanceof LayoutManager2 layoutManager2 ? layoutManager2.getLayoutAlignmentY(target) : 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
            if (myDelegate instanceof LayoutManager2 layoutManager2) {
                layoutManager2.invalidateLayout(target);
            }
        }
    }
}
