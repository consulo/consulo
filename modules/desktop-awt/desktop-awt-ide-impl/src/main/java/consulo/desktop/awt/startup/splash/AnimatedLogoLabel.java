/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.startup.splash;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.internal.Alphabet;
import consulo.ui.ex.internal.Glyph;
import consulo.ui.ex.internal.Names;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * @author VISTALL
 * @since 2016-12-11
 */
public class AnimatedLogoLabel extends JComponent {
    private static class MyComponentUI extends ComponentUI {
        private Dimension myFixedSizeScaled;

        private MyComponentUI(AnimatedLogoLabel animatedLogoLabel, boolean unstableScaling) {
            if (!unstableScaling) {
                myFixedSizeScaled = getPreferredSize(animatedLogoLabel);
            }
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            if (myFixedSizeScaled != null) {
                return myFixedSizeScaled;
            }

            AnimatedLogoLabel logoLabel = (AnimatedLogoLabel)c;
            return new Dimension(
                JBUI.scale(logoLabel.myPixels.getWidth() * logoLabel.myPixelSize),
                JBUI.scale(logoLabel.myPixels.getHeight() * logoLabel.myPixelSize)
            );
        }

        @Override
        public Dimension getMinimumSize(JComponent c) {
            return getPreferredSize(c);
        }

        @Override
        public Dimension getMaximumSize(JComponent c) {
            return getPreferredSize(c);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AnimatedLogoLabel logoLabel = (AnimatedLogoLabel)c;
            synchronized (logoLabel.myLock) {
                g.drawImage(logoLabel.myPixels, 0, 0, c.getWidth(), c.getHeight(), c);
            }
        }
    }

    private static final int N_LETTERS = Names.ourName.length();
    private static final int PADDING = 1, LETTER_SPACING = 1;
    private static final int COLUMNS = PADDING + (Glyph.WIDTH + LETTER_SPACING) * N_LETTERS - LETTER_SPACING + PADDING;
    private static final int DIMMED_ALPHA = 56;

    @SuppressWarnings("UndesirableClassUsage")
    private final BufferedImage myPixels = new BufferedImage(COLUMNS, PADDING + Glyph.HEIGHT + PADDING, BufferedImage.TYPE_INT_ARGB);

    private final Object myLock = new Object();

    private final int myPixelSize;
    private final boolean myAnimated;
    private final String myName;

    private int myValue;

    public AnimatedLogoLabel(int pixelSize, Color foreground, boolean animated, boolean unstableScaling) {
        setForeground(foreground);
        setOpaque(false);

        myPixelSize = pixelSize;
        myAnimated = animated;
        myName = animated ? pickName() : Names.ourName;

        fill();

        setUI(new MyComponentUI(this, unstableScaling));
    }

    private static String pickName() {
        Random random = new Random();
        if (random.nextInt(100_000) < 100) {
            return Names.ourEasterNames[random.nextInt(Names.ourEasterNames.length)];
        }
        return Names.ourName;
    }

    public void setValue(int value) {
        synchronized (myLock) {
            if (myValue == value) {
                return;
            }
            myValue = value;
            fill();
        }
        repaint();
    }

    private void fill() {
        Color foreground = getForeground();
        int filled = foreground.getRGB();
        int dimmed = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), DIMMED_ALPHA).getRGB();
        int leading = luminance(foreground) < 0.5f ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
        float fill = myValue / 100f * COLUMNS;

        synchronized (myLock) {
            for (int x = 0; x < myPixels.getWidth(); x++) {
                for (int y = 0; y < myPixels.getHeight(); y++) {
                    myPixels.setRGB(x, y, 0);
                }
            }

            for (int i = 0; i < N_LETTERS; i++) {
                int dx = PADDING + (Glyph.WIDTH + LETTER_SPACING) * i;
                Alphabet.VALID_CHARACTERS.get(myName.charAt(i)).draw(dx, PADDING, (x, y, lit) -> {
                    if (!lit) {
                        return;
                    }
                    int color;
                    if (!myAnimated || x < fill - 1) {
                        color = filled;
                    }
                    else if (x < fill) {
                        color = leading;
                    }
                    else {
                        color = dimmed;
                    }
                    myPixels.setRGB(x, y, color);
                });
            }
        }
    }

    private static float luminance(Color color) {
        return (0.2126f * color.getRed() + 0.7152f * color.getGreen() + 0.0722f * color.getBlue()) / 255f;
    }

    @Override
    public void updateUI() {
    }
}
