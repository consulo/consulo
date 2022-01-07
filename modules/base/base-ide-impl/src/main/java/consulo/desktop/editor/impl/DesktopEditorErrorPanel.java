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
package consulo.desktop.editor.impl;

import com.intellij.codeInsight.hint.TooltipController;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.DesktopEditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.HintHint;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import consulo.desktop.editor.impl.ui.DesktopEditorErrorPanelUI;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * @author VISTALL
 * @since 2018-04-28
 */
public class DesktopEditorErrorPanel extends JComponent implements UISettingsListener {
  public static final TooltipGroup ERROR_STRIPE_TOOLTIP_GROUP = new TooltipGroup("ERROR_STRIPE_TOOLTIP_GROUP", 0);

  private final DesktopEditorImpl myEditor;
  private final DesktopEditorAnalyzeStatusPanel myStatusPanel;
  private final DesktopEditorMarkupModelImpl myMarkupModel;

  private PopupHandler myHandler;

  private boolean dimensionsAreValid;
  private int myEditorScrollbarTop = -1;
  private int myEditorTargetHeight = -1;
  private int myEditorSourceHeight = -1;

  private boolean mySmallIconVisible = true;

  public DesktopEditorErrorPanel(DesktopEditorImpl editor, DesktopEditorAnalyzeStatusPanel statusPanel, DesktopEditorMarkupModelImpl markupModel) {
    myEditor = editor;
    myStatusPanel = statusPanel;
    myStatusPanel.setErrorPanel(this);
    myMarkupModel = markupModel;
    mySmallIconVisible = !EditorSettingsExternalizable.getInstance().isShowInspectionWidget();

    updateUI();

    uiSettingsChanged(UISettings.getInstance());
  }

  @Nonnull
  public ProperTextRange offsetsToYPositions(int start, int end) {
    if (!dimensionsAreValid) {
      recalcEditorDimensions();
    }
    Document document = myEditor.getDocument();
    int startLineNumber = end == -1 ? 0 : offsetToLine(start, document);
    int editorStartY = myEditor.visualLineToY(startLineNumber);
    int startY;
    int editorTargetHeight = Math.max(0, myEditorTargetHeight);
    if (myEditorSourceHeight < editorTargetHeight) {
      startY = myEditorScrollbarTop + editorStartY;
    }
    else {
      startY = myEditorScrollbarTop + (int)((float)editorStartY / myEditorSourceHeight * editorTargetHeight);
    }

    int endY;
    int endLineNumber = offsetToLine(end, document);
    if (end == -1 || start == -1) {
      endY = Math.min(myEditorSourceHeight, editorTargetHeight);
    }
    else if (startLineNumber == endLineNumber) {
      endY = startY; // both offsets are on the same line, no need to recalc Y position
    }
    else if (myEditorSourceHeight < editorTargetHeight) {
      endY = myEditorScrollbarTop + myEditor.visualLineToY(endLineNumber);
    }
    else {
      int editorEndY = myEditor.visualLineToY(endLineNumber);
      endY = myEditorScrollbarTop + (int)((float)editorEndY / myEditorSourceHeight * editorTargetHeight);
    }

    if (endY < startY) endY = startY;
    return new ProperTextRange(startY, endY);
  }

  private int offsetToLine(int offset, Document document) {
    if (offset < 0) {
      return 0;
    }
    if (offset > document.getTextLength()) {
      return document.getLineCount();
    }
    return myEditor.offsetToVisualLine(offset);
  }

