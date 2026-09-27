// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.image;

import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class DesktopSpinnerFrames {
    private record FrameKey(int rgb, int step) {
    }

    private static final Logger LOG = Logger.getInstance(DesktopSpinnerFrames.class);

    private static final String[] PATHS = {
        " x=\"7\"       y=\"1\"       width=\"2\" height=\"4\" rx=\"1\" ",
        " x=\"2.34961\" y=\"3.76416\" width=\"2\" height=\"4\" rx=\"1\" transform=\"rotate(-45 2.34961 3.76416)\" ",
        " x=\"1\"       y=\"7\"       width=\"4\" height=\"2\" rx=\"1\" ",
        " x=\"5.17871\" y=\"9.40991\" width=\"2\" height=\"4\" rx=\"1\" transform=\"rotate(45 5.17871 9.40991)\" ",
        " x=\"7\"       y=\"11\"      width=\"2\" height=\"4\" rx=\"1\" ",
        " x=\"9.41016\" y=\"10.8242\" width=\"2\" height=\"4\" rx=\"1\" transform=\"rotate(-45 9.41016 10.8242)\" ",
        " x=\"11\"      y=\"7\"       width=\"4\" height=\"2\" rx=\"1\" ",
        " x=\"12.2383\" y=\"2.3501\"  width=\"2\" height=\"4\" rx=\"1\" transform=\"rotate(45 12.2383 2.3501)\" "
    };

    private static final int SIZE = 16;

    private static final double[] OPACITIES = {1.0, 0.93, 0.78, 0.69, 0.62, 0.48, 0.38, 0.3};

    private static final int DEFAULT_DELAY = 125;

    private static final Color PROGRESS_ICON_COLOR = JBColor.namedColor("ProgressIcon.color", new JBColor(0xA8ADBD, 0x6F737A));

    private static final ConcurrentMap<FrameKey, Image> FRAMES = new ConcurrentHashMap<>();

    private DesktopSpinnerFrames() {
    }

    static int getStepAt(long nanoTime) {
        long step = Math.floorDiv(Math.floorDiv(nanoTime, 1_000_000L), (long) getSafeStepDelay());
        return (int) Math.floorMod(step, (long) getStepCount());
    }

    static int getMillisToNextStep(long nanoTime) {
        long delay = getSafeStepDelay();
        return (int) (delay - Math.floorMod(Math.floorDiv(nanoTime, 1_000_000L), delay));
    }

    static Color getProgressIconColor() {
        return PROGRESS_ICON_COLOR;
    }

    static Image getFrame(int step, Color color) {
        int safeStep = Math.floorMod(step, getStepCount());
        return FRAMES.computeIfAbsent(new FrameKey(color.getRGB(), safeStep), key -> loadFrame(key.step(), color));
    }

    private static int getStepCount() {
        return PATHS.length;
    }

    private static int getSafeStepDelay() {
        return Math.max(1, JBUI.getInt("ProgressIcon.delay", DEFAULT_DELAY));
    }

    private static Image loadFrame(int step, Color color) {
        String svg = generateSvg(step, color);
        try {
            return Image.fromStream(Image.ImageType.SVG, new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        }
        catch (IOException e) {
            LOG.error("Failed to load the spinning progress frame " + step, e);
            return ImageEffects.resize(PlatformIconGroup.processStep_passive(), SIZE, SIZE);
        }
    }

    private static String generateSvg(int step, Color color) {
        StringBuilder builder = new StringBuilder();
        builder.append("<svg width=\"").append(SIZE).append("\" height=\"").append(SIZE)
            .append("\" viewBox=\"0 0 ").append(SIZE).append(' ').append(SIZE)
            .append("\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        String stroke = ColorUtil.toHex(color);
        int alpha = color.getAlpha();
        for (int n = 0; n < PATHS.length; n++) {
            double opacity = OPACITIES[(n + step) % OPACITIES.length];
            if (alpha != 255) {
                opacity = opacity * alpha / 255.0;
            }
            builder.append("  <rect fill=\"#").append(stroke).append("\" opacity=\"").append(opacity).append("\" ")
                .append(PATHS[n]).append(" />\n");
        }
        builder.append("</svg>");
        return builder.toString();
    }
}
