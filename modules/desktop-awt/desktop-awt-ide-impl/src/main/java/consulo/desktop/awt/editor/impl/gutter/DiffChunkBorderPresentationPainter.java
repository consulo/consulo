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
import consulo.diff.util.DiffChunkBorderPresentation;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class DiffChunkBorderPresentationPainter implements AwtLineMarkerPresentationPainter<DiffChunkBorderPresentation> {
    @Override
    public Class<DiffChunkBorderPresentation> getPresentationType() {
        return DiffChunkBorderPresentation.class;
    }

    @Override
    public void paint(DiffChunkBorderPresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        if (bounds.width <= 0) {
            return;
        }

        DiffChunkPresentationPainter.drawBorderLine(
            g,
            bounds.x,
            bounds.x + bounds.width,
            bounds.y - 1,
            presentation.color(),
            presentation.doubleLine(),
            presentation.dotted()
        );
    }
}
