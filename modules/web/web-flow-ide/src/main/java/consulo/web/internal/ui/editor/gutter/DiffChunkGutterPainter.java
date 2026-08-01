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
package consulo.web.internal.ui.editor.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.diff.util.DiffChunkPresentation;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
@ExtensionImpl
public class DiffChunkGutterPainter implements WebLineMarkerPresentationPainter<DiffChunkPresentation> {
    @Override
    public Class<DiffChunkPresentation> getPresentationType() {
        return DiffChunkPresentation.class;
    }

    @Override
    public @Nullable GutterBand paint(DiffChunkPresentation presentation, Editor editor) {
        return new GutterBand(
            presentation.area().name(),
            presentation.startLine(),
            presentation.endLine(),
            WebGutterColors.toCss(presentation.fillColor()),
            WebGutterColors.toCss(presentation.borderColor()),
            presentation.dotted()
        );
    }
}
