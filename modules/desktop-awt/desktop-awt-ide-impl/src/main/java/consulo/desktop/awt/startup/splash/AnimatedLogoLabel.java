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
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            g.drawImage(logoLabel.myPixels, 0, 0, c.getWidth(), c.getHeight(), c);
        }
    }

    private static final int N_LETTERS = Names.ourName.length();
    private static final int PADDING = 1, LETTER_SPACING = 1;

    @SuppressWarnings("UndesirableClassUsage")
    private final BufferedImage myPixels = new BufferedImage(
        PADDING + (Glyph.WIDTH + LETTER_SPACING) * N_LETTERS - LETTER_SPACING + PADDING,
        PADDING + Glyph.HEIGHT + PADDING,
        BufferedImage.TYPE_INT_ARGB
    );

    private int myValue;

    private Runnable myTask;

    private final int myPixelSize;
    private final boolean myAnimated;
    private final Random myRandom = new Random();

    private boolean[] myLetterStabilized = new boolean[N_LETTERS];

    private Future<?> myFuture;

    private final Object lock = new Object();

    @Nullable
    private ScheduledExecutorService myExecutorService;

    public AnimatedLogoLabel(int pixelSize, Color foreground, boolean animated, boolean unstableScaling) {
        setForeground(foreground);

        myPixelSize = pixelSize;
        myAnimated = animated;
        myExecutorService = animated ? Executors.newSingleThreadScheduledExecutor() : null;

        Character[] abc = Alphabet.ALPHABET;

        char[] str = generateCharacters();

        for (int i = 0; i < N_LETTERS; i++) {
            fillAtOffset(str[i], i);
        }

        if (myAnimated) {
            myTask = () -> {
                synchronized (lock) {
                    float per = 100f / N_LETTERS;

                    boolean end = myValue >= 93;
                    int letterPosition = (int)(myValue / per);

                    if (end) {
                        letterPosition = N_LETTERS;
                    }

                    for (int i = 0; i < letterPosition; i++) {
                        if (!myLetterStabilized[i]) {
                            myLetterStabilized[i] = true;
                            fillAtOffset(Names.ourName.charAt(i), i);
                        }
                    }

                    // last pos
                    if (end) {
                        repaintAll();

                        stop();
                    }
                    else {
                        int randomIndex = -1;
                        while (true) {
                            randomIndex = myRandom.nextInt(abc.length);
                            if (abc[randomIndex] != Names.ourName.charAt(letterPosition)) {
                                break;
                            }
                        }

                        fillAtOffset(abc[randomIndex], letterPosition);

                        repaintAll();
                    }
                }
            };
        }

        setUI(new MyComponentUI(this, unstableScaling));
    }

    public void setPixel(int x, int y, boolean foreground) {
        Color color = foreground ? getForeground() : getBackground();
        if (color != null) {
            myPixels.setRGB(x, y, color.getRGB());
        }
    }

    private char[] generateCharacters() {
        if (!myAnimated) {
            return Names.ourName.toCharArray();
        }

        if (myRandom.nextInt(100_000) < 100) {
            return Names.ourEasterNames[myRandom.nextInt(Names.ourEasterNames.length)].toCharArray();
        }

        char[] str = new char[N_LETTERS];
        for (int i = 0; i < N_LETTERS; i++) {
            str[i] = randomCharExcept(Names.ourName.charAt(i));
        }
        return str;
    }

    private char randomCharExcept(char exclude) {
        while (true) {
            char c = Alphabet.ALPHABET[myRandom.nextInt(Alphabet.ALPHABET.length)];
            if (c != exclude) {
                return c;
            }
        }
    }

    private void repaintAll() {
        update(getGraphics());
    }

    public void start() {
        if (myExecutorService != null && myAnimated) {
            myFuture = myExecutorService.scheduleWithFixedDelay(myTask, 50, 20, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (myFuture != null) {
            myFuture.cancel(false);
        }
    }

    private void fillAtOffset(char c, int pos) {
        int dx = PADDING + (Glyph.WIDTH + LETTER_SPACING) * pos;

        Alphabet.VALID_CHARACTERS.get(c).draw(dx, 1, this);
    }

    public void setValue(int value) {
        synchronized (lock) {
            myValue = value;
        }
    }

    public void dispose() {
        if (myExecutorService != null) {
            myExecutorService.shutdown();
        }
    }

    @Override
    public void updateUI() {
    }
}
