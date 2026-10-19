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
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.diff.util.DiffChunkPresentation;
import consulo.ui.color.ColorValue;
import io.qt.core.QRect;
import io.qt.gui.QColor;
import io.qt.gui.QPainter;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class DiffChunkQtLineMarkerPresentationPainter implements QtLineMarkerPresentationPainter<DiffChunkPresentation> {
    private static final int MIN_INTERIOR_HEIGHT = 2;

    private static final int DOT_STEP = 4;

    @Override
    public Class<DiffChunkPresentation> getPresentationType() {
        return DiffChunkPresentation.class;
    }

    @Override
    public void paint(DiffChunkPresentation presentation, Editor editor, QPainter painter, QRect bounds) {
        if (bounds.width() <= 0) {
            return;
        }

        int x1 = bounds.x();
        int x2 = bounds.x() + bounds.width();
        int y1 = bounds.y();
        int y2 = bounds.y() + bounds.height();

        if (bounds.height() > MIN_INTERIOR_HEIGHT) {
            if (presentation.fillColor() != null) {
                painter.fillRect(x1, y1, x2 - x1, y2 - y1, TargetQt.to(presentation.fillColor()));
            }

            drawBorderLine(painter, x1, x2, y1 - 1, presentation.borderColor(), false, presentation.dotted());
            drawBorderLine(painter, x1, x2, y2 - 1, presentation.borderColor(), false, presentation.dotted());
        }
        else {
            drawBorderLine(painter, x1, x2, y1 - 1, presentation.borderColor(), true, presentation.dotted());
        }
    }

    static void drawBorderLine(
        QPainter painter,
        int x1,
        int x2,
        int y,
        ColorValue colorValue,
        boolean doubleLine,
        boolean dottedLine
    ) {
        QColor color = TargetQt.to(colorValue);

        if (dottedLine && doubleLine) {
            drawBoldDottedLine(painter, x1, x2, y - 1, color);
            drawBoldDottedLine(painter, x1, x2, y, color);
        }
        else if (dottedLine) {
            drawBoldDottedLine(painter, x1, x2, y - 1, color);
        }
        else if (doubleLine) {
            painter.fillRect(x1, y, x2 - x1, 2, color);
        }
        else {
            painter.fillRect(x1, y, x2 - x1, 1, color);
        }
    }

    private static void drawBoldDottedLine(QPainter painter, int startX, int endX, int lineY, QColor color) {
        int startPosCorrection = startX % DOT_STEP < 3 ? 0 : 1;
        for (int x = (startX / DOT_STEP + startPosCorrection) * DOT_STEP; x < endX; x += DOT_STEP) {
            painter.fillRect(x, lineY, 2, 2, color);
        }
    }
}
