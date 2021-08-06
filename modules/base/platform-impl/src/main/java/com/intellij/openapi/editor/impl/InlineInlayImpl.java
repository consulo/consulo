// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.DocumentEvent;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.CodeEditorInlayModelBase;
import consulo.util.dataholder.Key;
import com.intellij.util.DocumentUtil;
import javax.annotation.Nonnull;

import java.awt.*;
import java.util.List;

public class InlineInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, InlineInlayImpl> {
  private static final Key<Integer> ORDER_BEFORE_DISPOSAL = Key.create("inlay.order.before.disposal");

  public InlineInlayImpl(@Nonnull CodeEditorBase editor, int offset, boolean relatesToPrecedingText, @Nonnull R renderer) {
    super(editor, offset, relatesToPrecedingText, renderer);
  }

  @Override
  RangeMarkerTree<InlineInlayImpl> getTree() {
    return myEditor.getInlayModel().myInlineElementsTree;
  }

  @Override
  protected void changedUpdateImpl(@Nonnull DocumentEvent e) {
    myEditor.getInlayModel().myPutMergedIntervalsAtBeginning = intervalStart() == e.getOffset();
    super.changedUpdateImpl(e);
    if (isValid() && DocumentUtil.isInsideSurrogatePair(getDocument(), intervalStart())) {
      invalidate(e);
    }
  }

  @Override
  protected void onReTarget(int startOffset, int endOffset, int destOffset) {
    CodeEditorInlayModelBase inlayModel = myEditor.getInlayModel();
    inlayModel.myPutMergedIntervalsAtBeginning = intervalStart() == endOffset;
    if (DocumentUtil.isInsideSurrogatePair(getDocument(), getOffset())) {
      inlayModel.myMoveInProgress = true;
      try {
        invalidate("moved inside surrogate pair on retarget");
      }
      finally {
        inlayModel.myMoveInProgress = false;
      }
    }
  }

  @Override
  public void dispose() {
    if (isValid()) {
      int offset = getOffset();
      List<Inlay> inlays = myEditor.getInlayModel().getInlineElementsInRange(offset, offset);
      putUserData(ORDER_BEFORE_DISPOSAL, inlays.indexOf(this));
    }
    super.dispose();
  }

  @Override
  void doUpdateSize() {
    myWidthInPixels = myRenderer.calcWidthInPixels(this);
    if (myWidthInPixels <= 0) {
      throw new IllegalArgumentException("Positive width should be defined for an inline element");
    }
  }

  @Nonnull
  @Override
  public Placement getPlacement() {
    return Placement.INLINE;
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    int offset = getOffset();
    VisualPosition pos = myEditor.offsetToVisualPosition(offset);
    List<Inlay> inlays = myEditor.getInlayModel().getInlineElementsInRange(offset, offset);
    int order = inlays.indexOf(this);
    return new VisualPosition(pos.line, pos.column + order, true);
  }

  @Override
  Point getPosition() {
    VisualPosition pos = getVisualPosition();
    return myEditor.visualPositionToXY(pos);
  }

  @Override
  public int getHeightInPixels() {
    return myEditor.getLineHeight();
  }

  public int getOrder() {
    Integer value = getUserData(ORDER_BEFORE_DISPOSAL);
    return value == null ? -1 : value;
  }

  @Override
  public String toString() {
    return "[Inline inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", renderer=" + myRenderer + "]";
  }
}
