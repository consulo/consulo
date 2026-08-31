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

/**
 * Logical horizontal band of the gutter a {@link LineMarkerPresentation} is drawn in. Resolved to concrete
 * x/width by each platform, so no pixel value ever appears in the presentation itself.
 * <p>
 * Supersedes {@link LineMarkerRenderer.Position}, which only offered LEFT/RIGHT/CUSTOM and forced
 * renderers that needed anything else to read gutter offsets directly.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public enum EditorGutterArea {
    /**
     * Narrow strip left of the icons area. Used by code coverage.
     */
    LEFT_FREE_PAINTERS,
    /**
     * Narrow strip right of the icons area, adjacent to the editor content. Used by VCS change bars.
     */
    RIGHT_FREE_PAINTERS,
    /**
     * The gutter icons area.
     */
    ICONS,
    /**
     * The VCS annotations (blame) area.
     */
    ANNOTATIONS,
    /**
     * From the gutter's left edge up to the annotations area.
     */
    BEFORE_ANNOTATIONS,
    /**
     * The whole gutter width.
     */
    WHOLE_GUTTER,
    /**
     * From the right edge of the annotations area to the right edge of the gutter.
     */
    AFTER_ANNOTATIONS,
    /**
     * From the right edge of the annotations area up to the whitespace separator, i.e. everything
     * between the annotations and the folding outline.
     */
    AFTER_ANNOTATIONS_TO_SEPARATOR,
    /**
     * From the gutter's left edge up to the whitespace separator, i.e. everything except the folding outline.
     */
    BEFORE_WHITESPACE_SEPARATOR,
    /**
     * From the whitespace separator to the right edge of the gutter, i.e. the folding outline.
     */
    FROM_WHITESPACE_SEPARATOR
}
