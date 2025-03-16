/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopDockLayoutImpl extends DesktopLayoutBase<JPanel, StaticPosition> implements DockLayout {
    public DesktopDockLayoutImpl(int gapInPixels) {
        initDefaultPanel(new BorderLayout(gapInPixels, gapInPixels));
    }

    @Override
    protected Object convertConstraints(StaticPosition constraint) {
        switch (constraint) {
            case TOP:
                return BorderLayout.NORTH;
            case BOTTOM:
                return BorderLayout.SOUTH;
            case LEFT:
                return BorderLayout.WEST;
            case RIGHT:
                return BorderLayout.EAST;
            case CENTER:
                return BorderLayout.CENTER;
            default:
                throw new IllegalArgumentException(constraint.name());
        }
    }

    @Nonnull
    @Override
    public Layout<StaticPosition> add(@Nonnull Component component, @Nonnull StaticPosition constraint) {
        set(component, convertConstraints(constraint));
        return this;
    }

    protected void set(Component component, Object constraints) {
        JPanel panel = toAWTComponent();

        BorderLayout layout = (BorderLayout) panel.getLayout();
        java.awt.Component old = layout.getLayoutComponent(constraints);
        if (old != null) {
            panel.remove(old);
        }

        panel.add(TargetAWT.to(component), constraints);
        panel.validate();
        panel.repaint();
    }
}
