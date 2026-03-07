// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel which paints its background independently from {@link #isOpaque()} property
 * <p>
 * WARN: do not set background with alpha when {@link #isOpaque()} is true
 */
public class JPanelWithBackground extends JPanel {
    public JPanelWithBackground() {
        super();
    }

    public JPanelWithBackground(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public JPanelWithBackground(LayoutManager layout) {
        super(layout);
    }

    public JPanelWithBackground(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // opaque background is painted in javax.swing.plaf.ComponentUI.update
        if (!isOpaque() && isBackgroundSet()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }
}
