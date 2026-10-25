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
package consulo.desktop.awt.editor.impl.internal.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.InlayFillColorLineMarkerPresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

@ExtensionImpl
public class InlayFillColorLineMarkerPresentationPainter implements AwtLineMarkerPresentationPainter<InlayFillColorLineMarkerPresentation> {
    @Override
    public Class<InlayFillColorLineMarkerPresentation> getPresentationType() {
        return InlayFillColorLineMarkerPresentation.class;
    }

    @Override
    public void paint(InlayFillColorLineMarkerPresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        Rectangle band = presentation.getBandRelativeToLine(editor);
        if (band == null || bounds.width <= 0) {
            return;
        }
        g.setColor(TargetAWT.to(presentation.color()));
        g.fillRect(bounds.x, bounds.y + band.y, bounds.width, band.height);
    }
}
