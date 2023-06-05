/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.impl;

import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Encapsulates logic of drawing arrows at graphics buffer (primary usage is to draw tabulation symbols representation arrows).
 *
 * @author Denis Zhdanov
 * @since Jul 2, 2010 11:35:23 AM
 */
public class ArrowPainter {

  private final ColorProvider myColorHolder;
  private final Supplier<Integer> myWidthProvider;
  private final Supplier<Integer> myHeightProvider;

  /**
   * Creates an ArrowPainter with specified parameters.
   *
   * @param colorHolder defines arrow color
   * @param widthProvider defines character width, it is used to calculate an inset for the arrow's tip
   * @param heightProvider defines character height, it's used to calculate an arrow's width and height
   */
  public ArrowPainter(@Nonnull ColorProvider colorHolder, @Nonnull Supplier<Integer> widthProvider, @Nonnull Supplier<Integer> heightProvider) {
    myColorHolder = colorHolder;
    myWidthProvider = widthProvider;
    myHeightProvider = heightProvider;
  }

  /**
   * Paints arrow at the given graphics buffer using given coordinate parameters.
   *
   * @param g       target graphics buffer to use
   * @param y       defines baseline of the row where the arrow should be painted
   * @param start   starting <code>'x'</code> position to use during drawing
   * @param stop    ending <code>'x'</code> position to use during drawing
   */
  public void paint(Graphics g, int y, int start, int stop) {
    stop -= myWidthProvider.get() / 4;
    Color oldColor = g.getColor();
    g.setColor(TargetAWT.to(myColorHolder.getColor()));
    final int height = myHeightProvider.get();
    final int halfHeight = height / 2;
    int mid = y - halfHeight;
    int top = y - height;
    LinePainter2D.paint((Graphics2D)g, start, mid, stop, mid);
    LinePainter2D.paint((Graphics2D)g, stop, y, stop, top);
    g.fillPolygon(new int[]{stop - halfHeight, stop - halfHeight, stop}, new int[]{y, y - height, y - halfHeight}, 3);
    g.setColor(oldColor);
  }
}
