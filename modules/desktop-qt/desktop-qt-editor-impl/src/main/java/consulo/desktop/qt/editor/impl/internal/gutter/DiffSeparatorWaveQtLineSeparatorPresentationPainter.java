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
package consulo.desktop.qt.editor.impl.internal.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorWidget;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.diff.util.DiffSeparatorWavePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.util.ColorValueUtil;
import io.qt.core.QPoint;
import io.qt.gui.QPainter;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class DiffSeparatorWaveQtLineSeparatorPresentationPainter implements QtLineSeparatorPresentationPainter<DiffSeparatorWavePresentation> {
    @Override
    public Class<DiffSeparatorWavePresentation> getPresentationType() {
        return DiffSeparatorWavePresentation.class;
    }

    @Override
    public void paint(DiffSeparatorWavePresentation presentation, Editor editor, QPainter painter, QtLineSeparatorBounds bounds) {
        int lineHeight = editor.getLineHeight();
        int interval = getStepSize(lineHeight) * 2;

        int gutterWidth = 0;
        int contentWidth = 0;
        boolean mirrored = false;
        if (editor instanceof DesktopQtEditorImpl qtEditor) {
            mirrored = qtEditor.isMirrored();
            DesktopQtEditorWidget surface = qtEditor.getSurface();
            if (surface != null) {
                gutterWidth = surface.getGutter().width();
                contentWidth = surface.viewport().width();
            }
        }

        int shiftX = -interval;
        if (mirrored) {
            shiftX += contentWidth % interval - interval;
            shiftX += gutterWidth % interval - interval;
        }
        else {
            shiftX += -gutterWidth % interval - interval;
        }

        draw(painter, presentation.background(), presentation.topBorder(), presentation.bottomBorder(),
            shiftX, bounds.y() + 1, lineHeight, bounds.startX(), bounds.endX());
    }

    static void draw(
        QPainter painter,
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

        boolean antialiasing = painter.testRenderHint(QPainter.RenderHint.Antialiasing);
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, false);
        try {
            for (int row = 0; row < height; row++) {
                ColorValue color = getTopBorderColor(topBorder, background, row, lineHeight);
                if (color == null) {
                    color = getBottomBorderColor(bottomBorder, background, row, lineHeight);
                }
                if (color == null) {
                    color = background;
                }

                QPoint[] points = new QPoint[count];
                for (int index = 0; index < count; index++) {
                    points[index] = new QPoint(xPoints[index], yPoints[index] + row);
                }

                painter.setPen(TargetQt.to(color));
                painter.drawPolyline(points);
            }
        }
        finally {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, antialiasing);
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

    static int getStepSize(int lineHeight) {
        return Math.max(lineHeight / 3, 1);
    }

    static int getHeight(int lineHeight) {
        return Math.max(lineHeight / 2, 1);
    }
}
