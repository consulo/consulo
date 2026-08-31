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
package consulo.desktop.qt.wm.impl;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.layout.DesktopQtLayoutComponent;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.layout.LayoutConstraint;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopToolWindowStripeImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> {
    public enum Position {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    private final Position myPosition;

    public DesktopToolWindowStripeImpl(Position position) {
        myPosition = position;
    }

    public void addButton(ToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
        addImpl((QtComponentDelegate<?>) button, null);
    }

    @Override
    protected @Nullable QLayout createLayout() {
        QBoxLayout layout = switch (myPosition) {
            case LEFT, RIGHT -> new QVBoxLayout();
            case TOP, BOTTOM -> new QHBoxLayout();
        };

        layout.setSpacing(0);
        return layout;
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        if (myPosition == Position.LEFT || myPosition == Position.RIGHT) {
            component.setFixedWidth(22);
        }
        else if (myPosition == Position.TOP || myPosition == Position.BOTTOM) {
            component.setFixedHeight(22);
        }
    }
}
