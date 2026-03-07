// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public final class JComponentOverlay {
    private JComponentOverlay() {
    }

    public static @Nonnull JLayeredPane createCentered(@Nonnull JComponent component, @Nonnull JComponent centeredOverlay) {
        JLayeredPane pane = new JLayeredPane() {
            @Override
            public Dimension getPreferredSize() {
                return component.getPreferredSize();
            }

            @Override
            public Dimension getMinimumSize() {
                return component.getMinimumSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return component.getMaximumSize();
            }

            @Override
            public boolean isVisible() {
                return component.isVisible();
            }

            @Override
            public void doLayout() {
                super.doLayout();
                component.setBounds(0, 0, getWidth(), getHeight());
                centeredOverlay.setBounds(SingleComponentCenteringLayout.getBoundsForCentered(component, centeredOverlay));
            }
        };
        pane.setFocusable(false);
        pane.add(component, JLayeredPane.DEFAULT_LAYER, -1);
        pane.add(centeredOverlay, JLayeredPane.PALETTE_LAYER, -1);
        return pane;
    }
}
