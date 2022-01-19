// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor;

import consulo.disposer.Disposable;
import com.intellij.util.containers.ContainerUtil;
import consulo.document.RangeMarker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.EventListener;
import java.util.List;

/**
 * Provides an ability to introduce custom visual elements into editor's representation.
 * Such elements are not reflected in document contents. Elements are 'anchored' to a certain document offset at creation,
 * this offset behaves similar to a zero-range {@link RangeMarker} with respect to document changes.
 * <p>
 *
 * @see Editor#getInlayModel()
 */
public interface InlayModel {
  /**
   * Same as {@link #addInlineElement(int, boolean, EditorCustomElementRenderer)}, making created element associated with following text.
   */
  @Nullable
  default <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, @Nonnull T renderer) {
    return addInlineElement(offset, false, renderer);
  }

  /**
   * Introduces an inline visual element at a given offset, its width and appearance is defined by the provided renderer.
   *
   * @param relatesToPrecedingText whether element is associated with preceding or following text
   *                               (see {@link Inlay#isRelatedToPrecedingText()})
   * @return {@code null} if requested element cannot be created, e.g. if corresponding functionality
   * is not supported by current editor instance.
   */
  @Nullable
  <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer);

  /**
   * Introduces a 'block' visual element at a given offset, its size and appearance is defined by the provided renderer. This element
   * will be displayed between lines of text.
   *
   * @param relatesToPrecedingText whether element is associated with preceding or following text
   *                               (see {@link Inlay#isRelatedToPrecedingText()})
   * @param showAbove              whether element will be displayed above or below corresponding visual line
   * @param priority               if multiple elements are requested to be displayed for the same visual line, this parameter defines the relative
   *                               positioning of such elements (larger priority value means the element will be rendered closer to the text)
   * @return {@code null} if requested element cannot be created, e.g. if corresponding functionality
   * is not supported by current editor instance.
   */
  @Nullable
  <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, @Nonnull T renderer);


  /**
   * Introduces a visual element, which will be displayed after the end of corresponding logical line.
   *
   * @param relatesToPrecedingText whether element is associated with preceding or following text
   *                               (see {@link Inlay#isRelatedToPrecedingText()})
   * @return {@code null} if requested element cannot be created, e.g. if corresponding functionality
   * is not supported by current editor instance.
   */
  @Nullable
  <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer);

  /**
   * Returns a list of inline elements for a given offset range (both limits are inclusive). Returned list is sorted by offset.
   * Both visible and invisible (due to folding) elements are returned.
   */
  @Nonnull
  List<Inlay> getInlineElementsInRange(int startOffset, int endOffset);

  /**
   * Same as {@link #getInlineElementsInRange(int, int)}, but returned list contains only inlays with renderer of given type.
   */
  default <T> List<Inlay<? extends T>> getInlineElementsInRange(int startOffset, int endOffset, @Nonnull Class<T> type) {
    //noinspection unchecked
    return (List)ContainerUtil.filter(getInlineElementsInRange(startOffset, endOffset), inlay -> type.isInstance(inlay.getRenderer()));
  }

  /**
   * Returns a list of block elements for a given offset range (both limits are inclusive) in priority order
   * (higher priority ones appear first). Both visible and invisible (due to folding) elements are returned.
   */
  @Nonnull
  List<Inlay> getBlockElementsInRange(int startOffset, int endOffset);

  /**
   * Same as {@link #getBlockElementsInRange(int, int)}, but returned list contains only inlays with renderer of given type.
   */
  default <T> List<Inlay<? extends T>> getBlockElementsInRange(int startOffset, int endOffset, @Nonnull Class<T> type) {
    //noinspection unchecked
    return (List)ContainerUtil.filter(getBlockElementsInRange(startOffset, endOffset), inlay -> type.isInstance(inlay.getRenderer()));
  }

  /**
   * Returns a list of block elements displayed for a given visual line in appearance order (top to bottom).
   * Only visible (not folded) elements are returned.
   */
  @Nonnull
  List<Inlay> getBlockElementsForVisualLine(int visualLine, boolean above);

  /**
   * Tells whether there exists at least one block element currently.
   */
  default boolean hasBlockElements() {
    return !getBlockElementsInRange(0, Integer.MAX_VALUE).isEmpty();
  }

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
   * Return a custom visual element with renderer of given type at given coordinates in editor's coordinate space,
   * or {@code null} if there's no such element at given point.
   */
  @Nullable
  default <T> Inlay<? extends T> getElementAt(@Nonnull Point point, @Nonnull Class<T> type) {
    Inlay inlay = getElementAt(point);
    //noinspection unchecked
    return inlay != null && type.isInstance(inlay.getRenderer()) ? inlay : null;
  }

  /**
   * Returns a list of after-line-end elements for a given offset range (both limits are inclusive).
   * Returned list is sorted by offset. Both visible and invisible (due to folding) elements are returned.
   *
   * @see #addAfterLineEndElement(int, boolean, EditorCustomElementRenderer)
   */
  @Nonnull
  List<Inlay> getAfterLineEndElementsInRange(int startOffset, int endOffset);

  /**
   * Same as {@link #getAfterLineEndElementsInRange(int, int)}, but returned list contains only inlays with renderer of given type.
   */
  @Nonnull
  default <T> List<Inlay<? extends T>> getAfterLineEndElementsInRange(int startOffset, int endOffset, @Nonnull Class<T> type) {
    //noinspection unchecked
    return (List)ContainerUtil.filter(getAfterLineEndElementsInRange(startOffset, endOffset), inlay -> type.isInstance(inlay.getRenderer()));
  }

  /**
   * Returns after-line-end elements for a given logical line, in creation order (this is the order they are displayed in).
   * Elements are returned regardless of whether they are currently visible.
   *
   * @see #addAfterLineEndElement(int, boolean, EditorCustomElementRenderer)
   */
  @Nonnull
  List<Inlay> getAfterLineEndElementsForLogicalLine(int logicalLine);

  /**
   * When text is inserted at inline element's offset, resulting element's position is determined by its
   * {@link Inlay#isRelatedToPrecedingText()} property. But to enable natural editing experience around inline elements (so that typed text
   * appears at caret visual position), caret position is also taken into account at document insertion. This method allows to disable
   * accounting for caret position, and can be useful for document modifications which don't originate directly from user actions.
   */
  void setConsiderCaretPositionOnDocumentUpdates(boolean enabled);

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
    public void onUpdated(@Nonnull Inlay inlay) {
    }

    @Override
    public void onRemoved(@Nonnull Inlay inlay) {
      onUpdated(inlay);
    }
  }
}
