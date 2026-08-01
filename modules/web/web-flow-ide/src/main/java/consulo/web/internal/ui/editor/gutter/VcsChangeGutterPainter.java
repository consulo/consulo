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
import consulo.versionControlSystem.internal.VcsChangePresentation;
import org.jspecify.annotations.Nullable;

/**
 * The vcs change bars. A deletion arrives with {@code startLine == endLine} and the browser marks the boundary
 * between the two surviving lines rather than trying to cover none.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@ExtensionImpl
public class VcsChangeGutterPainter implements WebLineMarkerPresentationPainter<VcsChangePresentation> {
    @Override
    public Class<VcsChangePresentation> getPresentationType() {
        return VcsChangePresentation.class;
    }

    @Override
    public @Nullable GutterBand paint(VcsChangePresentation presentation, Editor editor) {
        String color = WebColors.toCssColor(presentation.color());
        String borderColor = WebColors.toCssColor(presentation.borderColor());

        if (color == null && borderColor == null) {
            return null;
        }

        return new GutterBand(
            presentation.area().name(),
            presentation.startLine(),
            presentation.endLine(),
            color,
            borderColor,
            false
        );
    }
}
