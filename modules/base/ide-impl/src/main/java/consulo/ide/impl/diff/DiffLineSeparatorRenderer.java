/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.diff;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.diff.DiffColors;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.ide.impl.idea.openapi.editor.markup.LineMarkerRendererEx;
import consulo.codeEditor.markup.LineSeparatorRenderer;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.util.ColorValueUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Arrays;

public class DiffLineSeparatorRenderer implements LineMarkerRendererEx, LineSeparatorRenderer {
  @Nonnull
  private final Editor myEditor;
  @Nonnull
  private final BooleanGetter myCondition;

  public DiffLineSeparatorRenderer(@Nonnull Editor editor) {
    this(editor, BooleanGetter.TRUE);
  }

  public DiffLineSeparatorRenderer(@Nonnull Editor editor, @Nonnull BooleanGetter condition) {
    myEditor = editor;
    myCondition = condition;
  }

  public static void drawConnectorLine(@Nonnull Graphics2D g,
                                       int x1, int x2,
                                       int start1, int end1,
                                       int start2, int end2) {
    drawConnectorLine(g, x1, x2, start1, start2, end1 - end2, null);
  }

  /*
   * Divider
   */
  public static void drawConnectorLine(@Nonnull Graphics2D g,
                                       int x1, int x2,
                                       int y1, int y2,
                                       int lineHeight,
                                       @Nullable EditorColorsScheme scheme) {
    int step = getStepSize(lineHeight);
    int height = getHeight(lineHeight);
    int dx = getDeltaX(lineHeight);
    int dy = getDeltaY(lineHeight);

    int start1 = y1 + (lineHeight - height - step) / 2 + step / 2;
    int start2 = y2 + (lineHeight - height - step) / 2 + step / 2;
    int end1 = start1 + height - 1;
    int end2 = start2 + height - 1;

    int[] xPoints;
    int[] yPoints;

    if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
      if (y2 < y1) {
        xPoints = new int[]{x1, x2 - dx, x2, x2, x1 + dx, x1};
        yPoints = new int[]{start1, start2 + dy, start2, end2, end1 - dy, end1};
      }
      else {
        xPoints = new int[]{x1, x1 + dx, x2, x2, x2 - dx, x1};
        yPoints = new int[]{start1, start1 + dy, start2, end2, end2 - dy, end1};
      }
    }
    else {
      xPoints = new int[]{x1, x2, x2, x1};
      yPoints = new int[]{start1, start2, end2, end1};
    }

