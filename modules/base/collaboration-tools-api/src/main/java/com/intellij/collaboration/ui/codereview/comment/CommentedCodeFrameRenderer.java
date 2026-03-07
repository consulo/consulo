// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorUtilKt;
import com.intellij.collaboration.ui.util.CodeReviewColorUtil;
import com.intellij.diff.util.Side;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.CustomHighlighterOrder;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.ui.scale.JBUIScale;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.geom.Path2D;
import kotlin.Pair;

final class CommentedCodeFrameRenderer implements CustomHighlighterRenderer, LineMarkerRenderer {
  private static final float OUTER_GUTTER_FRAME_PADDING = 2f;
  private static final Color color = CodeReviewColorUtil.Review.LineFrame.border;

  private final int startLine;
  private final int endLine;
  private final @Nullable Side editorSide;

  CommentedCodeFrameRenderer(int startLine, int endLine, @Nullable Side editorSide) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.editorSide = editorSide;
  }

  @Override
  public @Nonnull CustomHighlighterOrder getOrder() {
    return CustomHighlighterOrder.AFTER_TEXT;
  }

  @Override
  public void paint(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter, @Nonnull Graphics g) {
    // editor part
    float x = 0f;
    float width = editor.getContentComponent().getWidth();
    float[] yValues = getYAxisValues(editor);
    float y = yValues[0];
    float height = yValues[1];

    if (editorSide == Side.LEFT) {
      x += getScrollbarPadding();
      Path2D.Float path = createLeftOutlinePath(x, y, width, height);
      drawOutlinePath(g, path);
    }
    else {
      width -= getScrollbarPadding();
      Path2D.Float path = createRightOutlinePath(x, y, width, height);
      drawOutlinePath(g, path);
    }
  }

  @Override
  public void paint(@Nonnull Editor editor, @Nonnull Graphics g, @Nonnull Rectangle r) {
    // gutter part
    float x = OUTER_GUTTER_FRAME_PADDING;
    float width = editor instanceof EditorEx editorEx ? editorEx.getGutterComponentEx().getWidth() : 0f;
    float[] yValues = getYAxisValues(editor);
    float y = yValues[0];
    float height = yValues[1];
    Path2D.Float path = createLeftOutlinePath(x, y, width, height);
    drawOutlinePath(g, path);
  }

  private void drawOutlinePath(@Nonnull Graphics g, @Nonnull Path2D.Float path) {
    Graphics2D g2d = g.create() instanceof Graphics2D g2 ? g2 : null;
    if (g2d == null) return;
    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setColor(color);
      g2d.setStroke(getStroke());
      g2d.draw(path);
    }
    finally {
      g2d.dispose();
    }
  }

  private float @Nonnull [] getYAxisValues(@Nonnull Editor editor) {
    Pair<Integer, Integer> yRange = CodeReviewEditorUtilKt.yRangeForLogicalLineRange(editor, startLine, endLine);
    float startY = yRange.getFirst();
    float height = yRange.getSecond() - startY;
    return new float[]{startY, height};
  }

  private @Nonnull Path2D.Float createLeftOutlinePath(float x, float y, float width, float height) {
    int r = getRadius();
    Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
    path.moveTo(width, y);
    path.lineTo(x + r, y);
    path.quadTo(x, y, x, y + r);
    path.lineTo(x, y + height - r);
    path.quadTo(x, y + height, x + r, y + height);
    path.lineTo(width, y + height);
    return path;
  }

  private @Nonnull Path2D.Float createRightOutlinePath(float x, float y, float width, float height) {
    int r = getRadius();
    Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
    path.moveTo(x, y);
    path.lineTo(x + width - r, y);
    path.quadTo(x + width, y, x + width, y + r);
    path.lineTo(x + width, y + height - r);
    path.quadTo(x + width, y + height, x + width - r, y + height);
    path.lineTo(x, y + height);
    return path;
  }

  private static @Nonnull Stroke getStroke() {
    return new BasicStroke(JBUIScale.scale(1f));
  }

  private static int getRadius() {
    return JBUIScale.scale(4);
  }

  private static int getScrollbarPadding() {
    return JBUIScale.scale(15);
  }
}
