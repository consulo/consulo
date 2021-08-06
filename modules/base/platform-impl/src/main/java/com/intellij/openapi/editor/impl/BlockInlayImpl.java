// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.VisualPosition;
import consulo.editor.impl.CodeEditorBase;

import javax.annotation.Nonnull;

import java.awt.*;
import java.util.List;
import java.util.function.IntSupplier;

public class BlockInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, BlockInlayImpl> implements IntSupplier {
  public final boolean myShowAbove;
  public final int myPriority;
  private int myHeightInPixels;

  public BlockInlayImpl(@Nonnull CodeEditorBase editor, int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, @Nonnull R renderer) {
    super(editor, offset, relatesToPrecedingText, renderer);
    myShowAbove = showAbove;
    myPriority = priority;
  }

  @Override
  MarkerTreeWithPartialSums<BlockInlayImpl> getTree() {
    return myEditor.getInlayModel().myBlockElementsTree;
  }

  @Override
  void doUpdateSize() {
    myWidthInPixels = myRenderer.calcWidthInPixels(this);
    if (myWidthInPixels < 0) {
      throw new IllegalArgumentException("Non-negative width should be defined for a block element");
    }
    int oldHeightInPixels = myHeightInPixels;
    myHeightInPixels = myRenderer.calcHeightInPixels(this);
    if (oldHeightInPixels != myHeightInPixels) getTree().valueUpdated(this);
    if (myHeightInPixels < 0) {
      throw new IllegalArgumentException("Non-negative height should be defined for a block element");
    }

  }

  @Override
  Point getPosition() {
    int visualLine = myEditor.offsetToVisualLine(getOffset());
    int y = myEditor.visualLineToY(visualLine);
    List<Inlay> allInlays = myEditor.getInlayModel().getBlockElementsForVisualLine(visualLine, myShowAbove);
    if (myShowAbove) {
      boolean found = false;
      for (Inlay inlay : allInlays) {
        if (inlay == this) found = true;
        if (found) y -= inlay.getHeightInPixels();
      }
    }
    else {
      y += myEditor.getLineHeight();
      for (Inlay inlay : allInlays) {
        if (inlay == this) break;
        y += inlay.getHeightInPixels();
      }
    }
    return new Point(myEditor.getContentComponent().getInsets().left, y);
  }

  @Override
  public int getHeightInPixels() {
    return myHeightInPixels;
  }

  @Nonnull
  @Override
  public Placement getPlacement() {
    return myShowAbove ? Placement.ABOVE_LINE : Placement.BELOW_LINE;
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    return myEditor.offsetToVisualPosition(getOffset());
  }

  @Override
  public int getAsInt() {
    return myHeightInPixels;
  }

  @Override
  public String toString() {
    return "[Block inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", height=" + myHeightInPixels + ", renderer=" + myRenderer + "]";
  }
}