    paintConnectorLine(g, xPoints, yPoints, lineHeight, scheme);
  }

  /*
   * Gutter
   */
  @Override
  public void paint(Editor editor, Graphics g, Rectangle r) {
    if (!myCondition.get()) return;

    int y = r.y;
    int lineHeight = myEditor.getLineHeight();

    EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
    int annotationsOffset = gutter.getAnnotationsAreaOffset();
    int annotationsWidth = gutter.getAnnotationsAreaWidth();
    if (annotationsWidth != 0) {
      g.setColor(TargetAWT.to(editor.getColorsScheme().getColor(EditorColors.GUTTER_BACKGROUND)));
      g.fillRect(annotationsOffset, y, annotationsWidth, lineHeight);
    }

    draw(g, 0, y, lineHeight, myEditor.getColorsScheme());
  }

  /*
   * Editor
   */
  @Override
  public void drawLine(Graphics g, int x1, int x2, int y) {
    if (!myCondition.get()) return;

    y++; // we want y to be line's top position

    final int gutterWidth = ((EditorEx)myEditor).getGutterComponentEx().getComponent().getWidth();
    int lineHeight = myEditor.getLineHeight();
    int interval = getStepSize(lineHeight) * 2;

    int shiftX = -interval; // skip zero index painting
    if (DiffImplUtil.isMirrored(myEditor)) {
      int contentWidth = ((EditorEx)myEditor).getScrollPane().getViewport().getWidth();
      shiftX += contentWidth % interval - interval;
      shiftX += gutterWidth % interval - interval;
    }
    else {
      shiftX += -gutterWidth % interval - interval;
    }

    draw(g, shiftX, y, lineHeight, myEditor.getColorsScheme());
  }

  @Nonnull
  @Override
  public LineMarkerRendererEx.Position getPosition() {
    return LineMarkerRendererEx.Position.CUSTOM;
  }

  private static void draw(@Nonnull Graphics g,
                           int shiftX,
                           int shiftY,
                           int lineHeight,
                           @Nullable EditorColorsScheme scheme) {
    int step = getStepSize(lineHeight);
    int height = getHeight(lineHeight);

    Rectangle clip = g.getClipBounds();
    if (clip.width <= 0) return;
    int count = (clip.width / step + 3);
    int shift = (clip.x - shiftX) / step;

    int[] xPoints = new int[count];
    int[] yPoints = new int[count];

    shiftY += (lineHeight - height - step) / 2;

    for (int index = 0; index < count; index++) {
      int absIndex = index + shift;

      int xPos = absIndex * step + shiftX;
      int yPos;

      if (absIndex == 0) {
        yPos = step / 2 + shiftY;
      }
      else if (absIndex % 2 == 0) {
        yPos = shiftY;
      }
      else {
        yPos = step + shiftY;
      }

      xPoints[index] = xPos;
      yPoints[index] = yPos;
    }

    GraphicsConfig config = GraphicsUtil.disableAAPainting(g);
    try {
      paintLine(g, xPoints, yPoints, lineHeight, scheme);
    }
    finally {
      config.restore();
    }
  }

  private static void paintLine(@Nonnull Graphics g,
                                @Nonnull int[] xPoints, @Nonnull int[] yPoints,
                                int lineHeight,
                                @Nullable EditorColorsScheme scheme) {
    int height = getHeight(lineHeight);
    if (scheme == null) scheme = EditorColorsManager.getInstance().getGlobalScheme();

    Graphics2D gg = ((Graphics2D)g);
    AffineTransform oldTransform = gg.getTransform();

    for (int i = 0; i < height; i++) {
      ColorValue color = getTopBorderColor(i, lineHeight, scheme);
      if (color == null) color = getBottomBorderColor(i, lineHeight, scheme);
      if (color == null) color = getBackgroundColor(scheme);

      gg.setColor(TargetAWT.to(color));
      gg.drawPolyline(xPoints, yPoints, xPoints.length);
      gg.translate(0, 1);
    }
    gg.setTransform(oldTransform);
  }

  private static void paintConnectorLine(@Nonnull Graphics g,
                                         @Nonnull int[] xPoints, @Nonnull int[] yPoints,
                                         int lineHeight,
                                         @Nullable EditorColorsScheme scheme) {
    // TODO: shadow looks bad with big lineHeight and slope
    int height = getHeight(lineHeight);
    if (scheme == null) scheme = EditorColorsManager.getInstance().getGlobalScheme();

    Graphics2D gg = ((Graphics2D)g);
    AffineTransform oldTransform = gg.getTransform();

    // background
    gg.setColor(TargetAWT.to(getBackgroundColor(scheme)));
    gg.fillPolygon(xPoints, yPoints, xPoints.length);

    if (scheme.getColor(TOP_BORDER) != null) {
      for (int i = 0; i < height; i++) {
        ColorValue color = getTopBorderColor(i, lineHeight, scheme);
        if (color == null) break;

        gg.setColor(TargetAWT.to(color));
        gg.drawPolyline(xPoints, yPoints, xPoints.length / 2);
        gg.translate(0, 1);
      }
      gg.setTransform(oldTransform);
    }

    if (scheme.getColor(BOTTOM_BORDER) != null) {
      int[] xBottomPoints = Arrays.copyOfRange(xPoints, xPoints.length / 2, xPoints.length);
      int[] yBottomPoints = Arrays.copyOfRange(yPoints, xPoints.length / 2, xPoints.length);

      for (int i = height - 1; i >= 0; i--) {
        ColorValue color = getBottomBorderColor(i, lineHeight, scheme);
        if (color == null) break;

        gg.setColor(TargetAWT.to(color));
        gg.drawPolyline(xBottomPoints, yBottomPoints, xPoints.length / 2);
        gg.translate(0, -1);
      }
      gg.setTransform(oldTransform);
    }
  }

  //
  // Parameters
  //

  public static final EditorColorKey BACKGROUND = DiffColors.DIFF_SEPARATORS_BACKGROUND;
  private static final EditorColorKey TOP_BORDER = DiffColors.DIFF_SEPARATORS_TOP_BORDER;
  private static final EditorColorKey BOTTOM_BORDER = DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER;

  private static int getStepSize(int lineHeight) {
    return Math.max(lineHeight / 3, 1);
  }

  private static int getHeight(int lineHeight) {
    return Math.max(lineHeight / 2, 1);
  }

  private static int getDeltaX(int lineHeight) {
    return Math.max(lineHeight / 4, 1);
  }

  private static int getDeltaY(int lineHeight) {
    return Math.max(lineHeight / 6, 1);
  }

  @Nonnull
  private static ColorValue getBackgroundColor(@Nonnull EditorColorsScheme scheme) {
    ColorValue color = scheme.getColor(BACKGROUND);
    return color != null ? color : new RGBColor(128, 128, 128);
  }

  @Nullable
  private static ColorValue getTopBorderColor(int i, int lineHeight, @Nonnull EditorColorsScheme scheme) {
    int border = Math.max(lineHeight / 4, 1);
    double ratio = (double)i / border;
    if (ratio > 1) return null;

    ColorValue top = scheme.getColor(TOP_BORDER);
    if (top == null) return null;

    ColorValue background = getBackgroundColor(scheme);
    return ColorValueUtil.mix(top, background, ratio);
  }

  @Nullable
  private static ColorValue getBottomBorderColor(int i, int lineHeight, @Nonnull EditorColorsScheme scheme) {
    int height = getHeight(lineHeight);
    int border = Math.max(lineHeight / 12, 1);

    int index = (height - i - 1);
    double ratio = (double)index / border;
    if (ratio > 1) return null;

    ColorValue bottom = scheme.getColor(BOTTOM_BORDER);
    if (bottom == null) return null;

    ColorValue background = getBackgroundColor(scheme);
    return ColorValueUtil.mix(bottom, background, ratio);
  }
}
