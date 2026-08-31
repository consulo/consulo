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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * Shared part of bridging a {@link ComponentItemRender} onto a swing cell renderer - lists and tables differ
 * only in where the row colors come from.
 * <p/>
 * A swing renderer is a rubber stamp, re-invoked on every paint, so a {@link ReusableComponentItemRender}
 * builds its component once and only rebinds per row. That only holds while the bridge itself outlives a
 * single paint, so a host which asks for a renderer per row has to keep one bridge per column, not per call.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public abstract class DesktopComponentItemRenderBase<E> {
    private final ComponentItemRender<E> myRender;

    @SuppressWarnings("rawtypes")
    private consulo.ui.Component myReusedComponent;

    private boolean myMouseEventsAllowed;

    protected DesktopComponentItemRenderBase(ComponentItemRender<E> render) {
        myRender = render;
    }

    public boolean isMouseEventsAllowed() {
        return myMouseEventsAllowed;
    }

    /**
     * Renders the item and paints the result onto the given row colors. The mouse event opt-in is read back
     * right after the render, since that is the only point at which the render could have asked for it.
     */
    protected final Component render(RenderItem<E> item, Color background, Color foreground) {
        Component component;
        try {
            component = doRender(item);
        }
        finally {
            myMouseEventsAllowed |= item.isMouseEventsAllowed();
        }

        if (component != null) {
            applyRowColors(component, background, foreground);
        }
        return component;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component doRender(RenderItem<E> item) {
        if (myRender instanceof ReusableComponentItemRender reusable) {
            if (myReusedComponent == null) {
                myReusedComponent = (consulo.ui.Component) reusable.createComponent();
            }
            reusable.bind(myReusedComponent, item);
            return TargetAWT.to(myReusedComponent);
        }

        return TargetAWT.to(myRender.render(item));
    }

    /**
     * A row is painted by the host, not held by it, so the component handed back is what draws the row - it is
     * opaque and carries the host colors, and everything inside it stays transparent so that background shows
     * through. Without this a widget with a background of its own - a check box above all - paints its own
     * look and feel fill and the cell stops matching the rest of the row.
     */
    public static void applyRowColors(Component component, Color background, Color foreground) {
        applyRowColors(component, background, foreground, true);
    }

    private static void applyRowColors(Component component, Color background, Color foreground, boolean root) {
        if (root) {
            component.setBackground(background);

            if (component instanceof JComponent jComponent) {
                jComponent.setOpaque(true);
            }
        }
        else if (component instanceof JPanel panel && panel instanceof FromSwingComponentWrapper) {
            panel.setOpaque(false);
        }

        Color currentForeground = component.getForeground();
        if (currentForeground == null || currentForeground instanceof UIResource) {
            component.setForeground(foreground);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRowColors(child, background, foreground, false);
            }
        }
    }
}
