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
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.desktop.awt.editor.impl.gutter.AwtLineMarkerPresentationPainter;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class PatchChangePresentationPainter implements AwtLineMarkerPresentationPainter<PatchChangePresentation> {
    @Override
    public Class<PatchChangePresentation> getPresentationType() {
        return PatchChangePresentation.class;
    }

    @Override
    public void paint(PatchChangePresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        int x1 = bounds.x;
        int x2 = bounds.x + bounds.width;

        if (bounds.height != 0) {
            paint(g, presentation.color(), presentation.borderColor(), x1, bounds.y, x2, bounds.y + bounds.height);
        }
        else {
            // the hunk removed lines, so it sits on the boundary between the surviving ones
            int size = 5;
            int y = Math.max(bounds.y, size);
            paint(g, presentation.color(), presentation.borderColor(), x1, y - size, x2, y + size);
        }
    }

    private static void paint(
        Graphics2D g,
        ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int y1,
        int x2,
        int y2
    ) {
        double width = x2 - x1;
        g.setColor(TargetAWT.to(color));
        RectanglePainter2D.FILL.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);

        if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            RectanglePainter2D.DRAW.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);
        }
    }
}
