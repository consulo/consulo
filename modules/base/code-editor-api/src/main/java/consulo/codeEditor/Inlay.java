// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.disposer.Disposable;
import consulo.document.RangeMarker;
import consulo.util.dataholder.UserDataHolderEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * A custom visual element displayed in editor. It is associated with a certain position in a document, but is not
 * represented in document text in any way. Inlay's document position (offset) is updated on document changes just like
 * for a {@link RangeMarker}. Both 'inline' (displayed within text lines) and 'block' (displayed between text lines) elements are supported.
 * <p>
 * Inlay becomes invalid on explicit disposal, or when a document range fully containing inlay's offset, is deleted.
 * <p>
 *
 * @see InlayModel
 */
public interface Inlay<T extends EditorCustomElementRenderer> extends Disposable, UserDataHolderEx {
    /**
     * Returns editor, this custom visual element belongs to.
     */
    @Nonnull
    Editor getEditor();

    /**
     * Defines relative position of inlay element with respect to the containing text.
     */
    @Nonnull
    Placement getPlacement();

    /**
     * Tells whether this element is valid. Inlay becomes invalid on explicit disposal,
     * or when a document range fully containing inlay's offset, is deleted.
     */
    boolean isValid();

    /**
     * Returns current inlay's position in the document. This position is updated on document changes just like for a {@link RangeMarker}.
     */
    int getOffset();

    /**
     * Tells whether this element is associated with preceding or following text. This relation defines certain aspects of inlay's behaviour
     * with respect to changes in editor, e.g. when text is inserted at inlay's position, inlay will end up before the inserted text if the
     * returned value is {@code false} and after the text, if the returned value is {@code true}.
     * Also, when {@link Caret#moveToOffset(int)} or similar offset-based method is invoked, and an inlay exists at the given offset,
     * caret will be positioned to the left of inlay if returned value is {@code true}, and vice versa.
     * <p>
     * The value is determined at element's creation (see {@link InlayModel#addInlineElement(int, boolean, EditorCustomElementRenderer)
     * or {@link InlayModel#addBlockElement(int, boolean, boolean, EditorCustomElementRenderer)}}.
     */
    boolean isRelatedToPrecedingText();

    /**
     * Returns current visual position of the inlay's left boundary. For 'block' elements, this is just a visual position associated with
     * inlay's offset.
     */
    @Nonnull
    VisualPosition getVisualPosition();

    /**
     * Returns inlay element's bounds in editor coordinate system if it's visible (not folded), or {@code null} otherwise
     */
    @Nullable
    Rectangle getBounds();

    /**
     * Returns renderer, which defines size and representation for this inlay.
     */
    @Nonnull
    T getRenderer();

    /**
     * Returns current inlay's width. Width is defined at inlay's creation using information returned by inlay's renderer.
     * To change width, {@link #update()} method should be called.
     */
    int getWidthInPixels();

    /**
     * Returns current inlay's width. Width is defined at inlay's creation using information returned by inlay's renderer.
     * To change height (supported for 'block' elements only), {@link #update()} method should be called.
     */
    int getHeightInPixels();

    /**
     * Returns the {@link GutterIconRenderer} instance defining an icon displayed in gutter, and associated actions (supported for block inlays
     * at the moment). This provider is defined at inlay's creation using information returned by inlay's renderer. To change it,
     * {@link #update()} method should be called.
     *
     * @see EditorCustomElementRenderer#calcGutterIconRenderer(Inlay)
     */
    @Nullable
    GutterIconRenderer getGutterIconRenderer();

    /**
     * Updates inlay properties (width, height, gutter icon renderer) from inlay's renderer. Also, repaints the inlay.
     *
     * @see EditorCustomElementRenderer#calcWidthInPixels(Inlay)
     * @see EditorCustomElementRenderer#calcHeightInPixels(Inlay)
     * @see EditorCustomElementRenderer#calcGutterIconRenderer(Inlay)
     * @see #repaint()
     */
    void update();

    /**
     * Causes repaint of inlay in editor.
     */
    void repaint();

    /**
     * Returns properties specified at inlay creation.
     *
     * @see InlayModel#addInlineElement(int, InlayProperties, EditorCustomElementRenderer)
     * @see InlayModel#addBlockElement(int, InlayProperties, EditorCustomElementRenderer)
     * @see InlayModel#addAfterLineEndElement(int, InlayProperties, EditorCustomElementRenderer)
     */
    @Nonnull
    default InlayProperties getProperties() {
        return new InlayProperties();
    }

    /**
     * @see #getPlacement()
     */
    enum Placement {
        INLINE,
        ABOVE_LINE,
        BELOW_LINE,
        AFTER_LINE_END
    }
}
