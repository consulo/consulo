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
import consulo.web.internal.ui.WebColors;
import consulo.execution.coverage.CoveragePresentation;
import org.jspecify.annotations.Nullable;

/**
 * Coverage marks the strip left of the icons. The icon a presentation may carry is not drawn yet - the strip
 * is a few pixels wide and the awt gutter only shows it on hover, which the browser does not report.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@ExtensionImpl
public class CoverageGutterPainter implements WebLineMarkerPresentationPainter<CoveragePresentation> {
    @Override
    public Class<CoveragePresentation> getPresentationType() {
        return CoveragePresentation.class;
    }

    @Override
    public @Nullable GutterBand paint(CoveragePresentation presentation, Editor editor) {
        String color = WebColors.toCssColor(presentation.color());

        return color == null
            ? null
            : GutterBand.fill(presentation.area(), presentation.startLine(), presentation.endLine(), color);
    }
}
