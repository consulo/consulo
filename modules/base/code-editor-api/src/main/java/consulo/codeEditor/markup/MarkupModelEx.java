/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.disposer.Disposable;
import consulo.document.MarkupIterator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author max
 */
public interface MarkupModelEx extends MarkupModel {
    void dispose();

    @Nullable
    RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int lineNumber, int layer);

    /**
     * Consider using {@link #addPersistentLineHighlighter(TextAttributesKey, int, int)}
     * unless it's really necessary.
     * Creating a highlighter with hard-coded {@link TextAttributes} makes it stay the same in all {@link EditorColorsScheme}
     * An editor can provide a custom scheme different from the global one, also a user can change the global scheme explicitly.
     * Using the overload taking a {@link TextAttributesKey} will make the platform take care of all these cases.
     */
    @Nullable
    RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, @Nullable TextAttributes textAttributes);

    /**
     * @deprecated use {@code RangeHighlighterEx.setXXX()} methods to fire changes
     */
    @Deprecated
    default void fireAttributesChanged(@Nonnull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
    }

    void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter);

    void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter);

    boolean containsHighlighter(@Nonnull RangeHighlighter highlighter);

    void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener);

    void setRangeHighlighterAttributes(@Nonnull RangeHighlighter highlighter, @Nonnull TextAttributes textAttributes);

    boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor);

    boolean processRangeHighlightersOutside(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor);

    @Nonnull
    MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset);

    @Nonnull
    MarkupIterator<RangeHighlighterEx> overlappingIterator(
        int startOffset,
        int endOffset,
        boolean onlyRenderedInGutter,
        boolean onlyRenderedInScrollBar
    );

    // optimization: creates highlighter and fires only one event: highlighterCreated
    @Nonnull
    RangeHighlighterEx addRangeHighlighterAndChangeAttributes(
        @Nullable TextAttributesKey textAttributesKey,
        int startOffset,
        int endOffset,
        int layer,
        @Nonnull HighlighterTargetArea targetArea,
        boolean isPersistent,
        @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction
    );

    /**
     * @param isPersistent use different logic to update range boundaries on document changes.
     *                     See {@link RangeMarkerImpl#persistentHighlighterUpdate}.
     * @deprecated use {@link #addRangeHighlighterAndChangeAttributes(TextAttributesKey, int, int, int, HighlighterTargetArea, boolean, Consumer)}
     * Creating a highlighter with hard-coded {@link TextAttributes} makes it stay the same in all {@link EditorColorsScheme}
     * An editor can provide a custom scheme different from the global one, also a user can change the global scheme explicitly.
     * Using the overload taking a {@link TextAttributesKey} will make the platform take care of all these cases.
     */
    @Deprecated
    @Nonnull
    default RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                      int endOffset,
                                                                      int layer,
                                                                      TextAttributes textAttributes,
                                                                      @Nonnull HighlighterTargetArea targetArea,
                                                                      boolean isPersistent,
                                                                      @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        return addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, layer, targetArea, isPersistent, ex -> {
            if (textAttributes != null) {
                ex.setTextAttributes(textAttributes);
            }
            if (changeAttributesAction != null) {
                changeAttributesAction.accept(ex);
            }
        });
    }

    // runs change attributes action and fires highlighterChanged event if there were changes
    void changeAttributesInBatch(
        @Nonnull RangeHighlighterEx highlighter,
        @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction
    );

    default void setErrorStripeVisible(boolean value) {
    }
}
