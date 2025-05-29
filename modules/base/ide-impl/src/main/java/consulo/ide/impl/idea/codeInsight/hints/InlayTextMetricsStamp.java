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

import java.awt.font.FontRenderContext;
import java.util.Objects;

final class InlayTextMetricsStamp {
    private final float editorFontSize2D;
    private final String familyName;
    private final float ideScale;
    private final FontRenderContext fontRenderContext;

    InlayTextMetricsStamp(float editorFontSize2D,
                          String familyName,
                          float ideScale,
                          FontRenderContext fontRenderContext) {
        this.editorFontSize2D = editorFontSize2D;
        this.familyName = familyName;
        this.ideScale = ideScale;
        this.fontRenderContext = fontRenderContext;
    }

    public float getEditorFontSize2D() {
        return editorFontSize2D;
    }

    public String getFamilyName() {
        return familyName;
    }

    public float getIdeScale() {
        return ideScale;
    }

    public FontRenderContext getFontRenderContext() {
        return fontRenderContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InlayTextMetricsStamp)) return false;
        InlayTextMetricsStamp that = (InlayTextMetricsStamp) o;
        return Float.compare(that.editorFontSize2D, editorFontSize2D) == 0 &&
            Float.compare(that.ideScale, ideScale) == 0 &&
            Objects.equals(familyName, that.familyName) &&
            Objects.equals(fontRenderContext, that.fontRenderContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(editorFontSize2D, familyName, ideScale, fontRenderContext);
    }

    @Override
    public String toString() {
        return "InlayTextMetricsStamp{" +
            "editorFontSize2D=" + editorFontSize2D +
            ", familyName='" + familyName + '\'' +
            ", ideScale=" + ideScale +
            ", fontRenderContext=" + fontRenderContext +
            '}';
    }
}