  public int yPositionToOffset(int y, boolean beginLine) {
    if (!dimensionsAreValid) {
      recalcEditorDimensions();
    }
    final int safeY = Math.max(0, y - myEditorScrollbarTop);
    VisualPosition visual;
    if (myEditorSourceHeight < myEditorTargetHeight) {
      visual = myEditor.xyToVisualPosition(new Point(0, safeY));
    }
    else {
      float fraction = Math.max(0, Math.min(1, safeY / (float)myEditorTargetHeight));
      final int lineCount = myEditorSourceHeight / myEditor.getLineHeight();
      visual = new VisualPosition((int)(fraction * lineCount), 0);
    }
    int line = myEditor.visualToLogicalPosition(visual).line;
    Document document = myEditor.getDocument();
    if (line < 0) return 0;
    if (line >= document.getLineCount()) return document.getTextLength();

    final FoldingModelEx foldingModel = myEditor.getFoldingModel();
    if (beginLine) {
      final int offset = document.getLineStartOffset(line);
      final FoldRegion startCollapsed = foldingModel.getCollapsedRegionAtOffset(offset);
      return startCollapsed != null ? Math.min(offset, startCollapsed.getStartOffset()) : offset;
    }
    else {
      final int offset = document.getLineEndOffset(line);
      final FoldRegion startCollapsed = foldingModel.getCollapsedRegionAtOffset(offset);
      return startCollapsed != null ? Math.max(offset, startCollapsed.getEndOffset()) : offset;
    }
  }

  // startOffset == -1 || endOffset == -1 means whole document
  public void repaint(int startOffset, int endOffset) {
    ProperTextRange range = offsetsToYPositions(startOffset, endOffset);
    markDirtied(range);
    if (startOffset == -1 || endOffset == -1) {
      getUI().setDirtyYPositions(DesktopEditorErrorPanelUI.WHOLE_DOCUMENT);
    }

    if (isVisible()) {
      repaint(0, range.getStartOffset(), getWidth(), range.getLength() + myMarkupModel.getMinMarkHeight());
    }
  }

  public void markDirtied(@Nonnull ProperTextRange yPositions) {
    ProperTextRange dirtyYPositions = getUI().getDirtyYPositions();

    if (dirtyYPositions != DesktopEditorErrorPanelUI.WHOLE_DOCUMENT) {
      int start = Math.max(0, yPositions.getStartOffset() - myEditor.getLineHeight());
      int end = myEditorScrollbarTop + myEditorTargetHeight == 0
                ? yPositions.getEndOffset() + myEditor.getLineHeight()
                : Math.min(myEditorScrollbarTop + myEditorTargetHeight, yPositions.getEndOffset() + myEditor.getLineHeight());
      ProperTextRange adj = new ProperTextRange(start, Math.max(end, start));

      getUI().setDirtyYPositions(dirtyYPositions == null ? adj : dirtyYPositions.union(adj));
    }

    myEditorScrollbarTop = 0;
    myEditorSourceHeight = 0;
    myEditorTargetHeight = 0;
    dimensionsAreValid = false;
  }

  public void recalcEditorDimensions() {
    int scrollBarHeight = myEditor.getPanel().getHeight();

    myEditorScrollbarTop = getIconPanelSize();
    int editorScrollBarBottom = 0;
    JScrollBar horizontalScrollBar = myEditor.getScrollPane().getHorizontalScrollBar();
    if (horizontalScrollBar != null && horizontalScrollBar.isVisible()) {
      // reduce visible height by bottom scrollbar, and one just pixel
      editorScrollBarBottom = horizontalScrollBar.getHeight() + JBUI.scale(1);
    }

    myEditorTargetHeight = scrollBarHeight - myEditorScrollbarTop - editorScrollBarBottom;
    myEditorSourceHeight = myEditor.getPreferredHeight();

    dimensionsAreValid = scrollBarHeight != 0;
  }

  public int getIconPanelSize() {
    if (!mySmallIconVisible) {
      // just empty space
      return JBUIScale.scale(2);
    }

    ErrorStripeRenderer renderer = myMarkupModel.getErrorStripeRenderer();

    // 2px - top diff and bottom
    return renderer != null ? renderer.getSquareSize() + JBUIScale.scale(2) * 2 : 0;
  }

