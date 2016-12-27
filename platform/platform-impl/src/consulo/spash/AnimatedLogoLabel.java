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
package consulo.spash;

import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * @author VISTALL
 * @since 11-Dec-16.
 */
public class AnimatedLogoLabel extends JComponent {
  private static final int[] ourOffsets = new int[]{1, 7, 13, 19, 25, 31, 37};
  private final static String ourName = "CONSULO";

  private int[][] myData = new int[43][7];

  private volatile int myLastPosition;

  private volatile int myValue;

  private Thread myThread;

  private final int mySize;
  private final boolean myAnimated;

  public AnimatedLogoLabel(int size, boolean animated) {
    mySize = size;
    myAnimated = animated;

    Random random = new Random();
    Map<Character, AlphabetDraw> characterDraws = Alphabet.validCharacters;
    Character[] abc = Alphabet.alphabet;

    char[] str;
    if (myAnimated) {
      str = new char[ourName.length()];
      for (int i = 0; i < str.length; i++) {
        while (true) {
          str[i] = abc[random.nextInt(abc.length)];
          if (str[i] != ourName.charAt(i)) {
            break;
          }
        }
      }
    }
    else {
      str = ourName.toCharArray();
    }

    for (int i = 0; i < ourOffsets.length; i++) {
      int offset = ourOffsets[i];

      char c = str[i];


      characterDraws.get(c).draw(offset, myData);
    }

    if (myAnimated) {
      myThread = new Thread(() -> {
        while (true) {
          try {
            int per = 100 / ourName.length();

            int k = myValue / per;

            int l = myLastPosition;

            myLastPosition = k;

            // last pos
            if (k >= ourName.length()) {
              fillAtOffset(characterDraws, ourName.charAt(ourName.length() - 1), ourName.length() - 1);

              break;
            }
            else {
              int randomIndex = -1;
              while (true) {
                randomIndex = random.nextInt(abc.length);
                if (abc[randomIndex] != ourName.charAt(k)) {
                  break;
                }
              }

              fillAtOffset(characterDraws, abc[randomIndex], k);

              if (l != myLastPosition) {
                for (int i = 0; i < k; i++) {
                  fillAtOffset(characterDraws, ourName.charAt(i), i);
                }
              }
            }

            repaint();
          }
          catch (Exception e) {
            e.printStackTrace();
          }
          finally {
            TimeoutUtil.sleep(50L);
          }
        }
      });
      myThread.setName("Splash thread");
      myThread.setPriority(Thread.MAX_PRIORITY);
    }
  }

  public void start() {
    if (myAnimated) {
      myThread.start();
    }
  }

  private void fillAtOffset(Map<Character, AlphabetDraw> characterDraws, char c, int pos) {
    int offset = ourOffsets[pos];
    for (int i = 0; i < 5; i++) {
      int[] ints = myData[i + offset];
      for (int j = 0; j < ints.length; j++) {
        ints[j] = 0;
      }
    }

    characterDraws.get(c).draw(offset, myData);
  }

  public void setValue(int value) {
    myValue = value;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(JBUI.scale(myData.length * mySize), JBUI.scale(myData[0].length * mySize));
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public void paint(Graphics g) {
    for (int y = 0; y < myData.length; y++) {
      int[] ints = myData[y];

      for (int x = 0; x < ints.length; x++) {
        int a = ints[x];

        if (a > 0) {
          int size = JBUI.scale(mySize);
          g.setColor(getForeground());
          g.fillRect(y * size, x * size, size, size);
        }
      }
    }
  }
}
