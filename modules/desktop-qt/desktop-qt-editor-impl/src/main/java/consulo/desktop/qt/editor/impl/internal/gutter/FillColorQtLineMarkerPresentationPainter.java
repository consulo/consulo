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
import consulo.codeEditor.markup.FillColorLineMarkerPresentation;
import consulo.desktop.qt.ui.impl.TargetQt;
import io.qt.core.QRect;
import io.qt.gui.QPainter;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class FillColorQtLineMarkerPresentationPainter implements QtLineMarkerPresentationPainter<FillColorLineMarkerPresentation> {
    @Override
    public Class<FillColorLineMarkerPresentation> getPresentationType() {
        return FillColorLineMarkerPresentation.class;
    }

    @Override
    public void paint(FillColorLineMarkerPresentation presentation, Editor editor, QPainter painter, QRect bounds) {
        // a zero-height presentation is a line boundary, and a boundary has nothing to fill
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }
        painter.fillRect(bounds, TargetQt.to(presentation.color()));
    }
}
