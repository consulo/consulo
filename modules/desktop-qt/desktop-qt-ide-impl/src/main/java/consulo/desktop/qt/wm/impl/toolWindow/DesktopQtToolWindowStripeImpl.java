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
package consulo.desktop.qt.wm.impl.toolWindow;

import consulo.project.ui.impl.internal.wm.ToolWindowStripePlacement;
import consulo.ui.BorderBuilder;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.impl.style.IslandsState;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowStripeImpl implements PseudoComponent {
    private final List<ToolWindowStripeButton> myButtons = new ArrayList<>();

    private final DesktopQtToolWindowStripePosition myPosition;

    private final Component myComponent;
    private final Layout<?> myStartLayout;
    private final Layout<?> myEndLayout;

    private boolean myBordered;

    @RequiredUIAccess
    public DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition position) {
        myPosition = position;
        myStartLayout = createGroupLayout(position);

        switch (position) {
            case LEFT, RIGHT -> {
                myEndLayout = createGroupLayout(position);
                myComponent = DockLayout.create(Space.NONE).top(myStartLayout).bottom(myEndLayout);
            }
            case TOP -> {
                myEndLayout = createGroupLayout(position);
                myComponent = DockLayout.create(Space.NONE).left(myStartLayout).right(myEndLayout);
            }
            default -> {
                myEndLayout = myStartLayout;
                myComponent = myStartLayout;
            }
        }
    }

    @RequiredUIAccess
    private static Layout<?> createGroupLayout(DesktopQtToolWindowStripePosition position) {
        return switch (position) {
            case LEFT, RIGHT -> VerticalLayout.create(Space.NONE);
            case TOP, BOTTOM -> HorizontalLayout.create(Space.NONE);
        };
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    public boolean contains(ToolWindowStripeButton button) {
        return myButtons.contains(button);
    }

    @RequiredUIAccess
    public void addButton(ToolWindowStripeButton button, ToolWindowSettings settings) {
        myButtons.add(button);

        relayout(settings);
    }

    @RequiredUIAccess
    public void relayout(ToolWindowSettings settings) {
        myStartLayout.removeAll();
        myEndLayout.removeAll();

        myButtons.sort(ToolWindowStripePlacement.comparator(settings));

        for (ToolWindowStripeButton stripeButton : myButtons) {
            boolean atEnd = ToolWindowStripePlacement.isAtStripeEnd(settings, stripeButton.getWindowInfo());
            addComponent(atEnd ? myEndLayout : myStartLayout, stripeButton.getComponent());
        }

        updateBorder();
    }

    @RequiredUIAccess
    public void removeButton(ToolWindowStripeButton button) {
        if (!myButtons.remove(button)) {
            return;
        }

        Component component = button.getComponent();
        myStartLayout.remove(component);
        myEndLayout.remove(component);

        updateBorder();
    }

    @RequiredUIAccess
    private void updateBorder() {
        boolean bordered = !myButtons.isEmpty() && !IslandsState.isEnabled();
        if (bordered == myBordered) {
            return;
        }

        myBordered = bordered;

        BorderBuilder builder = myComponent.borderBuilder();
        switch (myPosition) {
            case LEFT -> (bordered ? builder.rightSet() : builder.rightReset()).apply();
            case RIGHT -> (bordered ? builder.leftSet() : builder.leftReset()).apply();
            case TOP -> (bordered ? builder.bottomSet() : builder.bottomReset()).apply();
            case BOTTOM -> {
            }
        }
    }

    @RequiredUIAccess
    private static void addComponent(Layout<?> layout, Component component) {
        if (layout instanceof VerticalLayout verticalLayout) {
            verticalLayout.add(component);
        }
        else {
            ((HorizontalLayout) layout).add(component);
        }
    }
}
