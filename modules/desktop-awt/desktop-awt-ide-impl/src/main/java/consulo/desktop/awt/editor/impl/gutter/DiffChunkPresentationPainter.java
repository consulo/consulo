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
import consulo.diff.util.DiffChunkPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class DiffChunkPresentationPainter implements AwtLineMarkerPresentationPainter<DiffChunkPresentation> {
    /**
     * Below this height a chunk has no room for an interior, so it collapses to one doubled edge.
     */
    private static final int MIN_INTERIOR_HEIGHT = 2;

    @Override
    public Class<DiffChunkPresentation> getPresentationType() {
        return DiffChunkPresentation.class;
    }

    @Override
    public void paint(DiffChunkPresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        if (bounds.width <= 0) {
            return;
        }

        int x1 = bounds.x;
        int x2 = bounds.x + bounds.width;
        int y1 = bounds.y;
        int y2 = bounds.y + bounds.height;

        if (bounds.height > MIN_INTERIOR_HEIGHT) {
            if (presentation.fillColor() != null) {
                g.setColor(TargetAWT.to(presentation.fillColor()));
                g.fillRect(x1, y1, x2 - x1, y2 - y1);
            }

            drawBorderLine(g, x1, x2, y1 - 1, presentation.borderColor(), false, presentation.dotted());
            drawBorderLine(g, x1, x2, y2 - 1, presentation.borderColor(), false, presentation.dotted());
        }
        else {
            // insertion or deletion point, or a range folded to nothing
            drawBorderLine(g, x1, x2, y1 - 1, presentation.borderColor(), true, presentation.dotted());
        }
    }

    static void drawBorderLine(
        Graphics2D g,
        int x1,
        int x2,
        int y,
        ColorValue colorValue,
        boolean doubleLine,
        boolean dottedLine
    ) {
        Color color = TargetAWT.to(colorValue);

        if (dottedLine && doubleLine) {
            UIUtil.drawBoldDottedLine(g, x1, x2, y - 1, null, color, false);
            UIUtil.drawBoldDottedLine(g, x1, x2, y, null, color, false);
        }
        else if (dottedLine) {
            UIUtil.drawBoldDottedLine(g, x1, x2, y - 1, null, color, false);
        }
        else if (doubleLine) {
            UIUtil.drawLine(g, x1, y, x2, y, null, color);
            UIUtil.drawLine(g, x1, y + 1, x2, y + 1, null, color);
        }
        else {
            UIUtil.drawLine(g, x1, y, x2, y, null, color);
        }
    }
}
