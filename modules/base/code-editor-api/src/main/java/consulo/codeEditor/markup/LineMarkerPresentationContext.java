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
package consulo.codeEditor.markup;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * Everything a {@link LineMarkerPresentationProvider} may consult while building presentations.
 * <p>
 * Exposes no pixel metrics and no line-to-y conversion, on purpose: line-to-y goes through the view
 * model (folding and soft wraps), so it stays on the rendering side and providers cannot reach it
 * from the paint path.
 * <p>
 * Colour lookup is narrowed to key resolution rather than exposing the whole
 * {@link consulo.colorScheme.EditorColorsScheme}, so providers stay trivially fakeable in tests.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public interface LineMarkerPresentationContext {
    /**
     * First logical line of the provider's own highlighter. Removes the need for a provider to hold
     * a {@link RangeHighlighter} just to convert its offsets to lines.
     */
    int startLine();

    /**
     * Logical line past the last one of the provider's own highlighter.
     */
    int endLine();

    /**
     * First logical line currently visible, for clipping. Presentations outside the visible range may be
     * skipped.
     */
    int firstVisibleLine();

    /**
     * Logical line past the last visible one.
     */
    int lastVisibleLine();

    /**
     * Document line count. A presentation may legally end at this value.
     */
    int lineCount();

    /**
     * Logical line the mouse currently hovers in the gutter, or {@code -1}.
     */
    int hoveredLine();

    boolean isLineNumbersShown();

    boolean isAnnotationsShown();

    boolean isMirrored();

    @Nullable
    ColorValue getColor(EditorColorKey key);

    /**
     * The editor's background colour, for presentations that blend against it.
     */
    ColorValue getEditorBackgroundColor();

    TextAttributes getAttributes(TextAttributesKey key);
}
