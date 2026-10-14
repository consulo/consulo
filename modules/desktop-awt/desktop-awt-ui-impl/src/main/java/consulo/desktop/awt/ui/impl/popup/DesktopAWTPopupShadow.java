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
package consulo.desktop.awt.ui.impl.popup;

import consulo.ui.ex.awt.JBUIScale;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

final class DesktopAWTPopupShadow {
    private static final int[] OFFSETS = {3, 1};
    private static final int[] BLURS = {10, 2};
    private static final float[] ALPHAS = {0.30f, 0.20f};

    private DesktopAWTPopupShadow() {
    }

    static Insets insets() {
        int top = 0;
        int side = 0;
        int bottom = 0;
        for (int i = 0; i < OFFSETS.length; i++) {
            int blur = JBUIScale.scale(BLURS[i]);
            int offset = JBUIScale.scale(OFFSETS[i]);
            top = Math.max(top, blur - offset);
            bottom = Math.max(bottom, blur + offset);
            side = Math.max(side, blur);
        }
        return new Insets(top, side, bottom, side);
    }

    static BufferedImage render(Shape outline, int width, int height) {
        BufferedImage result = new BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = result.createGraphics();
        try {
            for (int i = 0; i < OFFSETS.length; i++) {
                int blur = JBUIScale.scale(BLURS[i]);
                int offset = JBUIScale.scale(OFFSETS[i]);
                int pad = blur * 2;
                g.drawImage(blurred(outline, width, height, blur, pad, ALPHAS[i]), -pad, offset - pad, null);
            }
        }
        finally {
            g.dispose();
        }
        return result;
    }

    private static BufferedImage blurred(Shape outline, int width, int height, int blur, int pad, float alpha) {
        BufferedImage mask = new BufferedImage(width + pad * 2, height + pad * 2, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = mask.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, 0, 0, Math.round(alpha * 255)));
            g.fill(AffineTransform.getTranslateInstance(pad, pad).createTransformedShape(outline));
        }
        finally {
            g.dispose();
        }
        if (blur <= 0) {
            return mask;
        }
        float[] kernel = gaussian(blur);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(kernel.length, 1, kernel), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, kernel.length, kernel), ConvolveOp.EDGE_NO_OP, null);
        return vertical.filter(horizontal.filter(mask, null), null);
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
