/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.colorScheme.TextAttributes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * An interface, defining size and representation of custom visual element in editor.
 *
 * @see InlayModel#addInlineElement(int, boolean, EditorCustomElementRenderer)
 * @see InlayModel#addBlockElement(int, boolean, boolean, int, EditorCustomElementRenderer)
 * @see InlayModel#addAfterLineEndElement(int, boolean, EditorCustomElementRenderer)
 * @see Inlay#getRenderer()
 */
public interface EditorCustomElementRenderer {
    /**
     * Renderer implementation should override this to define width of custom element (in pixels). Returned value will define the result of
     * {@link Inlay#getWidthInPixels()} and the width of {@code targetRegion} parameter passed to renderer's
     * {@link #paint(Inlay, Graphics, Rectangle, TextAttributes)} method. For inline and after-line-end elements it should always be
     * a positive value.
     */
    int calcWidthInPixels(@Nonnull Inlay<?> inlay);

    /**
     * Block element's renderer implementation can override this method to defines the height of element (in pixels). If not overridden,
     * element's height will be equal to editor's line height. Returned value will define the result of {@link Inlay#getWidthInPixels()} and
     * the height of {@code targetRegion} parameter passed to renderer's {@link #paint(Inlay, Graphics, Rectangle, TextAttributes)} method.
     * Returned value is currently not used for inline elements.
     */
    default int calcHeightInPixels(@Nonnull Inlay<?> inlay) {
        return inlay.getEditor().getLineHeight();
    }

    /**
     * Defines the appearance of an inlay.
     * <p>
     * For precise positioning on HiDPI screens,
     * override {@link #paint(Inlay, Graphics2D, Rectangle2D, TextAttributes)} instead.
     *
     * @param targetRegion   the region where painting should be performed.
     *                       The location of this rectangle is provided by the editor,
     *                       the size of the rectangle matches the inlay's width and height,
     *                       as provided by {@link #calcWidthInPixels(Inlay)} and {@link #calcHeightInPixels(Inlay)}.
     * @param textAttributes attributes of the surrounding text
     */
    default void paint(@Nonnull Inlay<?> inlay, @Nonnull Graphics g, @Nonnull Rectangle targetRegion, @Nonnull TextAttributes textAttributes) {
    }

    /**
     * Defines the appearance of an inlay.
     * <p>
     * Either this method or {@link #paint(Inlay, Graphics, Rectangle, TextAttributes)} needs to be overridden only.
     *
     * @param targetRegion   the region where painting should be performed.
     *                       The location of this rectangle is provided by the editor,
     *                       the size of the rectangle approximately matches the inlay's width and height,
     *                       as provided by {@link #calcWidthInPixels(Inlay)} and {@link #calcHeightInPixels(Inlay)} &#x2014;
     *                       they can differ somewhat due to rounding to integer device pixels
     * @param textAttributes attributes of the surrounding text
     */
    default void paint(@Nonnull Inlay<?> inlay,
                       @Nonnull Graphics2D g,
                       @Nonnull Rectangle2D targetRegion,
                       @Nonnull TextAttributes textAttributes) {
        Rectangle region =
            new Rectangle((int) targetRegion.getX(), (int) targetRegion.getY(), inlay.getWidthInPixels(), inlay.getHeightInPixels());
        paint(inlay, (Graphics) g, region, textAttributes);
    }

    /**
     * Returns a registered id of action group, which is to be used for displaying context menu for the given custom element.
     * If {@code null} is returned, standard editor's context menu will be displayed upon corresponding mouse event.
     */
    @Nullable
    default String getContextMenuGroupId(@Nonnull Inlay<?> inlay) {
        return null;
    }

    /**
     * For block inlays, allows showing an icon for related actions in the gutter.
     * The icon is only rendered if its height is not larger than the inlay's height.
     * <p>
     * The returned renderer must implement {@code equals} based on its value, not its identity,
     * as {@link Inlay#update()} only updates the inlay's provider
     * if the returned instance is not equal to the previously defined one.
     */
    default @Nullable GutterIconRenderer calcGutterIconRenderer(@Nonnull Inlay<?> inlay) {
        return null;
    }
}
