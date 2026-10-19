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
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.diff.util.DiffSeparatorLinePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.style.ComponentColors;
import io.qt.gui.QPainter;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class DiffSeparatorLineQtLineSeparatorPresentationPainter implements QtLineSeparatorPresentationPainter<DiffSeparatorLinePresentation> {
    @Override
    public Class<DiffSeparatorLinePresentation> getPresentationType() {
        return DiffSeparatorLinePresentation.class;
    }

    @Override
    public void paint(DiffSeparatorLinePresentation presentation, Editor editor, QPainter painter, QtLineSeparatorBounds bounds) {
        int y = bounds.placement() == SeparatorPlacement.TOP ? bounds.y() + 1 : bounds.y();

        ColorValue color = presentation.color();
        if (color == null) {
            painter.fillRect(bounds.startX(), y, bounds.endX() - bounds.startX(), 1, TargetQt.to(ComponentColors.BORDER));
            return;
        }

        DiffChunkQtLineMarkerPresentationPainter.drawBorderLine(
            painter,
            bounds.startX(),
            bounds.endX(),
            y,
            color,
            presentation.doubleLine(),
            presentation.dotted()
        );
    }
}
