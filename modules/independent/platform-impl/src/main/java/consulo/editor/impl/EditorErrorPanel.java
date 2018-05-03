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
package consulo.editor.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.ex.MarkupIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.impl.IntervalTreeImpl;
import com.intellij.openapi.editor.impl.TrafficTooltipRenderer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.PopupHandler;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
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
public class EditorErrorPanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, UISettingsListener {

  private static class PositionedStripe {
    @Nonnull
    private Color color;
    private int yEnd;
    private final boolean thin;
    private final int layer;

    private PositionedStripe(@Nonnull Color color, int yEnd, boolean thin, int layer) {
      this.color = color;
      this.yEnd = yEnd;
      this.thin = thin;
      this.layer = layer;
    }
  }

  private final EditorImpl myEditor;
  private final EditorMarkupModelImpl myMarkupModel;

  private PopupHandler myHandler;
  @Nullable
  private BufferedImage myCachedTrack;
  private int myCachedHeight = -1;

  public EditorErrorPanel(EditorImpl editor, EditorMarkupModelImpl markupModel) {
    myEditor = editor;
    myMarkupModel = markupModel;
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public EditorMarkupModelImpl getMarkupModel() {
    return myMarkupModel;
  }

  public EditorImpl getEditor() {
    return myEditor;
  }

  @Override
  public void updateUI() {
    setUI(EditorErrorPanelUI.createUI(this));
    myCachedTrack = null;
  }

  @Override
  public void uiSettingsChanged(UISettings source) {
    if (!UISettings.getInstance().SHOW_EDITOR_TOOLTIP) {
      hideMyEditorPreviewHint();
    }
  }

  //@Override
  //protected void paintComponent(Graphics g) {
  //  //Rectangle componentBounds = getBounds();
  //  //ProperTextRange docRange = ProperTextRange.create(0, componentBounds.height);
  //  //if (myCachedTrack == null || myCachedHeight != componentBounds.height) {
  //  //  myCachedTrack = UIUtil.createImage(componentBounds.width, componentBounds.height, BufferedImage.TYPE_INT_ARGB);
  //  //  myCachedHeight = componentBounds.height;
  //  //  myDirtyYPositions = docRange;
  //  //  paintBackground(myCachedTrack.getGraphics(), new Rectangle(0, 0, componentBounds.width, componentBounds.height));
  //  //}
  //  //if (myDirtyYPositions == WHOLE_DOCUMENT) {
  //  //  myDirtyYPositions = docRange;
  //  //}
  //  //if (myDirtyYPositions != null) {
  //  //  final Graphics2D imageGraphics = myCachedTrack.createGraphics();
  //  //
  //  //  ((ApplicationImpl)ApplicationManager.getApplication()).editorPaintStart();
  //  //
  //  //  try {
  //  //    myDirtyYPositions = myDirtyYPositions.intersection(docRange);
  //  //    if (myDirtyYPositions == null) myDirtyYPositions = docRange;
  //  //    repaint(imageGraphics, componentBounds.width, myDirtyYPositions);
  //  //    myDirtyYPositions = null;
  //  //  }
  //  //  finally {
  //  //    ((ApplicationImpl)ApplicationManager.getApplication()).editorPaintFinish();
  //  //  }
  //  //}
  //  //
  //  //UIUtil.drawImage(g, myCachedTrack, null, 0, 0);
  //  //
  //  //if (myErrorStripeRenderer != null) {
  //  //  myErrorStripeRenderer.paint(this, g, new Point(JBUI.scale(1), 0));
  //  //}
  //}

  private void paintBackground(Graphics g, Rectangle bounds) {
    g.setColor(UIUtil.getPanelBackground());
    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

    g.setColor(UIUtil.getBorderColor());
    int border = myEditor.isMirrored() ? bounds.x + bounds.width - getBorderWidth() : bounds.x;
    g.drawLine(border, bounds.y, border, bounds.y + bounds.height + getBorderWidth());
  }

  private int getBorderWidth() {
    return JBUI.scale(1);
  }

  private void repaint(@Nonnull final Graphics g, int gutterWidth, @Nonnull ProperTextRange yrange) {
    final Rectangle clip = new Rectangle(0, yrange.getStartOffset(), gutterWidth, yrange.getLength() + myMarkupModel.getMinMarkHeight());
    paintBackground(g, clip);

    Document document = myEditor.getDocument();
    int startOffset = myMarkupModel.yPositionToOffset(clip.y - myMarkupModel.getMinMarkHeight(), true);
    int endOffset = myMarkupModel.yPositionToOffset(clip.y + clip.height, false);

    Shape oldClip = g.getClip();
    g.clipRect(clip.x, clip.y, clip.width, clip.height);

    drawMarkup(g, startOffset, endOffset, myEditor.getFilteredDocumentMarkupModel(), myMarkupModel);

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
    MarkupIterator<RangeHighlighterEx> iterator = IntervalTreeImpl.mergeIterators(iterator1, iterator2, RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    try {
      ContainerUtil.process(iterator, new Processor<RangeHighlighterEx>() {
        @Override
        public boolean process(@Nonnull RangeHighlighterEx highlighter) {
          Color color = highlighter.getErrorStripeMarkColor();
          if (color == null) return true;
          boolean isThin = highlighter.isThinErrorStripeMark();
          int[] yStart = isThin ? thinYStart : wideYStart;
          List<PositionedStripe> stripes = isThin ? thinStripes : wideStripes;
          Queue<PositionedStripe> ends = isThin ? thinEnds : wideEnds;

          ProperTextRange range = myMarkupModel.offsetsToYPositions(highlighter.getStartOffset(), highlighter.getEndOffset());
          final int ys = range.getStartOffset();
          int ye = range.getEndOffset();
          if (ye - ys < myMarkupModel.getMinMarkHeight()) ye = ys + myMarkupModel.getMinMarkHeight();

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
        }
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

  private void drawSpot(@Nonnull Graphics g, boolean thinErrorStripeMark, int yStart, int yEnd, @Nonnull Color color) {
    int paintWidth;
    int x;
    if (thinErrorStripeMark) {
      //noinspection SuspiciousNameCombination
      paintWidth = myMarkupModel.getMinMarkHeight();
      x = myEditor.isMirrored() ? getWidth() - paintWidth - getBorderWidth() : getBorderWidth();
      if (yEnd - yStart < 6) {
        yStart -= JBUI.scale(1);
        yEnd += yEnd - yStart - JBUI.scale(1);
      }
    }
    else {
      x = myEditor.isMirrored() ? getBorderWidth() : myMarkupModel.getMinMarkHeight() + getBorderWidth();
      paintWidth = getWidth() - myMarkupModel.getMinMarkHeight();
    }
    g.setColor(color);
    g.fillRect(x, yStart, paintWidth, yEnd - yStart);
  }

  // mouse events
  @Override
  @RequiredDispatchThread
  public void mouseClicked(final MouseEvent e) {
    CommandProcessor.getInstance().executeCommand(myEditor.getProject(), new Runnable() {
      @Override
      @RequiredDispatchThread
      public void run() {
        doMouseClicked(e);
      }
    }, EditorBundle.message("move.caret.command.name"), DocCommandGroupId.noneGroupId(myEditor.getDocument()), UndoConfirmationPolicy.DEFAULT, myEditor.getDocument());
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @RequiredDispatchThread
  private void doMouseClicked(MouseEvent e) {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
      IdeFocusManager.getGlobalInstance().requestFocus(myEditor.getContentComponent(), true);
    });

    int lineCount = myEditor.getDocument().getLineCount() + myEditor.getSettings().getAdditionalLinesCount();
    if (lineCount == 0) {
      return;
    }
    if (e.getX() > 0 && e.getX() <= getWidth()) {
      myMarkupModel.doClick(e);
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    //int lineCount = myEditor.getDocument().getLineCount() + myEditor.getSettings().getAdditionalLinesCount();
    //if (lineCount == 0) {
    //  return;
    //}
    //
    //if (myErrorStripeRenderer != null && e.getY() < myErrorStripeRenderer.getSquareSize()) {
    //  showTrafficLightTooltip(e);
    //  return;
    //}
    //
    //if (e.getX() > 0 && e.getX() <= getWidth() && showToolTipByMouseMove(e)) {
    //  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    //  return;
    //}
    //
    //cancelMyToolTips(e, false);
    //
    //if (getCursor().equals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))) {
    //  setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    //}
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    //if (myEditorPreviewHint == null) return;
    //myWheelAccumulator += (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL ? e.getUnitsToScroll() * e.getScrollAmount() : e.getWheelRotation() < 0 ? -e.getScrollAmount() : e.getScrollAmount());
    //myRowAdjuster = myWheelAccumulator / myEditor.getLineHeight();
    //showToolTipByMouseMove(e);
  }

  private TrafficTooltipRenderer myTrafficTooltipRenderer;

  private void showTrafficLightTooltip(MouseEvent e) {
    //if (myTrafficTooltipRenderer == null) {
    //  myTrafficTooltipRenderer = myTooltipRendererProvider.createTrafficTooltipRenderer(new Runnable() {
    //    @Override
    //    public void run() {
    //      myTrafficTooltipRenderer = null;
    //    }
    //  }, myEditor);
    //}
    //showTooltip(e, myTrafficTooltipRenderer, new HintHint(e).setAwtTooltip(true).setMayCenterPosition(true).setContentActive(false).setPreferredPosition(Balloon.Position.atLeft));
  }

  public void repaintTrafficTooltip() {
    if (myTrafficTooltipRenderer != null) {
      myTrafficTooltipRenderer.repaintTooltipWindow();
    }
  }

  private void cancelMyToolTips(final MouseEvent e, boolean checkIfShouldSurvive) {
    //hideMyEditorPreviewHint();
    //final TooltipController tooltipController = TooltipController.getInstance();
    //if (!checkIfShouldSurvive || !tooltipController.shouldSurvive(e)) {
    //  tooltipController.cancelTooltip(ERROR_STRIPE_TOOLTIP_GROUP, e, true);
    //}
  }

  private void hideMyEditorPreviewHint() {
    //if (myEditorPreviewHint != null) {
    //  myEditorPreviewHint.hide();
    //  myEditorPreviewHint = null;
    //  myRowAdjuster = 0;
    //  myWheelAccumulator = 0;
    //}
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
    cancelMyToolTips(e, true);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    cancelMyToolTips(e, true);
  }

  public void setPopupHandler(@Nullable PopupHandler handler) {
    if (myHandler != null) {
      removeMouseListener(myHandler);
    }

    if (handler != null) {
      myHandler = handler;
      addMouseListener(handler);
    }
  }
}
