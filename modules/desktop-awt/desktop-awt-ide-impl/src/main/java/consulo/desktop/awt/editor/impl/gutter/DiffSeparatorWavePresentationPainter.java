/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.editor.impl.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;

import consulo.diff.util.DiffSeparatorWavePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Paints the diff separator as a zigzag band: one polyline redrawn once per pixel row, shifted a
 * pixel each time, with the row colour interpolated from the border colours into the background.
 * <p>
 * All of the arithmetic here is a function of the editor's line height and the visible clip, which
 * is why it belongs on this side rather than in the presentation.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class DiffSeparatorWavePresentationPainter implements AwtLineSeparatorPresentationPainter<DiffSeparatorWavePresentation> {
    @Override
    public Class<DiffSeparatorWavePresentation> getPresentationType() {
        return DiffSeparatorWavePresentation.class;
    }

    @Override
    public void paint(DiffSeparatorWavePresentation presentation, Editor editor, Graphics2D g, AwtLineSeparatorBounds bounds) {
        int lineHeight = editor.getLineHeight();
        int step = getStepSize(lineHeight);
        int interval = step * 2;

        int gutterWidth = ((EditorEx) editor).getGutterComponentEx().getComponent().getWidth();

        // skip the zero index, and align the wave so it does not shift as the editor scrolls
        int shiftX = -interval;
        if (isMirrored(editor)) {
            int contentWidth = ((EditorEx) editor).getScrollPane().getViewport().getWidth();
            shiftX += contentWidth % interval - interval;
            shiftX += gutterWidth % interval - interval;
        }
        else {
            shiftX += -gutterWidth % interval - interval;
        }

        // y points at the boundary; the band hangs from the top of the following line
        draw(g, presentation.background(), presentation.topBorder(), presentation.bottomBorder(),
            shiftX, bounds.y() + 1, lineHeight, bounds.startX(), bounds.endX());
    }

    static void draw(
        Graphics2D g,
        ColorValue background,
        @Nullable ColorValue topBorder,
        @Nullable ColorValue bottomBorder,
        int shiftX,
        int shiftY,
        int lineHeight,
        int startX,
        int endX
    ) {
        int step = getStepSize(lineHeight);
        int height = getHeight(lineHeight);

        int width = endX - startX;
        if (width <= 0) {
            return;
        }

        int count = width / step + 3;
        int shift = (startX - shiftX) / step;
        int baseY = shiftY + (lineHeight - height - step) / 2;

        int[] xPoints = new int[count];
        int[] yPoints = new int[count];
        for (int index = 0; index < count; index++) {
            int absIndex = index + shift;
            xPoints[index] = absIndex * step + shiftX;
            yPoints[index] = absIndex == 0 ? step / 2 + baseY : absIndex % 2 == 0 ? baseY : step + baseY;
        }

        GraphicsConfig config = GraphicsUtil.disableAAPainting(g);
        AffineTransform oldTransform = g.getTransform();
        try {
            for (int i = 0; i < height; i++) {
                ColorValue color = getTopBorderColor(topBorder, background, i, lineHeight);
                if (color == null) {
                    color = getBottomBorderColor(bottomBorder, background, i, lineHeight);
                }
                if (color == null) {
                    color = background;
                }

                g.setColor(TargetAWT.to(color));
                g.drawPolyline(xPoints, yPoints, xPoints.length);
                g.translate(0, 1);
            }
        }
        finally {
            g.setTransform(oldTransform);
            config.restore();
        }
    }

    private static @Nullable ColorValue getTopBorderColor(
        @Nullable ColorValue topBorder,
        ColorValue background,
        int row,
        int lineHeight
    ) {
        if (topBorder == null) {
            return null;
        }

        int border = Math.max(lineHeight / 4, 1);
        double ratio = (double) row / border;
        return ratio > 1 ? null : ColorValueUtil.mix(topBorder, background, ratio);
    }

    private static @Nullable ColorValue getBottomBorderColor(
        @Nullable ColorValue bottomBorder,
        ColorValue background,
        int row,
        int lineHeight
    ) {
        if (bottomBorder == null) {
            return null;
        }

        int border = Math.max(lineHeight / 12, 1);
        double ratio = (double) (getHeight(lineHeight) - row - 1) / border;
        return ratio > 1 ? null : ColorValueUtil.mix(bottomBorder, background, ratio);
    }

    private static boolean isMirrored(Editor editor) {
        return editor instanceof EditorEx editorEx && editorEx.getVerticalScrollbarOrientation() != EditorEx.VERTICAL_SCROLLBAR_RIGHT;
    }

    static int getStepSize(int lineHeight) {
        return Math.max(lineHeight / 3, 1);
    }

    static int getHeight(int lineHeight) {
        return Math.max(lineHeight / 2, 1);
    }
}
