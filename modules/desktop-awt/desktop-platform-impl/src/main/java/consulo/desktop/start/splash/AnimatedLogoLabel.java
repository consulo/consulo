/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.start.splash;

import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 11-Dec-16.
 */
public class AnimatedLogoLabel extends JComponent {
  private static class MyComponentUI extends ComponentUI {
    private final int[][] myEmptyData = new int[43][7];

    private Dimension myFixedSizeScaled;
    private Integer myLetterHeightScaled;

    private MyComponentUI(AnimatedLogoLabel animatedLogoLabel, boolean unstableScaling) {
      for (int i = 0; i < ourOffsets.length; i++) {
        fillAtOffset(Alphabet.validCharacters, ' ', i, myEmptyData);
      }

      if (!unstableScaling) {
        myFixedSizeScaled = getPreferredSize(animatedLogoLabel);
        myLetterHeightScaled = JBUI.scale(animatedLogoLabel.myLetterHeight);
      }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
      if (myFixedSizeScaled != null) {
        return myFixedSizeScaled;
      }

      AnimatedLogoLabel logoLabel = (AnimatedLogoLabel)c;
      return new Dimension(JBUI.scale(logoLabel.myData.length * logoLabel.myLetterHeight), JBUI.scale(logoLabel.myData[0].length * logoLabel.myLetterHeight));
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

      BufferedImage image = UIUtil.createImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics graphics = image.getGraphics();
      paint(graphics, logoLabel, myEmptyData, c.getBackground());
      paint(graphics, logoLabel, logoLabel.myData, c.getForeground());
      graphics.dispose();

      g.drawImage(image, 0, 0, c.getWidth(), c.getHeight(), c);
    }

    private void paint(Graphics g, AnimatedLogoLabel c, int[][] data, Color color) {
      for (int y = 0; y < data.length; y++) {
        int[] ints = data[y];

        for (int x = 0; x < ints.length; x++) {
          int a = ints[x];

          if (a > 0) {
            int size = myLetterHeightScaled != null ? myLetterHeightScaled : JBUI.scale(c.myLetterHeight);
            g.setColor(color);
            g.fillRect(y * size, x * size, size, size);
          }
        }
      }
    }
  }

  private static final int[] ourOffsets = new int[]{1, 7, 13, 19, 25, 31, 37};

  private int[][] myData = new int[43][7];

  private int myValue;

  private Runnable myTask;

  private final int myLetterHeight;
  private final boolean myAnimated;

  private boolean[] myStates = new boolean[Names.ourName.length()];

  private Future<?> myFuture;

  private final Object lock = new Object();

  public AnimatedLogoLabel(int letterHeightInPixels, boolean animated, boolean unstableScaling) {
    myLetterHeight = letterHeightInPixels;
    myAnimated = animated;

    Random random = new Random();
    Map<Character, AlphabetDraw> characterDraws = Alphabet.validCharacters;
    Character[] abc = Alphabet.alphabet;

    char[] str;
    if (myAnimated) {
      if (random.nextInt(100_000) < 100) {
        str = Names.ourEasterNames[random.nextInt(Names.ourEasterNames.length)].toCharArray();
      }
      else {
        str = new char[Names.ourName.length()];
        for (int i = 0; i < str.length; i++) {
          while (true) {
            str[i] = abc[random.nextInt(abc.length)];
            if (str[i] != Names.ourName.charAt(i)) {
              break;
            }
          }
        }
      }
    }
    else {
      str = Names.ourName.toCharArray();
    }

    for (int i = 0; i < ourOffsets.length; i++) {
      int offset = ourOffsets[i];
      char c = str[i];
      characterDraws.get(c).draw(offset, myData);
    }

    if (myAnimated) {
      myTask = () -> {
        synchronized (lock) {
          float per = 100f / Names.ourName.length();

          boolean end = myValue >= 93;
          int letterPosition = (int)(myValue / per);

          if (end) {
            letterPosition = Names.ourName.length();
          }

          for (int i = 0; i < letterPosition; i++) {
            boolean state = myStates[i];
            if (!state) {
              myStates[i] = true;
              fillAtOffset(characterDraws, Names.ourName.charAt(i), i);
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
              randomIndex = random.nextInt(abc.length);
              if (abc[randomIndex] != Names.ourName.charAt(letterPosition)) {
                break;
              }
            }

            fillAtOffset(characterDraws, abc[randomIndex], letterPosition);

            repaintAll();
          }
        }
      };
    }

    setUI(new MyComponentUI(this, unstableScaling));
  }

  private void repaintAll() {
    update(getGraphics());
  }

  public void start() {
    if (myAnimated) {
      myFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(myTask, 50, 20, TimeUnit.MILLISECONDS);
    }
  }

  public void stop() {
    if (myFuture != null) {
      myFuture.cancel(false);
    }
  }

  private void fillAtOffset(Map<Character, AlphabetDraw> characterDraws, char c, int pos) {
    fillAtOffset(characterDraws, c, pos, myData);
  }

  private static void fillAtOffset(Map<Character, AlphabetDraw> characterDraws, char c, int pos, int[][] data) {
    int offset = ourOffsets[pos];
    for (int i = 0; i < 5; i++) {
      int[] ints = data[i + offset];
      for (int j = 0; j < ints.length; j++) {
        ints[j] = 0;
      }
    }

    characterDraws.get(c).draw(offset, data);
  }

  public void setValue(int value) {
    synchronized (lock) {
      myValue = value;
    }
  }

  @Override
  public void updateUI() {
  }
}
