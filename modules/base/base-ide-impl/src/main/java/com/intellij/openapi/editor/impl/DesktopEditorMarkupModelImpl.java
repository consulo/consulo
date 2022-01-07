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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Apr 19, 2002
 * Time: 2:56:43 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.openapi.editor.impl;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.hint.*;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorUIUtil;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.DisposerUtil;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.*;
import com.intellij.util.Alarm;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.desktop.editor.impl.DesktopEditorAnalyzeStatusPanel;
import consulo.desktop.editor.impl.DesktopEditorErrorPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntMaps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
@DeprecationInfo("Desktop only implementation")
@SuppressWarnings("deprecation")
public class DesktopEditorMarkupModelImpl extends MarkupModelImpl implements EditorMarkupModel {
  private static final int myPreviewLines = 5;// Actually preview has myPreviewLines * 2 + 1 lines (above + below + current one)
  private static final int myCachePreviewLines = 100;// Actually cache image has myCachePreviewLines * 2 + 1 lines (above + below + current one)

  private final DesktopEditorImpl myEditor;
  // null renderer means we should not show traffic light icon
  private ErrorStripeRenderer myErrorStripeRenderer;
  private final List<ErrorStripeListener> myErrorMarkerListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private final DesktopEditorErrorPanel myErrorPanel;

  @Nonnull
  private ErrorStripTooltipRendererProvider myTooltipRendererProvider = new BasicTooltipRendererProvider();

  private int myMinMarkHeight = JBUI.scale(2);

  private LightweightHint myEditorPreviewHint = null;
  private final EditorFragmentRenderer myEditorFragmentRenderer;
  private int myRowAdjuster = 0;
  private int myWheelAccumulator = 0;

  private DesktopEditorAnalyzeStatusPanel myStatusPanel;

  DesktopEditorMarkupModelImpl(@Nonnull DesktopEditorImpl editor) {
    super(editor.getDocument());
    myEditor = editor;
    myEditorFragmentRenderer = new EditorFragmentRenderer();
    myStatusPanel = new DesktopEditorAnalyzeStatusPanel(this);
    myErrorPanel = new DesktopEditorErrorPanel(editor, myStatusPanel, this);
  }

  public void updateUI() {
    myStatusPanel.updateUI();
  }

  @Override
  public int getMinMarkHeight() {
    return myMinMarkHeight;
  }

  void recalcEditorDimensions() {
    if (myErrorPanel != null) {
      myErrorPanel.recalcEditorDimensions();
    }
  }

  @Override
  public void repaintTrafficLightIcon() {
    myStatusPanel.repaintTrafficLightIcon();
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    if (myEditorPreviewHint == null) return;
    myWheelAccumulator += (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL ? e.getUnitsToScroll() * e.getScrollAmount() : e.getWheelRotation() < 0 ? -e.getScrollAmount() : e.getScrollAmount());
    myRowAdjuster = myWheelAccumulator / myEditor.getLineHeight();
    showToolTipByMouseMove(e);
  }

