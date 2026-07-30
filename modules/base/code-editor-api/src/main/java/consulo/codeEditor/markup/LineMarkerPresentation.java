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

import org.jspecify.annotations.Nullable;

/**
 * A semantic gutter presentation: <em>what</em> is at these lines, never <em>how</em> it looks.
 * <p>
 * Implementations are defined by the feature that owns the concept — {@code VcsChangePresentation},
 * {@code CoveragePresentation}, {@code DiffChunkPresentation} — and are rendered by a painter looked
 * up by class. A presentation whose type has no painter registered is skipped.
 * <p>
 * Contains no shapes and no pixel values. Colours are allowed, since colour scheme keys are feature
 * knowledge.
 * <p>
 * Coordinates are <b>logical lines</b>, half-open {@code [startLine, endLine)}. The document line
 * count is a legal value; {@code startLine == endLine} denotes a line <em>boundary</em> rather than
 * an empty span, and is how deletion markers are expressed.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public interface LineMarkerPresentation {
    /**
     * First logical line, inclusive.
     */
    int startLine();

    /**
     * Logical line past the last one, exclusive. May equal {@link #startLine()} to denote a
     * boundary between lines, and may equal the document line count.
     */
    int endLine();

    /**
     * Gutter band this presentation occupies. Must be one of the areas its provider declares in
     * {@link LineMarkerPresentationProvider#getUsedAreas()}.
     */
    EditorGutterArea area();

    /**
     * Domain object handed back to the provider on click or tooltip, so hit-testing never needs to
     * re-derive which model element a position belongs to.
     */
    default @Nullable Object payload() {
        return null;
    }
}
