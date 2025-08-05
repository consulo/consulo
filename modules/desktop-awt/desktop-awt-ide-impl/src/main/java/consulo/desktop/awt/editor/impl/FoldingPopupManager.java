// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.ide.impl.idea.codeInsight.hint.DocumentFragmentTooltipRenderer;
import consulo.ide.impl.idea.codeInsight.hint.TooltipController;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.document.DocumentFragment;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * This class implements showing a preview of text in a collapsed fold region on mouse hover.
 */
public class FoldingPopupManager implements EditorMouseListener, EditorMouseMotionListener {
  private static final Key<Boolean> DISABLED = Key.create("FoldingPopupManager.disabled");
  private static final TooltipGroup FOLDING_TOOLTIP_GROUP = new TooltipGroup("FOLDING_TOOLTIP_GROUP", 10);
  private static final int TOOLTIP_DELAY_MS = 300;

  private final Alarm myAlarm;

  public static void disableForEditor(@Nonnull Editor editor) {
    editor.putUserData(DISABLED, Boolean.TRUE);
    TooltipController.getInstance().cancelTooltip(FOLDING_TOOLTIP_GROUP, null, true);
  }

  public static void enableForEditor(@Nonnull Editor editor) {
    editor.putUserData(DISABLED, null);
  }

  public FoldingPopupManager(CodeEditorBase editor) {
    myAlarm = new Alarm(editor.getDisposable());
    editor.addEditorMouseListener(this);
    editor.addEditorMouseMotionListener(this);
  }

  @RequiredUIAccess
  @Override
  public void mouseMoved(@Nonnull EditorMouseEvent e) {
    myAlarm.cancelAllRequests();
    Editor editor = e.getEditor();
    if (editor.getUserData(DISABLED) != null) return;
    if (e.getArea() == EditorMouseEventArea.EDITING_AREA) {
      MouseEvent mouseEvent = e.getMouseEvent();
      Point point = mouseEvent.getPoint();
      FoldRegion fold = ((EditorEx)editor).getFoldingModel().getFoldingPlaceholderAt(point);
      TooltipController controller = TooltipController.getInstance();
      if (fold != null && !fold.shouldNeverExpand()) {
        myAlarm.addRequest(() -> {
          if (editor.getUserData(DISABLED) != null || !editor.getComponent().isShowing() || !fold.isValid() || fold.isExpanded()) return;
          DocumentFragment range = createDocumentFragment(fold);
          Point p = SwingUtilities.convertPoint((Component)mouseEvent.getSource(), point, editor.getComponent().getRootPane().getLayeredPane());
          controller.showTooltip(editor, p, new DocumentFragmentTooltipRenderer(range), false, FOLDING_TOOLTIP_GROUP);
        }, TOOLTIP_DELAY_MS);
      }
      else {
        controller.cancelTooltip(FOLDING_TOOLTIP_GROUP, mouseEvent, true);
      }
    }
  }

  @Nonnull
  private static DocumentFragment createDocumentFragment(@Nonnull FoldRegion fold) {
    CodeEditorBase editor = (CodeEditorBase)fold.getEditor();
    Document document = editor.getDocument();
    FoldingGroup group = fold.getGroup();
    int startOffset = fold.getStartOffset();
    int endOffset = fold.getEndOffset();
    if (group != null) {
      int groupEndOffset = editor.getFoldingModel().getEndOffset(group);
      if (editor.offsetToVisualLine(groupEndOffset) == editor.offsetToVisualLine(startOffset)) {
        endOffset = groupEndOffset;
      }
    }
    return new DocumentFragment(document, startOffset, endOffset);
  }

  @Override
  public void mouseExited(@Nonnull EditorMouseEvent e) {
    myAlarm.cancelAllRequests();
    if (e.getEditor().getUserData(DISABLED) != null) return;
    TooltipController.getInstance().cancelTooltip(FOLDING_TOOLTIP_GROUP, e.getMouseEvent(), true);
  }
}
