/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.editor.impl.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.openapi.editor.ex.MarkupIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.desktop.editor.impl.DesktopEditorErrorPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author VISTALL
 * @since 2018-04-28
 */
public class DesktopEditorErrorPanelUI extends ComponentUI {
  private static class PositionedStripe {
    @Nonnull
    private ColorValue color;
    private int yEnd;
    private final boolean thin;
    private final int layer;

    private PositionedStripe(@Nonnull ColorValue color, int yEnd, boolean thin, int layer) {
      this.color = color;
      this.yEnd = yEnd;
      this.thin = thin;
      this.layer = layer;
    }
  }

  public static ComponentUI createUI(JComponent c) {
    return new DesktopEditorErrorPanelUI((DesktopEditorErrorPanel)c);
  }

  public static final ProperTextRange WHOLE_DOCUMENT = new ProperTextRange(0, 0);

  private final DesktopEditorErrorPanel myPanel;
  private ProperTextRange myDirtyYPositions;
  @Nullable
  private BufferedImage myCachedTrack;
  private int myCachedHeight = -1;

  private MouseListener myMouseListener;
  private MouseMotionListener myMouseMotionListener;
  private MouseWheelListener myMouseWheelListener;

  private DesktopEditorErrorPanelUI(DesktopEditorErrorPanel panel) {
    myPanel = panel;
  }

  @Override
  public void installUI(JComponent c) {
    c.addMouseListener(myMouseListener = new MouseAdapter() {
      @Override
      @RequiredUIAccess
      public void mouseClicked(MouseEvent e) {
        myPanel.mouseClicked(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myPanel.cancelMyToolTips(e, true);
      }
    });

    c.addMouseMotionListener(myMouseMotionListener = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        myPanel.mouseMoved(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        myPanel.mouseDragged(e);
      }
    });

    c.addMouseWheelListener(myMouseWheelListener = e -> myPanel.mouseWheelMoved(e));
  }

  @Override
  public void uninstallUI(JComponent c) {
    c.removeMouseListener(myMouseListener);
    c.removeMouseMotionListener(myMouseMotionListener);
    c.removeMouseWheelListener(myMouseWheelListener);
  }

  public ProperTextRange getDirtyYPositions() {
    return myDirtyYPositions;
  }

  public void setDirtyYPositions(ProperTextRange dirtyYPositions) {
    myDirtyYPositions = dirtyYPositions;
    myCachedTrack = null;
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    return new Dimension(JBUI.scale(6) + HighlightDisplayLevel.getEmptyIconDim(), 0);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    Rectangle componentBounds = c.getBounds();
    ProperTextRange docRange = ProperTextRange.create(0, componentBounds.height);
    if (myCachedTrack == null || myCachedHeight != componentBounds.height) {
      myCachedTrack = UIUtil.createImage(c, componentBounds.width, componentBounds.height, BufferedImage.TYPE_INT_ARGB);
      myCachedHeight = componentBounds.height;

      myDirtyYPositions = docRange;

      paintBackground(myCachedTrack.getGraphics(), new Rectangle(0, 0, componentBounds.width, componentBounds.height));
    }

    if (myDirtyYPositions == WHOLE_DOCUMENT) {
      myDirtyYPositions = docRange;
    }

    if (myDirtyYPositions != null) {
      final Graphics2D imageGraphics = myCachedTrack.createGraphics();

      myDirtyYPositions = myDirtyYPositions.intersection(docRange);
      if (myDirtyYPositions == null) myDirtyYPositions = docRange;
      repaint(imageGraphics, componentBounds.width, myDirtyYPositions);
      myDirtyYPositions = null;
    }

    UIUtil.drawImage(g, myCachedTrack, null, 0, 0);

    if(myPanel.isSmallIconVisible()) {
      ErrorStripeRenderer errorStripeRenderer = myPanel.getMarkupModel().getErrorStripeRenderer();

      if (errorStripeRenderer != null) {
        errorStripeRenderer.paint(c, g, new Rectangle(JBUI.scale(2), JBUI.scale(2), errorStripeRenderer.getSquareSize(), errorStripeRenderer.getSquareSize()));
      }
    }
  }

  public void dropCache() {
    myCachedTrack = null;
  }

  private void repaint(@Nonnull final Graphics g, int gutterWidth, @Nonnull ProperTextRange yrange) {
    final Rectangle clip = new Rectangle(0, yrange.getStartOffset(), gutterWidth, yrange.getLength() + myPanel.getMarkupModel().getMinMarkHeight());

    paintBackground(g, clip);

    int startOffset = myPanel.yPositionToOffset(clip.y - myPanel.getMarkupModel().getMinMarkHeight(), true);
    int endOffset = myPanel.yPositionToOffset(clip.y + clip.height, false);

    Shape oldClip = g.getClip();
    g.clipRect(clip.x, clip.y, clip.width, clip.height);

    DesktopEditorImpl editor = myPanel.getEditor();
    drawMarkup(g, startOffset, endOffset, editor.getFilteredDocumentMarkupModel(), myPanel.getMarkupModel());

    g.setClip(oldClip);
  }

