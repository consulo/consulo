// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;

/**
 * Similar in principle to {@link com.intellij.ui.TextIcon}, but also always limits the size to the specified size.
 * Uses label font to draw the emoji by default, but will perform font fallback lookup if necessary.
 */
final class UnicodeEmojiIcon implements Icon {
    private final String text;
    private final int size;
    private final ScaleContextCache<PaintData> paintDataCache;

    UnicodeEmojiIcon(@Nonnull String text, int size) {
        this.text = text.endsWith(CodeReviewReactionsUIUtil.VARIATION_SELECTOR)
            ? text
            : text + CodeReviewReactionsUIUtil.VARIATION_SELECTOR;
        this.size = size;
        this.paintDataCache = new ScaleContextCache<>(ctx -> {
            // we don't use font scale here, bc it's not a text, but icon
            Font labelFont = UIUtil.getLabelFont();
            Font font = CodeReviewReactionsUIUtil.EMOJI_FONT != null
                ? CodeReviewReactionsUIUtil.EMOJI_FONT.deriveFont(labelFont.getSize2D())
                : labelFont;
            return new PaintData(JBUI.scale(size), font);
        });
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (c == null) {
            return;
        }
        PaintData paintData = getPaintData();
        Graphics2D g2d = (Graphics2D) g.create(x, y, paintData.size, paintData.size);
        try {
            GraphicsUtil.setupAntialiasing(g2d);

            g2d.setFont(paintData.font);
            g2d.setColor(UIUtil.getLabelForeground());

            var frc = g2d.getFontRenderContext();
            TextLayout layout = new TextLayout(text, g2d.getFont(), frc);

            float baselineX = Math.max(0f, (paintData.size - layout.getVisibleAdvance())) / 2f;

            float height = layout.getAscent() + layout.getDescent();
            float baselineY = layout.getAscent() + Math.max(0f, (paintData.size - height)) / 2f;

            layout.draw(g2d, baselineX, baselineY);
        }
        finally {
            g2d.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return getPaintData().size;
    }

    @Override
    public int getIconHeight() {
        return getPaintData().size;
    }

    private @Nonnull PaintData getPaintData() {
        PaintData data = paintDataCache.getOrProvide(JBUI.ScaleContext.create());
        return data != null ? data : new PaintData(size, UIUtil.getLabelFont());
    }

    private record PaintData(int size, @Nonnull Font font) {
    }
}
