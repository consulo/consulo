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
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.diff.util.DiffSeparatorGutterPresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

/**
 * Continues the separator band across the gutter, so it lines up with the one drawn over the
 * editor content, and clears the annotations area behind it.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class DiffSeparatorGutterPresentationPainter
    implements AwtLineMarkerPresentationPainter<DiffSeparatorGutterPresentation> {

    @Override
    public Class<DiffSeparatorGutterPresentation> getPresentationType() {
        return DiffSeparatorGutterPresentation.class;
    }

    @Override
    public void paint(DiffSeparatorGutterPresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        int lineHeight = editor.getLineHeight();

        if (presentation.annotationsBackground() != null) {
            EditorGutterComponentEx gutter = ((EditorEx) editor).getGutterComponentEx();
            int annotationsWidth = gutter.getAnnotationsAreaWidth();
            if (annotationsWidth != 0) {
                g.setColor(TargetAWT.to(presentation.annotationsBackground()));
                g.fillRect(gutter.getAnnotationsAreaOffset(), bounds.y, annotationsWidth, lineHeight);
            }
        }

        // shiftX 0: the gutter band starts at the gutter's own left edge, and the content painter
        // offsets its band by the gutter width so the two form one continuous wave
        DiffSeparatorWavePresentationPainter.draw(
            g,
            presentation.background(),
            presentation.topBorder(),
            presentation.bottomBorder(),
            0,
            bounds.y,
            lineHeight,
            bounds.x,
            bounds.x + bounds.width
        );
    }
}
