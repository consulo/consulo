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

import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowStripeImpl implements PseudoComponent {
    private final List<ToolWindowStripeButton> myButtons = new ArrayList<>();

    private final Layout<?> myLayout;

    @RequiredUIAccess
    public DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition position) {
        myLayout = switch (position) {
            case LEFT, RIGHT -> VerticalLayout.create(0);
            case TOP, BOTTOM -> HorizontalLayout.create(0);
        };
    }

    @Override
    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    public void addButton(ToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
        myLayout.removeAll();

        myButtons.add(button);

        myButtons.sort(comparator);

        for (ToolWindowStripeButton stripeButton : myButtons) {
            addComponent(stripeButton.getComponent());
        }
    }

    @RequiredUIAccess
    private void addComponent(Component component) {
        if (myLayout instanceof VerticalLayout verticalLayout) {
            verticalLayout.add(component);
        }
        else {
            ((HorizontalLayout) myLayout).add(component);
        }
    }
}
