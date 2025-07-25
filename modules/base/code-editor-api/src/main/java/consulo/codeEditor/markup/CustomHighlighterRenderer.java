/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * Specifies custom representation for an editor highlighter.
 *
 * @see RangeHighlighter#setCustomRenderer(CustomHighlighterRenderer)
 * @see RangeHighlighter#getCustomRenderer()
 * @author max
 */
public interface CustomHighlighterRenderer {
    void paint(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter, @Nonnull Graphics g);

    /**
     * @deprecated please use {@link #getOrder()} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(forRemoval = true)
    default boolean isForeground() {
        return false;
    }

    /**
     * Allows specifying the order of painting for this highlighter.
     * Default is {@link CustomHighlighterOrder#AFTER_BACKGROUND} - paint highlighter over the background and before the text.
     *
     * @see CustomHighlighterOrder
     */
    default @Nonnull CustomHighlighterOrder getOrder() {
        if (isForeground()) {
            return CustomHighlighterOrder.AFTER_TEXT;
        }
        else {
            return CustomHighlighterOrder.AFTER_BACKGROUND;
        }
    }
}
