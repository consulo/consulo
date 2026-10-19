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
import consulo.diff.util.DiffSeparatorGutterPresentation;
import io.qt.core.QRect;
import io.qt.gui.QPainter;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class DiffSeparatorGutterQtLineMarkerPresentationPainter implements QtLineMarkerPresentationPainter<DiffSeparatorGutterPresentation> {
    @Override
    public Class<DiffSeparatorGutterPresentation> getPresentationType() {
        return DiffSeparatorGutterPresentation.class;
    }

    @Override
    public void paint(DiffSeparatorGutterPresentation presentation, Editor editor, QPainter painter, QRect bounds) {
        DiffSeparatorWaveQtLineSeparatorPresentationPainter.draw(
            painter,
            presentation.background(),
            presentation.topBorder(),
            presentation.bottomBorder(),
            0,
            bounds.y(),
            editor.getLineHeight(),
            bounds.x(),
            bounds.x() + bounds.width()
        );
    }
}
