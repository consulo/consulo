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

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.LogicalPosition;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * The band of a block inlay marked with a colour across the gutter.
 * <p>
 * It stands on the boundary at the top of {@code line}, so it covers no line of its own; every editor implementation
 * places it by {@link #getBandRelativeToLine(Editor)}.
 *
 * @param line  logical line of the inlay
 * @param area  gutter band to occupy
 * @param color fill colour
 * @param inlay the block inlay whose band is filled
 */
public record InlayFillColorLineMarkerPresentation(
    int line,
    EditorGutterArea area,
    ColorValue color,
    Inlay<?> inlay
) implements LineMarkerPresentation {
    @Override
    public int startLine() {
        return line;
    }

    @Override
    public int endLine() {
        return line;
    }

    /**
     * @return the band of the inlay with its y relative to the top of {@link #line()}, x and width left zero, or
     * {@code null} while the inlay is not shown
     */
    public @Nullable Rectangle getBandRelativeToLine(Editor editor) {
        Rectangle bounds = inlay.isValid() ? inlay.getBounds() : null;
        if (bounds == null || bounds.height <= 0) {
            return null;
        }
        int lineTop = editor.logicalPositionToXY(new LogicalPosition(line, 0)).y;
        return new Rectangle(0, bounds.y - lineTop, 0, bounds.height);
    }
}
