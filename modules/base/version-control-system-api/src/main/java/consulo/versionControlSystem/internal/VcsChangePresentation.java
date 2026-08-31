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
package consulo.versionControlSystem.internal;

import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.EditorGutterArea;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * A VCS changed-line gutter presentation: it states that these lines were inserted, modified or deleted.
 *
 * @param startLine   first logical line, inclusive
 * @param endLine     logical line past the last, exclusive; equal to {@code startLine} for
 *                    {@link Kind#DELETION}
 * @param kind        what this presentation represents
 * @param changeType  one of {@link VcsRange#INSERTED}, {@link VcsRange#DELETED},
 *                    {@link VcsRange#MODIFIED}, {@link VcsRange#EQUAL}
 * @param color       fill colour, resolved from the colour scheme while building; {@code null} for
 *                    {@link Kind#OUTLINE}
 * @param borderColor border colour, or {@code null} when the scheme defines none
 * @param hovered     whether the mouse is over this presentation's range; resolved at build time so the
 *                    painter stays free of hit-test logic
 * @param payload     the {@link VcsRange} this presentation came from, handed back on click
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record VcsChangePresentation(
    int startLine,
    int endLine,
    Kind kind,
    byte changeType,
    @Nullable ColorValue color,
    @Nullable ColorValue borderColor,
    boolean hovered,
    @Nullable Object payload
) implements LineMarkerPresentation {

    public enum Kind {
        /**
         * Lines that were inserted or modified — a filled span.
         */
        CHANGE,
        /**
         * Lines deleted at a boundary between two surviving lines. {@code startLine == endLine}.
         */
        DELETION,
        /**
         * Border around a whole range whose interior is described by finer-grained
         * {@link #CHANGE} presentations (word-level diff, "smart" mode).
         */
        OUTLINE
    }

    @Override
    public EditorGutterArea area() {
        return EditorGutterArea.RIGHT_FREE_PAINTERS;
    }
}
