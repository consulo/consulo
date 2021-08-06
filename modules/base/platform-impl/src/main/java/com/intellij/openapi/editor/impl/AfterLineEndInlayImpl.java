// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.util.DocumentUtil;
import consulo.editor.impl.CodeEditorBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.List;

public class AfterLineEndInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, AfterLineEndInlayImpl> {
  private static int ourGlobalCounter = 0;
  public final int myOrder;

  public AfterLineEndInlayImpl(@Nonnull CodeEditorBase editor, int offset, boolean relatesToPrecedingText, @Nonnull R renderer) {
    super(editor, offset, relatesToPrecedingText, renderer);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    myOrder = ourGlobalCounter++;
  }

  @Override
  RangeMarkerTree<AfterLineEndInlayImpl> getTree() {
    return myEditor.getInlayModel().myAfterLineEndElementsTree;
  }

  @Override
  void doUpdateSize() {
    myWidthInPixels = myRenderer.calcWidthInPixels(this);
    if (myWidthInPixels <= 0) {
      throw new IllegalArgumentException("Positive width should be defined for an after-line-end element");
    }
  }

  @Override
  Point getPosition() {
    VisualPosition pos = getVisualPosition();
    return myEditor.visualPositionToXY(pos);
  }

  @Nullable
  @Override
  public Rectangle getBounds() {
    int targetOffset = DocumentUtil.getLineEndOffset(getOffset(), myEditor.getDocument());
    if (myEditor.getFoldingModel().isOffsetCollapsed(targetOffset)) return null;
    Point pos = getPosition();
    return new Rectangle(pos.x, pos.y, getWidthInPixels(), getHeightInPixels());
  }

  @Nonnull
  @Override
  public Placement getPlacement() {
    return Placement.AFTER_LINE_END;
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    int offset = getOffset();
    int logicalLine = myEditor.getDocument().getLineNumber(offset);
    int lineEndOffset = myEditor.getDocument().getLineEndOffset(logicalLine);
    VisualPosition position = myEditor.offsetToVisualPosition(lineEndOffset, true, true);
    if (myEditor.getFoldingModel().isOffsetCollapsed(lineEndOffset)) return position;
    List<Inlay> inlays = myEditor.getInlayModel().getAfterLineEndElementsForLogicalLine(logicalLine);
    int order = inlays.indexOf(this);
    return new VisualPosition(position.line, position.column + 1 + order);
  }

  @Override
  public int getHeightInPixels() {
    return myEditor.getLineHeight();
  }

  @Override
  public String toString() {
    return "[After-line-end inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", renderer=" + myRenderer + "]";
  }
}