  public boolean showToolTipByMouseMove(final MouseEvent e) {
    boolean newLook = Registry.is("editor.new.mouse.hover.popups");

    if (myEditor.getVisibleLineCount() == 0) return false;
    MouseEvent me = new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), 0, e.getY() + 1, e.getClickCount(), e.isPopupTrigger());

    final int visualLine = getVisualLineByEvent(e);
    Rectangle area = myEditor.getScrollingModel().getVisibleArea();
    int visualY = myEditor.getLineHeight() * visualLine;
    boolean isVisible = area.contains(area.x, visualY) && myWheelAccumulator == 0;

    TooltipRenderer bigRenderer;
    if (IJSwingUtilities.findParentByInterface(myEditor.getComponent(), EditorWindowHolder.class) == null || isVisible || !UISettings.getInstance().SHOW_EDITOR_TOOLTIP) {
      final Set<RangeHighlighter> highlighters = new HashSet<>();
      getNearestHighlighters(this, me.getY(), highlighters);
      getNearestHighlighters((MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), getEditor().getProject(), true), me.getY(), highlighters);
      if (highlighters.isEmpty()) return false;

      int y = e.getY();
      RangeHighlighter nearest = getNearestRangeHighlighter(e);
      if (nearest != null) {
        ProperTextRange range = offsetsToYPositions(nearest.getStartOffset(), nearest.getEndOffset());
        int eachStartY = range.getStartOffset();
        int eachEndY = range.getEndOffset();
        y = eachStartY + (eachEndY - eachStartY) / 2;
      }
      me = new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), me.getX(), y + 1, e.getClickCount(), e.isPopupTrigger());
      bigRenderer = myTooltipRendererProvider.calcTooltipRenderer(highlighters);
      if (bigRenderer != null) {
        myErrorPanel.showTooltip(me, bigRenderer, createHint(me).setForcePopup(newLook));
        return true;
      }
      return false;
    }
    else {
      float rowRatio = (float)visualLine / (myEditor.getVisibleLineCount() - 1);
      int y = myRowAdjuster != 0 ? (int)(rowRatio * myEditor.getVerticalScrollBar().getHeight()) : me.getY();
      me = new MouseEvent(me.getComponent(), me.getID(), me.getWhen(), me.getModifiers(), me.getX(), y, me.getClickCount(), me.isPopupTrigger());
      final List<RangeHighlighterEx> highlighters = new ArrayList<>();
      collectRangeHighlighters(this, visualLine, highlighters);
      collectRangeHighlighters((MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), getEditor().getProject(), true), visualLine, highlighters);
      myEditorFragmentRenderer.update(visualLine, highlighters, me.isAltDown());
      myEditorFragmentRenderer.show(myEditor, me.getPoint(), true, DesktopEditorErrorPanel.ERROR_STRIPE_TOOLTIP_GROUP, createHint(me));
      return true;
    }
  }

  private static HintHint createHint(MouseEvent me) {
    return new HintHint(me).setAwtTooltip(true).setPreferredPosition(Balloon.Position.atLeft).setBorderInsets(new Insets(1, 1, 1, 1)).setShowImmediately(true).setAnimationEnabled(false);
  }

  private int getVisualLineByEvent(MouseEvent e) {
    return fitLineToEditor(myEditor.offsetToVisualLine(yPositionToOffset(e.getY() + myWheelAccumulator, true)));
  }

  private int fitLineToEditor(int visualLine) {
    return Math.max(0, Math.min(myEditor.getVisibleLineCount() - 1, visualLine));
  }

  private int getOffset(int visualLine, boolean startLine) {
    int logicalLine = myEditor.visualToLogicalPosition(new VisualPosition(visualLine, 0)).line;
    return startLine ? myEditor.getDocument().getLineStartOffset(logicalLine) : myEditor.getDocument().getLineEndOffset(logicalLine);
  }

  private void collectRangeHighlighters(MarkupModelEx markupModel, final int visualLine, final Collection<RangeHighlighterEx> highlighters) {
    final int startOffset = getOffset(fitLineToEditor(visualLine - myPreviewLines), true);
    final int endOffset = getOffset(fitLineToEditor(visualLine + myPreviewLines), false);
    markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset, new Processor<RangeHighlighterEx>() {
      @Override
      public boolean process(RangeHighlighterEx highlighter) {
        if (highlighter.getErrorStripeMarkColor() != null) {
          if (highlighter.getStartOffset() < endOffset && highlighter.getEndOffset() > startOffset) {
            highlighters.add(highlighter);
          }
        }
        return true;
      }
    });
  }

  @Nullable
  private RangeHighlighter getNearestRangeHighlighter(final MouseEvent e) {
    List<RangeHighlighter> highlighters = new ArrayList<>();
    getNearestHighlighters(this, e.getY(), highlighters);
    getNearestHighlighters((MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), myEditor.getProject(), true), e.getY(), highlighters);
    RangeHighlighter nearestMarker = null;
    int yPos = 0;
    for (RangeHighlighter highlighter : highlighters) {
      final int newYPos = offsetsToYPositions(highlighter.getStartOffset(), highlighter.getEndOffset()).getStartOffset();

      if (nearestMarker == null || Math.abs(yPos - e.getY()) > Math.abs(newYPos - e.getY())) {
        nearestMarker = highlighter;
        yPos = newYPos;
      }
    }
    return nearestMarker;
  }

  private void getNearestHighlighters(MarkupModelEx markupModel, final int scrollBarY, final Collection<RangeHighlighter> nearest) {
    int startOffset = yPositionToOffset(scrollBarY - myMinMarkHeight, true);
    int endOffset = yPositionToOffset(scrollBarY + myMinMarkHeight, false);
    markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset, new Processor<RangeHighlighterEx>() {
      @Override
      public boolean process(RangeHighlighterEx highlighter) {
        if (highlighter.getErrorStripeMarkColor() != null) {
          ProperTextRange range = offsetsToYPositions(highlighter.getStartOffset(), highlighter.getEndOffset());
          if (scrollBarY >= range.getStartOffset() - myMinMarkHeight * 2 && scrollBarY <= range.getEndOffset() + myMinMarkHeight * 2) {
            nearest.add(highlighter);
          }
        }
        return true;
      }
    });
  }

  public void hideMyEditorPreviewHint() {
    if (myEditorPreviewHint != null) {
      myEditorPreviewHint.hide();
      myEditorPreviewHint = null;
      myRowAdjuster = 0;
      myWheelAccumulator = 0;
    }
  }

  @RequiredUIAccess
  public void doClick(final MouseEvent e) {
    RangeHighlighter marker = getNearestRangeHighlighter(e);
    int offset;
    LogicalPosition logicalPositionToScroll = null;
    if (marker == null) {
      if (myEditorPreviewHint != null) {
        logicalPositionToScroll = myEditor.visualToLogicalPosition(new VisualPosition(myEditorFragmentRenderer.myStartVisualLine, 0));
        offset = myEditor.getDocument().getLineStartOffset(logicalPositionToScroll.line);
      }
      else {
        return;
      }
    }
    else {
      offset = marker.getStartOffset();
    }

    final Document doc = myEditor.getDocument();
    if (doc.getLineCount() > 0 && myEditorPreviewHint == null) {
      // Necessary to expand folded block even if navigating just before one
      // Very useful when navigating to first unused import statement.
      int lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset));
      myEditor.getCaretModel().moveToOffset(lineEnd);
    }
    myEditor.getCaretModel().removeSecondaryCarets();
    myEditor.getCaretModel().moveToOffset(offset);
    myEditor.getSelectionModel().removeSelection();
    ScrollingModel scrollingModel = myEditor.getScrollingModel();
    scrollingModel.disableAnimation();
    if (logicalPositionToScroll != null) {
      int lineY = myEditor.logicalPositionToXY(logicalPositionToScroll).y;
      int relativePopupOffset = myEditorFragmentRenderer.myRelativeY;
      scrollingModel.scrollVertically(lineY - relativePopupOffset);
    }
    else {
      scrollingModel.scrollToCaret(ScrollType.CENTER);
    }
    scrollingModel.enableAnimation();
    if (marker != null) {
      fireErrorMarkerClicked(marker, e);
    }
  }

  @Override
  public void setErrorStripeVisible(boolean val) {
    myErrorPanel.setPopupHandler(null);

    myStatusPanel.setTrafficLightIconVisible(val);

    updateErrorStripePanel();
  }

  public void updateErrorStripePanel() {
    if(!isErrorStripeVisible()) {
      return;
    }

    myEditor.getPanel().remove(myErrorPanel);

    myEditor.getPanel().add(myErrorPanel, myEditor.getVerticalScrollbarOrientation() == EditorEx.VERTICAL_SCROLLBAR_LEFT ? BorderLayout.WEST : BorderLayout.EAST);
  }

  @Nullable
  public DesktopEditorErrorPanel getErrorPanel() {
    return myErrorPanel;
  }

  @RequiredUIAccess
  @Override
  public void setErrorPanelPopupHandler(@Nonnull PopupHandler handler) {
    Application.get().assertIsDispatchThread();
    DesktopEditorErrorPanel errorPanel = getErrorPanel();
    if (errorPanel != null) {
      errorPanel.setPopupHandler(handler);
    }
  }

  @Override
  public void setErrorStripTooltipRendererProvider(@Nonnull final ErrorStripTooltipRendererProvider provider) {
    myTooltipRendererProvider = provider;
  }

  @Override
  @Nonnull
  public ErrorStripTooltipRendererProvider getErrorStripTooltipRendererProvider() {
    return myTooltipRendererProvider;
  }

  @Override
  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @RequiredUIAccess
  @Override
  public void setErrorStripeRenderer(ErrorStripeRenderer renderer) {
    assertIsDispatchThread();
    if (myErrorStripeRenderer instanceof Disposable) {
      Disposer.dispose((Disposable)myErrorStripeRenderer);
    }
    myErrorStripeRenderer = renderer;
    //try to not cancel tooltips here, since it is being called after every writeAction, even to the console
    //HintManager.getInstance().getTooltipController().cancelTooltips();
  }

  @RequiredUIAccess
  private static void assertIsDispatchThread() {
    Application.get().assertIsDispatchThread();
  }

  @Override
  public ErrorStripeRenderer getErrorStripeRenderer() {
    return myErrorStripeRenderer;
  }

  @Override
  public void dispose() {
    final DesktopEditorErrorPanel panel = getErrorPanel();
    if (panel != null) {
      panel.setPopupHandler(null);
    }

    if (myErrorStripeRenderer instanceof Disposable) {
      Disposer.dispose((Disposable)myErrorStripeRenderer);
    }

    Disposer.dispose(myStatusPanel);

    myErrorStripeRenderer = null;
    super.dispose();
  }

  // startOffset == -1 || endOffset == -1 means whole document
  void repaint(int startOffset, int endOffset) {
    if (myErrorPanel != null) {
      myErrorPanel.repaint(startOffset, endOffset);
    }
  }

  private boolean isMirrored() {
    return myEditor.isMirrored();
  }

  @RequiredUIAccess
  private void fireErrorMarkerClicked(RangeHighlighter marker, MouseEvent e) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ErrorStripeEvent event = new ErrorStripeEvent(getEditor(), e, marker);
    for (ErrorStripeListener listener : myErrorMarkerListeners) {
      listener.errorMarkerClicked(event);
    }
  }

  @Override
  public void addErrorMarkerListener(@Nonnull final ErrorStripeListener listener, @Nonnull Disposable parent) {
    DisposerUtil.add(listener, myErrorMarkerListeners, parent);
  }

  @Override
  public void setMinMarkHeight(final int minMarkHeight) {
    myMinMarkHeight = JBUI.scale(minMarkHeight);
  }

  @Override
  public boolean isErrorStripeVisible() {
    return getErrorPanel() != null;
  }

  private static class BasicTooltipRendererProvider implements ErrorStripTooltipRendererProvider {
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull final Collection<? extends RangeHighlighter> highlighters) {
      LineTooltipRenderer bigRenderer = null;
      //do not show same tooltip twice
      Set<String> tooltips = null;

      for (RangeHighlighter highlighter : highlighters) {
        final Object tooltipObject = highlighter.getErrorStripeTooltip();
        if (tooltipObject == null) continue;

        final String text = tooltipObject instanceof HighlightInfo ? ((HighlightInfo)tooltipObject).getToolTip() : tooltipObject.toString();
        if (text == null) continue;

        if (tooltips == null) {
          tooltips = new HashSet<>();
        }
        if (tooltips.add(text)) {
          if (bigRenderer == null) {
            bigRenderer = new LineTooltipRenderer(text, new Object[]{highlighters});
          }
          else {
            bigRenderer.addBelow(text);
          }
        }
      }

      return bigRenderer;
    }

    @Nonnull
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull final String text) {
      return new LineTooltipRenderer(text, new Object[]{text});
    }

    @Nonnull
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull final String text, final int width) {
      return new LineTooltipRenderer(text, width, new Object[]{text});
    }

    @Nonnull
    @Override
    public TrafficTooltipRenderer createTrafficTooltipRenderer(@Nonnull final Runnable onHide, @Nonnull Editor editor) {
      return new TrafficTooltipRenderer() {
        @Override
        public void repaintTooltipWindow() {
        }

        @Override
        public LightweightHint show(@Nonnull Editor editor, @Nonnull Point p, boolean alignToRight, @Nonnull TooltipGroup group, @Nonnull HintHint hintHint) {
          JLabel label = new JLabel("WTF");
          return new LightweightHint(label) {
            @Override
            public void hide() {
              super.hide();
              onHide.run();
            }
          };
        }
      };
    }
  }

  @Nonnull
  public ProperTextRange offsetsToYPositions(int start, int end) {
    return myErrorPanel.offsetsToYPositions(start, end);
  }

  public int yPositionToOffset(int y, boolean beginLine) {
    return myErrorPanel.yPositionToOffset(y, beginLine);
  }

  private class EditorFragmentRenderer implements TooltipRenderer {
    private int myVisualLine;
    private boolean myShowInstantly;
    private final List<RangeHighlighterEx> myHighlighters = new ArrayList<>();
    private BufferedImage myCacheLevel1;
    private BufferedImage myCacheLevel2;
    private int myCacheStartLine;
    private int myCacheEndLine;
    private int myStartVisualLine;
    private int myEndVisualLine;
    private int myRelativeY;
    private boolean myDelayed = false;
    private boolean isDirty = false;
    private final AtomicReference<Point> myPointHolder = new AtomicReference<>();
    private final AtomicReference<HintHint> myHintHolder = new AtomicReference<>();

    private EditorFragmentRenderer() {
      update(-1, Collections.<RangeHighlighterEx>emptyList(), false);
    }

    void update(int visualLine, Collection<RangeHighlighterEx> rangeHighlighters, boolean showInstantly) {
      myVisualLine = visualLine;
      myShowInstantly = showInstantly;
      myHighlighters.clear();
      if (myVisualLine == -1) return;
      int oldStartLine = myStartVisualLine;
      int oldEndLine = myEndVisualLine;
      myStartVisualLine = fitLineToEditor(myVisualLine - myPreviewLines);
      myEndVisualLine = fitLineToEditor(myVisualLine + myPreviewLines);
      isDirty |= oldStartLine != myStartVisualLine || oldEndLine != myEndVisualLine;
      for (RangeHighlighterEx rangeHighlighter : rangeHighlighters) {
        myHighlighters.add(rangeHighlighter);
      }
      Collections.sort(myHighlighters, new Comparator<RangeHighlighterEx>() {
        @Override
        public int compare(RangeHighlighterEx ex1, RangeHighlighterEx ex2) {
          LogicalPosition startPos1 = myEditor.offsetToLogicalPosition(ex1.getAffectedAreaStartOffset());
          LogicalPosition startPos2 = myEditor.offsetToLogicalPosition(ex2.getAffectedAreaStartOffset());
          if (startPos1.line != startPos2.line) return 0;
          return startPos1.column - startPos2.column;
        }
      });
    }

    @Override
    public LightweightHint show(@Nonnull final Editor editor, @Nonnull Point p, boolean alignToRight, @Nonnull TooltipGroup group, @Nonnull final HintHint hintInfo) {
      final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
      boolean needDelay = false;
      if (myEditorPreviewHint == null) {
        needDelay = true;
        final JPanel editorFragmentPreviewPanel = new JPanel() {
          private static final int R = 6;

          @Override
          public Dimension getPreferredSize() {
            int width = myEditor.getGutterComponentEx().getWidth() + myEditor.getScrollingModel().getVisibleArea().width;
            if (!ToolWindowManagerEx.getInstanceEx(myEditor.getProject()).getIdsOn(ToolWindowAnchor.LEFT).isEmpty()) width--;
            return new Dimension(width - BalloonImpl.POINTER_LENGTH.get(), myEditor.getLineHeight() * (myEndVisualLine - myStartVisualLine));
          }

          @Override
          protected void paintComponent(Graphics g) {
            if (myVisualLine == -1) return;
            Dimension size = getPreferredSize();
            EditorGutterComponentEx gutterComponentEx = myEditor.getGutterComponentEx();
            int gutterWidth = gutterComponentEx.getWidth();
            if (myCacheLevel2 == null || myCacheStartLine > myStartVisualLine || myCacheEndLine < myEndVisualLine) {
              myCacheStartLine = fitLineToEditor(myVisualLine - myCachePreviewLines);
              myCacheEndLine = fitLineToEditor(myCacheStartLine + 2 * myCachePreviewLines + JBUI.scale(1));
              if (myCacheLevel2 == null) {
                myCacheLevel2 = UIUtil.createImage(size.width, myEditor.getLineHeight() * (2 * myCachePreviewLines + JBUI.scale(1)), BufferedImage.TYPE_INT_RGB);
              }
              Graphics2D cg = myCacheLevel2.createGraphics();
              final AffineTransform t = cg.getTransform();
              EditorUIUtil.setupAntialiasing(cg);
              int lineShift = -myEditor.getLineHeight() * myCacheStartLine;

              AffineTransform translateInstance = AffineTransform.getTranslateInstance(-JBUI.scale(3), lineShift);
              translateInstance.preConcatenate(t);
              cg.setTransform(translateInstance);

              cg.setClip(0, -lineShift, gutterWidth, myCacheLevel2.getHeight());
              gutterComponentEx.paint(cg);
              translateInstance = AffineTransform.getTranslateInstance(gutterWidth - JBUI.scale(3), lineShift);
              translateInstance.preConcatenate(t);
              cg.setTransform(translateInstance);
              EditorComponentImpl contentComponent = myEditor.getContentComponent();
              cg.setClip(0, -lineShift, contentComponent.getWidth(), myCacheLevel2.getHeight());
              contentComponent.paint(cg);
            }
            if (myCacheLevel1 == null) {
              myCacheLevel1 = UIUtil.createImage(size.width, myEditor.getLineHeight() * (2 * myPreviewLines + JBUI.scale(1)), BufferedImage.TYPE_INT_RGB);
              isDirty = true;
            }
            if (isDirty) {
              myRelativeY = SwingUtilities.convertPoint(this, 0, 0, myEditor.getScrollPane()).y;
              Graphics2D g2d = myCacheLevel1.createGraphics();
              final AffineTransform transform = g2d.getTransform();
              EditorUIUtil.setupAntialiasing(g2d);
              GraphicsUtil.setupAAPainting(g2d);
              g2d.setColor(TargetAWT.to(myEditor.getBackgroundColor()));
              g2d.fillRect(0, 0, getWidth(), getHeight());
              AffineTransform translateInstance = AffineTransform.getTranslateInstance(gutterWidth, myEditor.getLineHeight() * (myCacheStartLine - myStartVisualLine));
              translateInstance.preConcatenate(transform);
              g2d.setTransform(translateInstance);
              UIUtil.drawImage(g2d, myCacheLevel2, -gutterWidth, 0, null);
              IntIntMap rightEdges = IntMaps.newIntIntHashMap();
              int h = myEditor.getLineHeight() - 2;
              for (RangeHighlighterEx ex : myHighlighters) {
                int hEndOffset = ex.getAffectedAreaEndOffset();
                Object tooltip = ex.getErrorStripeTooltip();
                if (tooltip == null) continue;
                String s = String.valueOf(tooltip);
                if (s.isEmpty()) continue;
                s = s.replaceAll("&nbsp;", " ").replaceAll("\\s+", " ");

                LogicalPosition logicalPosition = myEditor.offsetToLogicalPosition(hEndOffset);
                int endOfLineOffset = myEditor.getDocument().getLineEndOffset(logicalPosition.line);
                logicalPosition = myEditor.offsetToLogicalPosition(endOfLineOffset);
                Point placeToShow = myEditor.logicalPositionToXY(logicalPosition);
                logicalPosition = myEditor.xyToLogicalPosition(placeToShow);//wraps&foldings workaround
                placeToShow.x += R * 3 / 2;
                placeToShow.y -= myCacheStartLine * myEditor.getLineHeight() - 1;

                Font font = myEditor.getColorsScheme().getFont(EditorFontType.PLAIN);
                g2d.setFont(font.deriveFont(font.getSize() * .8F));
                int w = g2d.getFontMetrics().stringWidth(s);

                int rightEdge = rightEdges.getInt(logicalPosition.line);
                placeToShow.x = Math.max(placeToShow.x, rightEdge);
                rightEdge = Math.max(rightEdge, placeToShow.x + w + 3 * R);
                rightEdges.putInt(logicalPosition.line, rightEdge);

                g2d.setColor(MessageType.WARNING.getPopupBackground());
                g2d.fillRoundRect(placeToShow.x, placeToShow.y, w + JBUI.scale(2) * R, h, R, R);
                g2d.setColor(new JBColor(JBColor.GRAY, Gray._200));
                g2d.drawRoundRect(placeToShow.x, placeToShow.y, w + JBUI.scale(2) * R, h, R, R);
                g2d.setColor(JBColor.foreground());
                g2d.drawString(s, placeToShow.x + R, placeToShow.y + h - g2d.getFontMetrics(g2d.getFont()).getDescent() / 2 - 2);
              }
              isDirty = false;
            }
            Graphics2D g2 = (Graphics2D)g.create();
            try {
              GraphicsUtil.setupAAPainting(g2);
              g2.setClip(new RoundRectangle2D.Double(0, 0, size.width - .5, size.height - .5, 2, 2));
              UIUtil.drawImage(g2, myCacheLevel1, 0, 0, this);
              if (UIUtil.isUnderDarkTheme()) {
                //Add glass effect
                Shape s = new Rectangle(0, 0, size.width, size.height);
                double cx = size.width / 2;
                double cy = 0;
                double rx = size.width / 10;
                int ry = myEditor.getLineHeight() * 3 / 2;
                g2.setPaint(new GradientPaint(0, 0, Gray._255.withAlpha(75), 0, ry, Gray._255.withAlpha(10)));
                double pseudoMajorAxis = size.width - rx * 9 / 5;
                Shape topShape1 = new Ellipse2D.Double(cx - rx - pseudoMajorAxis / 2, cy - ry, 2 * rx, 2 * ry);
                Shape topShape2 = new Ellipse2D.Double(cx - rx + pseudoMajorAxis / 2, cy - ry, 2 * rx, 2 * ry);
                Area topArea = new Area(topShape1);
                topArea.add(new Area(topShape2));
                topArea.add(new Area(new Rectangle.Double(cx - pseudoMajorAxis / 2, cy, pseudoMajorAxis, ry)));
                g2.fill(topArea);
                Area bottomArea = new Area(s);
                bottomArea.subtract(topArea);
                g2.setPaint(new GradientPaint(0, size.height - ry, Gray._0.withAlpha(10), 0, size.height, Gray._255.withAlpha(30)));
                g2.fill(bottomArea);
              }
            }
            finally {
              g2.dispose();
            }
          }
        };
        myEditorPreviewHint = new LightweightHint(editorFragmentPreviewPanel) {

          @Override
          public void hide(boolean ok) {
            super.hide(ok);
            myCacheLevel1 = null;
            if (myCacheLevel2 != null) {
              myCacheLevel2 = null;
              myCacheStartLine = -1;
              myCacheEndLine = -1;
            }

            myDelayed = false;
          }
        };
        myEditorPreviewHint.setForceLightweightPopup(true);
      }
      Point point = new Point(hintInfo.getOriginalPoint());
      hintInfo.setTextBg(TargetAWT.to(myEditor.getColorsScheme().getDefaultBackground()));
      hintInfo.setBorderColor(TargetAWT.to(myEditor.getColorsScheme().getDefaultForeground()));
      point = SwingUtilities.convertPoint(((DesktopEditorImpl)editor).getVerticalScrollBar(), point, myEditor.getComponent().getRootPane());
      myPointHolder.set(point);
      myHintHolder.set(hintInfo);
      if (needDelay && !myShowInstantly) {
        myDelayed = true;
        Alarm alarm = new Alarm();
        alarm.addRequest(new Runnable() {
          @Override
          public void run() {
            if (myEditorPreviewHint == null || !myDelayed) return;
            showEditorHint(hintManager, myPointHolder.get(), myHintHolder.get());
            myDelayed = false;
          }
        }, /*Registry.intValue("ide.tooltip.initialDelay")*/300);
      }
      else if (!myDelayed) {
        showEditorHint(hintManager, point, hintInfo);
      }
      return myEditorPreviewHint;
    }

    private void showEditorHint(HintManagerImpl hintManager, Point point, HintHint hintInfo) {
      int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_MOUSEOVER | HintManager.HIDE_BY_ESCAPE | HintManager.HIDE_BY_SCROLLING;
      hintManager.showEditorHint(myEditorPreviewHint, myEditor, point, flags, 0, false, hintInfo);
    }
  }
}
