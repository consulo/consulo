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

import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.diff.util.DiffSeparatorLinePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class DiffSeparatorLinePresentationPainter implements AwtLineSeparatorPresentationPainter<DiffSeparatorLinePresentation> {
    @Override
    public Class<DiffSeparatorLinePresentation> getPresentationType() {
        return DiffSeparatorLinePresentation.class;
    }

    @Override
    public void paint(DiffSeparatorLinePresentation presentation, Editor editor, Graphics2D g, AwtLineSeparatorBounds bounds) {
        // a TOP separator sits on the line's own top edge rather than the boundary above it
        int y = bounds.placement() == SeparatorPlacement.TOP ? bounds.y() + 1 : bounds.y();

        ColorValue color = presentation.color();
        if (color == null) {
            g.setColor(JBColor.border());
            g.drawLine(bounds.startX(), y, bounds.endX(), y);
            return;
        }

        DiffChunkPresentationPainter.drawBorderLine(
            g,
            bounds.startX(),
            bounds.endX(),
            y,
            color,
            presentation.doubleLine(),
            presentation.dotted()
        );
    }
}
