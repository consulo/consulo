// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.ex;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.IntUnaryOperator;

public abstract class EditorGutterComponentEx extends JComponent implements EditorGutter {
  /**
   * The key to retrieve a logical editor line position of a latest actionable click inside the gutter.
   * Available to gutter popup actions (see {@link #setGutterPopupGroup(ActionGroup)},
   * {@link GutterIconRenderer#getPopupMenuActions()}, {@link TextAnnotationGutterProvider#getPopupActions(int, Editor)})
   */
  public static final Key<Integer> LOGICAL_LINE_AT_CURSOR = Key.create("EditorGutter.LOGICAL_LINE_AT_CURSOR");

  /**
   * The key to retrieve a editor gutter icon center position of a latest actionable click inside the gutter.
   * Available to gutter popup actions (see {@link #setGutterPopupGroup(ActionGroup)},
   * {@link GutterIconRenderer#getPopupMenuActions()}, {@link TextAnnotationGutterProvider#getPopupActions(int, Editor)})
   */
  public static final Key<Point> ICON_CENTER_POSITION = Key.create("EditorGutter.ICON_CENTER_POSITION");

  @Nullable
  public abstract FoldRegion findFoldingAnchorAt(int x, int y);

  @Nonnull
  public abstract List<GutterMark> getGutterRenderers(int line);

  public abstract int getWhitespaceSeparatorOffset();

  public abstract void revalidateMarkup();

  public abstract int getLineMarkerAreaOffset();

  public abstract int getIconAreaOffset();

  public abstract int getLineMarkerFreePaintersAreaOffset();

  public abstract int getIconsAreaWidth();

  public abstract int getAnnotationsAreaOffset();

  public abstract int getAnnotationsAreaWidth();

  @Nullable
  public abstract Point getCenterPoint(GutterIconRenderer renderer);

  public abstract void setLineNumberConvertor(@Nullable IntUnaryOperator lineNumberConvertor);

  public abstract void setLineNumberConvertor(@Nullable IntUnaryOperator lineNumberConvertor1, @Nullable IntUnaryOperator lineNumberConvertor2);

  public abstract void setShowDefaultGutterPopup(boolean show);

  /**
   * When set to false, makes {@link #closeAllAnnotations()} a no-op and hides the corresponding context menu action.
   */
  public abstract void setCanCloseAnnotations(boolean canCloseAnnotations);

  public abstract void setGutterPopupGroup(@Nullable ActionGroup group);

  public abstract void setPaintBackground(boolean value);

  public abstract void setForceShowLeftFreePaintersArea(boolean value);

  public abstract void setForceShowRightFreePaintersArea(boolean value);

  public abstract void setInitialIconAreaWidth(int width);

  @Nullable
  public GutterMark getGutterRenderer(final Point p) {
    return null;
  }
}
