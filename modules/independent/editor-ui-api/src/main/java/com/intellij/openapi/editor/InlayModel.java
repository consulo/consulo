// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor;

import com.intellij.openapi.Disposable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.EventListener;
import java.util.List;

/**
 * Provides an ability to introduce custom visual elements into editor's representation.
 * Such elements are not reflected in document contents.
 * <p>
 * WARNING! This is an experimental API, it can change at any time.
 */
public interface InlayModel {
  /**
   * Same as {@link #addInlineElement(int, boolean, EditorCustomElementRenderer)}, making created element associated with following text.
   */
  @Nullable
  default Inlay addInlineElement(int offset, @Nonnull EditorCustomElementRenderer renderer) {
    return addInlineElement(offset, false, renderer);
  }

  /**
   * Introduces an inline visual element at a given offset, its width and appearance is defined by the provided renderer. With respect to
   * document changes, created element behaves in a similar way to a zero-range {@link RangeMarker}. This method returns {@code null}
   * if requested element cannot be created, e.g. if corresponding functionality is not supported by current editor instance.
   *
   * @param relatesToPrecedingText whether element is associated with preceding or following text
   *                               (see {@link Inlay#isRelatedToPrecedingText()})
   */
  @Nullable
  Inlay addInlineElement(int offset, boolean relatesToPrecedingText,@Nonnull EditorCustomElementRenderer renderer);

  /**
   * Returns a list of inline elements for a given offset range (both limits are inclusive). Returned list is sorted by offset.
   */
  @Nonnull
  List<Inlay> getInlineElementsInRange(int startOffset, int endOffset);

  /**
   * Tells whether given range of offsets (both sides inclusive) contains at least one inline element.
   */
  default boolean hasInlineElementsInRange(int startOffset, int endOffset) {
    return !getInlineElementsInRange(startOffset, endOffset).isEmpty();
  }

  /**
   * Tells whether there exists at least one inline element currently.
   */
  default boolean hasInlineElements() {
    return hasInlineElementsInRange(0, Integer.MAX_VALUE);
  }

  /**
   * Tells whether there exists an inline visual element at a given offset.
   */
  boolean hasInlineElementAt(int offset);

  /**
   * Tells whether there exists an inline visual element at a given visual position.
   * Only visual position to the left of the element is recognized.
   */
  default boolean hasInlineElementAt(@Nonnull VisualPosition visualPosition) {
    return getInlineElementAt(visualPosition) != null;
  }

  /**
   * Return a custom visual element at at a given visual position. Only visual position to the left of the element is recognized.
   */
  @Nullable
  Inlay getInlineElementAt(@Nonnull VisualPosition visualPosition);

  /**
   * Return a custom visual element at given coordinates in editor's coordinate space,
   * or {@code null} if there's no element at given point.
   */
  @Nullable
  Inlay getElementAt(@Nonnull Point point);

  /**
   * Adds a listener that will be notified after adding, updating and removal of custom visual elements.
   */
  void addListener(@Nonnull Listener listener, @Nonnull Disposable disposable);

  interface Listener extends EventListener {
    void onAdded(@Nonnull Inlay inlay);

    void onUpdated(@Nonnull Inlay inlay);

    void onRemoved(@Nonnull Inlay inlay);
  }

  /**
   * An adapter useful for the cases, when the same action is to be performed after custom visual element's adding, updating and removal.
   */
  abstract class SimpleAdapter implements Listener {
    @Override
    public void onAdded(@Nonnull Inlay inlay) {
      onUpdated(inlay);
    }

    @Override
    public void onUpdated(@Nonnull Inlay inlay) {}

    @Override
    public void onRemoved(@Nonnull Inlay inlay) {
      onUpdated(inlay);
    }
  }
}
