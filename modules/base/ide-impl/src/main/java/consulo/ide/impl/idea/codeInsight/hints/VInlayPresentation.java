/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;

import java.awt.*;

/**
 * Renderer for visual formatting inlay placeholders.
 */
public class VInlayPresentation implements EditorCustomElementRenderer {
    private final FontMetrics editorFontMetrics;
    final boolean vertical;
    private final int fillerLength;

    public VInlayPresentation(Editor editor, int fillerLength, boolean vertical) {
        this.fillerLength = fillerLength;
        this.vertical = vertical;
        var font = EditorColorsManager.getInstance()
            .getGlobalScheme()
            .getFont(EditorFontType.PLAIN);
        Component component = editor.getContentComponent();
        this.editorFontMetrics = component.getFontMetrics(font);
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay) {
        return vertical ? 0 : editorFontMetrics.stringWidth(" ".repeat(fillerLength));
    }

    @Override
    public int calcHeightInPixels(Inlay<?> inlay) {
        return (vertical ? fillerLength : 1) * editorFontMetrics.getHeight();
    }
}
