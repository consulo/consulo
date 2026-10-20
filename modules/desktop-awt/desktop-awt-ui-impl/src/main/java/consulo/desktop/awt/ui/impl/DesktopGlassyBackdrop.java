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

import consulo.ui.ex.awt.JBUI;
import consulo.ui.impl.ToastPresentation;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * The blurred view of the frame behind a glassy surface floating in the layered pane - a toast or a light popup.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class DesktopGlassyBackdrop {
    private DesktopGlassyBackdrop() {
    }

    /**
     * The fill of a glassy surface, which keeps {@link ToastPresentation#GLASSY_OPACITY} of {@code background}.
     */
    public static Color fill(Color background) {
        return new Color(
            background.getRed(),
            background.getGreen(),
            background.getBlue(),
            Math.round(background.getAlpha() * ToastPresentation.GLASSY_OPACITY)
        );
    }

    /**
     * Paints the content pane of the frame under {@code shape} blurred, clipped to {@code shape}. Both {@code g} and
     * {@code shape} are in the space of {@code surface}.
     * <p/>
     * What lies under the surface is the content pane - the surface floats in the layered pane, outside of it.
     * {@code print} and not {@code paint}: inside a paint cycle {@code paint} goes through the back buffer of the
     * repaint manager instead of the given graphics.
     */
    public static void paint(JComponent surface, Graphics2D g, Shape shape) {
        JRootPane rootPane = SwingUtilities.getRootPane(surface);
        if (rootPane == null || !(rootPane.getContentPane() instanceof JComponent content) || SwingUtilities.isDescendingFrom(surface, content)) {
            return;
        }

        Rectangle shapeBounds = shape.getBounds();
        Rectangle bounds = SwingUtilities.convertRectangle(surface, shapeBounds, content);
        int sigma = JBUI.scale(ToastPresentation.GLASSY_BLUR);
        int margin = 2 * sigma;
        Rectangle area = new Rectangle(bounds.x - margin, bounds.y - margin, bounds.width + 2 * margin, bounds.height + 2 * margin)
            .intersection(new Rectangle(0, 0, content.getWidth(), content.getHeight()));
        if (area.isEmpty()) {
            return;
        }

        double scale = g.getTransform().getScaleX();
        BufferedImage snapshot = new BufferedImage(
            (int) Math.ceil(area.width * scale),
            (int) Math.ceil(area.height * scale),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D snapshotGraphics = snapshot.createGraphics();
        try {
            snapshotGraphics.scale(scale, scale);
            snapshotGraphics.translate(-area.x, -area.y);
            snapshotGraphics.setClip(area.x, area.y, area.width, area.height);
            content.print(snapshotGraphics);
        }
        finally {
            snapshotGraphics.dispose();
        }

        BufferedImage blurred = blur(snapshot, 2 * Math.max(1, (int) Math.round(sigma * scale)));

        Graphics2D backdrop = (Graphics2D) g.create();
        try {
            backdrop.clip(shape);
            backdrop.drawImage(
                blurred,
                shapeBounds.x + area.x - bounds.x,
                shapeBounds.y + area.y - bounds.y,
                area.width,
                area.height,
                null
            );
        }
        finally {
            backdrop.dispose();
        }
    }

    public static BufferedImage blur(BufferedImage image, int radius) {
        float[] kernel = gaussian(radius);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(kernel.length, 1, kernel), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, kernel.length, kernel), ConvolveOp.EDGE_NO_OP, null);
        return vertical.filter(horizontal.filter(image, null), null);
    }

    private static float[] gaussian(int radius) {
        double sigma = radius / 2.0;
        float[] kernel = new float[radius * 2 + 1];
        float sum = 0;
        for (int i = -radius; i <= radius; i++) {
            float value = (float) Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = value;
            sum += value;
        }
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
}