  private void drawMarkup(@Nonnull final Graphics g, int startOffset, int endOffset, @Nonnull MarkupModelEx markup1, @Nonnull MarkupModelEx markup2) {
    final Queue<PositionedStripe> thinEnds = new PriorityQueue<>(5, (o1, o2) -> o1.yEnd - o2.yEnd);
    final Queue<PositionedStripe> wideEnds = new PriorityQueue<>(5, (o1, o2) -> o1.yEnd - o2.yEnd);
    // sorted by layer
    final List<PositionedStripe> thinStripes = new ArrayList<>(); // layer desc
    final List<PositionedStripe> wideStripes = new ArrayList<>(); // layer desc
    final int[] thinYStart = new int[1];  // in range 0..yStart all spots are drawn
    final int[] wideYStart = new int[1];  // in range 0..yStart all spots are drawn

    MarkupIterator<RangeHighlighterEx> iterator1 = markup1.overlappingIterator(startOffset, endOffset);
    MarkupIterator<RangeHighlighterEx> iterator2 = markup2.overlappingIterator(startOffset, endOffset);
    MarkupIterator<RangeHighlighterEx> iterator = MarkupIterator.mergeIterators(iterator1, iterator2, RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    try {
      ContainerUtil.process(iterator, highlighter -> {
        ColorValue color = highlighter.getErrorStripeMarkColor();
        if (color == null) return true;
        boolean isThin = highlighter.isThinErrorStripeMark();
        int[] yStart = isThin ? thinYStart : wideYStart;
        List<PositionedStripe> stripes = isThin ? thinStripes : wideStripes;
        Queue<PositionedStripe> ends = isThin ? thinEnds : wideEnds;

        ProperTextRange range = myPanel.offsetsToYPositions(highlighter.getStartOffset(), highlighter.getEndOffset());
        final int ys = range.getStartOffset();
        int ye = range.getEndOffset();
        if (ye - ys < myPanel.getMarkupModel().getMinMarkHeight()) ye = ys + myPanel.getMarkupModel().getMinMarkHeight();

        yStart[0] = drawStripesEndingBefore(ys, ends, stripes, g, yStart[0]);

        final int layer = highlighter.getLayer();

        PositionedStripe stripe = null;
        int i;
        for (i = 0; i < stripes.size(); i++) {
          PositionedStripe s = stripes.get(i);
          if (s.layer == layer) {
            stripe = s;
            break;
          }
          if (s.layer < layer) {
            break;
          }
        }
        if (stripe == null) {
          // started new stripe, draw previous above
          if (i == 0 && yStart[0] != ys) {
            if (!stripes.isEmpty()) {
              PositionedStripe top = stripes.get(0);
              drawSpot(g, top.thin, yStart[0], ys, top.color);
            }
            yStart[0] = ys;
          }
          stripe = new PositionedStripe(color, ye, isThin, layer);
          stripes.add(i, stripe);
          ends.offer(stripe);
        }
        else {
          if (stripe.yEnd < ye) {
            if (!color.equals(stripe.color)) {
              // paint previous stripe on this layer
              if (i == 0 && yStart[0] != ys) {
                drawSpot(g, stripe.thin, yStart[0], ys, stripe.color);
                yStart[0] = ys;
              }
              stripe.color = color;
            }

            // key changed, reinsert into queue
            ends.remove(stripe);
            stripe.yEnd = ye;
            ends.offer(stripe);
          }
        }
        return true;
      });
    }
    finally {
      iterator.dispose();
    }

    drawStripesEndingBefore(Integer.MAX_VALUE, thinEnds, thinStripes, g, thinYStart[0]);
    drawStripesEndingBefore(Integer.MAX_VALUE, wideEnds, wideStripes, g, wideYStart[0]);
  }

  private int drawStripesEndingBefore(int ys, @Nonnull Queue<PositionedStripe> ends, @Nonnull List<PositionedStripe> stripes, @Nonnull Graphics g, int yStart) {
    while (!ends.isEmpty()) {
      PositionedStripe endingStripe = ends.peek();
      if (endingStripe.yEnd > ys) break;
      ends.remove();

      // check whether endingStripe got obscured in the range yStart..endingStripe.yEnd
      int i = stripes.indexOf(endingStripe);
      stripes.remove(i);
      if (i == 0) {
        // visible
        drawSpot(g, endingStripe.thin, yStart, endingStripe.yEnd, endingStripe.color);
        yStart = endingStripe.yEnd;
      }
    }
    return yStart;
  }

  private void drawSpot(@Nonnull Graphics g, boolean thinErrorStripeMark, int yStart, int yEnd, @Nonnull ColorValue color) {
    int paintWidth;
    int x;
    if (thinErrorStripeMark) {
      //noinspection SuspiciousNameCombination
      paintWidth = myPanel.getMarkupModel().getMinMarkHeight();
      x = myPanel.getEditor().isMirrored() ? myPanel.getWidth() - paintWidth - getBorderWidth() : getBorderWidth();
      if (yEnd - yStart < 6) {
        yStart -= JBUI.scale(1);
        yEnd += yEnd - yStart - JBUI.scale(1);
      }
    }
    else {
      x = myPanel.getEditor().isMirrored() ? getBorderWidth() : myPanel.getMarkupModel().getMinMarkHeight() + getBorderWidth();
      paintWidth = myPanel.getWidth() - myPanel.getMarkupModel().getMinMarkHeight();
    }
    g.setColor(TargetAWT.to(color));
    g.fillRect(x, yStart, paintWidth, yEnd - yStart);
  }

  private void paintBackground(Graphics g, @Nonnull Rectangle bounds) {
    g.setColor(UIUtil.getPanelBackground());
    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

    g.setColor(UIUtil.getBorderColor());
    int border = myPanel.getEditor().isMirrored() ? bounds.x + bounds.width - getBorderWidth() : bounds.x;
    g.drawLine(border, bounds.y, border, bounds.y + bounds.height + getBorderWidth());
  }

  private int getBorderWidth() {
    return JBUI.scale(1);
  }
}
