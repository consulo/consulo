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
import consulo.codeEditor.markup.FillColorLineMarkerPresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

/**
 * AWT look for {@link FillColorLineMarkerPresentation}: the band it was given, filled.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
@ExtensionImpl
public class FillColorLineMarkerPresentationPainter implements AwtLineMarkerPresentationPainter<FillColorLineMarkerPresentation> {
    @Override
    public Class<FillColorLineMarkerPresentation> getPresentationType() {
        return FillColorLineMarkerPresentation.class;
    }

    @Override
    public void paint(FillColorLineMarkerPresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        // a zero-height presentation is a line boundary, and a boundary has nothing to fill
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        g.setColor(TargetAWT.to(presentation.color()));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
