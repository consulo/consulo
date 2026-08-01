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
import consulo.codeEditor.markup.FillColorLineMarkerPresentation;
import org.jspecify.annotations.Nullable;

/**
 * Every editor ships one of these - a run of lines in a colour is the one presentation a provider may emit
 * without supplying a look of its own.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@ExtensionImpl
public class FillColorGutterPainter implements WebLineMarkerPresentationPainter<FillColorLineMarkerPresentation> {
    @Override
    public Class<FillColorLineMarkerPresentation> getPresentationType() {
        return FillColorLineMarkerPresentation.class;
    }

    @Override
    public @Nullable GutterBand paint(FillColorLineMarkerPresentation presentation, Editor editor) {
        String color = WebGutterColors.toCss(presentation.color());

        return color == null
            ? null
            : GutterBand.fill(presentation.area(), presentation.startLine(), presentation.endLine(), color);
    }
}
