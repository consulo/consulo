// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import jakarta.annotation.Nonnull;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;

final class UIResourceBorder implements Border, UIResource {
    private final Border delegate;

    UIResourceBorder(@Nonnull Border delegate) {
        this.delegate = delegate;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        delegate.paintBorder(c, g, x, y, width, height);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return delegate.getBorderInsets(c);
    }

    @Override
    public boolean isBorderOpaque() {
        return delegate.isBorderOpaque();
    }
}
