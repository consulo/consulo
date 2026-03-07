// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.paint.PaintUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;

import java.awt.*;
import java.awt.image.VolatileImage;
import java.util.function.Consumer;

/**
 * Allows painting with the help of an intermediate buffer.
 * This in turn allows post-processing of the painted data without affecting the actual target surface.
 * <p>
 * This implementation uses {@link VolatileImage} as a buffer, which can sometimes be hardware accelerated.
 */
final class VolatileImageBufferingPainter {
    private final int bufferTransparency;
    private VolatileImage buffer;

    VolatileImageBufferingPainter(int bufferTransparency) {
        this.bufferTransparency = bufferTransparency;
    }

    void paintBuffered(Graphics targetG, Dimension bufferSize, Consumer<Graphics2D> painter) {
        Graphics2D g2 = targetG.create() instanceof Graphics2D g2d ? g2d : null;
        if (g2 == null) {
            return;
        }
        try {
            VolatileImage buf = validateAndRecreateBuffer(g2, bufferSize);
            if (buf == null) {
                return;
            }
            boolean painted = paintToVolatileImage(buf, painter);
            if (!painted) {
                return;
            }
            GraphicsUtil.disableAAPainting(g2);
            PaintUtil.alignTxToInt(g2, null, true, true, PaintUtil.RoundingMode.ROUND_FLOOR_BIAS);
            g2.drawImage(buf, 0, 0, null);
        }
        finally {
            g2.dispose();
        }
    }

    private VolatileImage validateAndRecreateBuffer(Graphics2D g2, Dimension bufferSize) {
        JBUI.ScaleContext ctx = JBUI.ScaleContext.create(g2);
        int widthAligned = PaintUtil.alignIntToInt(bufferSize.width, ctx, PaintUtil.RoundingMode.CEIL, null);
        int heightAligned = PaintUtil.alignIntToInt(bufferSize.height, ctx, PaintUtil.RoundingMode.CEIL, null);
        if (widthAligned <= 0 || heightAligned <= 0) {
            return null;
        }

        GraphicsConfiguration dc = g2.getDeviceConfiguration();
        VolatileImage existing = buffer;
        if (existing != null &&
            existing.getWidth() == widthAligned &&
            existing.getHeight() == heightAligned &&
            existing.validate(dc) != VolatileImage.IMAGE_INCOMPATIBLE) {
            return existing;
        }

        VolatileImage created = createVolatileImage(dc, widthAligned, heightAligned);
        buffer = created;
        return created;
    }

    private VolatileImage createVolatileImage(GraphicsConfiguration dc, int width, int height) {
        try {
            // acceleration does not work for BITMASK so we defer to full transparency
            int transparency = bufferTransparency != Transparency.OPAQUE ? Transparency.TRANSLUCENT : bufferTransparency;
            VolatileImage image = dc.createCompatibleVolatileImage(width, height, new ImageCapabilities(true), transparency);
            if (image != null && image.validate(dc) != VolatileImage.IMAGE_INCOMPATIBLE) {
                return image;
            }
        }
        catch (AWTException ignored) {
        }
        VolatileImage image = dc.createCompatibleVolatileImage(width, height, bufferTransparency);
        if (image != null && image.validate(dc) != VolatileImage.IMAGE_INCOMPATIBLE) {
            return image;
        }
        return null;
    }

    private static boolean paintToVolatileImage(VolatileImage image, Consumer<Graphics2D> painter) {
        int iteration = 0;
        do {
            iteration++;
            Graphics2D bufferG = image.createGraphics();
            try {
                painter.accept(bufferG);
            }
            finally {
                bufferG.dispose();
            }
        }
        while (image.contentsLost() && iteration <= 3);
        return !image.contentsLost();
    }
}