  public DesktopEditorMarkupModelImpl getMarkupModel() {
    return myMarkupModel;
  }

  public DesktopEditorImpl getEditor() {
    return myEditor;
  }

  public DesktopEditorErrorPanelUI getUI() {
    return (DesktopEditorErrorPanelUI)ui;
  }

  @Override
  public void updateUI() {
    setUI(DesktopEditorErrorPanelUI.createUI(this));
  }

  @Override
  public void uiSettingsChanged(UISettings source) {
    if (!source.SHOW_EDITOR_TOOLTIP) {
      hideMyEditorPreviewHint();
    }

    setBorder(JBUI.Borders.empty());
  }

  @RequiredUIAccess
  public void mouseClicked(final MouseEvent e) {
    CommandProcessor.getInstance().executeCommand(myEditor.getProject(), new Runnable() {
      @Override
      @RequiredUIAccess
      public void run() {
        doMouseClicked(e);
      }
    }, EditorBundle.message("move.caret.command.name"), DocCommandGroupId.noneGroupId(myEditor.getDocument()), UndoConfirmationPolicy.DEFAULT, myEditor.getDocument());
  }

  @RequiredUIAccess
  private void doMouseClicked(MouseEvent e) {
    if (mySmallIconVisible && e.getY() < getIconPanelSize()) {
      showTrafficLightTooltip(e);
      return;
    }

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

  public void mouseMoved(MouseEvent e) {
    int lineCount = myEditor.getDocument().getLineCount() + myEditor.getSettings().getAdditionalLinesCount();
    if (lineCount == 0) {
      return;
    }

    if (mySmallIconVisible && e.getY() < getIconPanelSize()) {
      cancelMyToolTips(e, false);
      return;
    }

    if (e.getX() > 0 && e.getX() <= getWidth() && myMarkupModel.showToolTipByMouseMove(e)) {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return;
    }

    cancelMyToolTips(e, false);

    if (getCursor().equals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }

  public void mouseDragged(MouseEvent e) {
    cancelMyToolTips(e, true);
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    myMarkupModel.mouseWheelMoved(e);
  }

  private void showTrafficLightTooltip(MouseEvent e) {
    myStatusPanel.showStatusPopup(e, (size) -> {
      JComponent component = (JComponent)e.getComponent();
      RelativePoint originalPoint = new RelativePoint(e);
      Point p = originalPoint.getPoint(component);
      return new RelativePoint(component, new Point(p.x - size.width - getIconPanelSize() / 2, p.y));
    });
  }

  public void showTooltip(MouseEvent e, final TooltipRenderer tooltipObject, @Nonnull HintHint hintHint) {
    TooltipController tooltipController = TooltipController.getInstance();
    tooltipController.showTooltipByMouseMove(myEditor, new RelativePoint(e), tooltipObject, myEditor.getVerticalScrollbarOrientation() == EditorEx.VERTICAL_SCROLLBAR_RIGHT, ERROR_STRIPE_TOOLTIP_GROUP,
                                             hintHint);
  }

  public void cancelMyToolTips(final MouseEvent e, boolean checkIfShouldSurvive) {
    hideMyEditorPreviewHint();
    final TooltipController tooltipController = TooltipController.getInstance();
    if (!checkIfShouldSurvive || !tooltipController.shouldSurvive(e)) {
      tooltipController.cancelTooltip(ERROR_STRIPE_TOOLTIP_GROUP, e, true);
    }
  }

  private void hideMyEditorPreviewHint() {
    myMarkupModel.hideMyEditorPreviewHint();
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

  public void setSmallIconVisible(boolean visible) {
    mySmallIconVisible = visible;

    recalcEditorDimensions();
    getUI().dropCache();
    getUI().setDirtyYPositions(DesktopEditorErrorPanelUI.WHOLE_DOCUMENT);
    repaint();
  }

  public boolean isSmallIconVisible() {
    return mySmallIconVisible;
  }
}
