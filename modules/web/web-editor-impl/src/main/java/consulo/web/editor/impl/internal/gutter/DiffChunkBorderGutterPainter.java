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
package consulo.web.editor.impl.internal.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.diff.util.DiffChunkBorderPresentation;
import consulo.web.ui.impl.internal.WebColors;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class DiffChunkBorderGutterPainter implements WebLineMarkerPresentationPainter<DiffChunkBorderPresentation> {
    @Override
    public Class<DiffChunkBorderPresentation> getPresentationType() {
        return DiffChunkBorderPresentation.class;
    }

    @Override
    public @Nullable GutterBand paint(DiffChunkBorderPresentation presentation, Editor editor) {
        String color = WebColors.toCssColor(presentation.color());
        if (color == null) {
            return null;
        }
        return GutterBand.line(presentation.area(), presentation.line(), color, presentation.doubleLine(), presentation.dotted());
    }
}
