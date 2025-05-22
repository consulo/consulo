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
package consulo.codeEditor.impl;

import consulo.application.util.function.FilteringProcessor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.MarkupIterator;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EditorFilteringMarkupModelEx implements MarkupModelEx {
    @Nonnull
    private final RealEditor myEditor;
    @Nonnull
    private final MarkupModelEx myDelegate;

    private final Predicate<RangeHighlighter> IS_AVAILABLE = this::isAvailable;

    public EditorFilteringMarkupModelEx(@Nonnull RealEditor editor, @Nonnull MarkupModelEx delegate) {
        myEditor = editor;
        myDelegate = delegate;
    }

    @Nonnull
    public MarkupModelEx getDelegate() {
        return myDelegate;
    }

    private boolean isAvailable(@Nonnull RangeHighlighter highlighter) {
        return highlighter.getEditorFilter().avaliableIn(myEditor) && myEditor.isHighlighterAvailable(highlighter);
    }

    @Override
    public boolean containsHighlighter(@Nonnull RangeHighlighter highlighter) {
        return isAvailable(highlighter) && myDelegate.containsHighlighter(highlighter);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
        //noinspection unchecked
        FilteringProcessor<? super RangeHighlighterEx> filteringProcessor = new FilteringProcessor(IS_AVAILABLE, processor);
        return myDelegate.processRangeHighlightersOverlappingWith(start, end, filteringProcessor);
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
        //noinspection unchecked
        FilteringProcessor<? super RangeHighlighterEx> filteringProcessor = new FilteringProcessor(IS_AVAILABLE, processor);
        return myDelegate.processRangeHighlightersOutside(start, end, filteringProcessor);
    }

    @Override
    @Nonnull
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        return new FilteringMarkupIterator<>(myDelegate.overlappingIterator(startOffset, endOffset), this::isAvailable);
    }

    @Nonnull
    @Override
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(
        int startOffset,
        int endOffset,
        boolean onlyRenderedInGutter,
        boolean onlyRenderedInScrollBar
    ) {
        return new FilteringMarkupIterator<>(
            myDelegate.overlappingIterator(
                startOffset,
                endOffset,
                onlyRenderedInGutter,
                onlyRenderedInScrollBar
            ),
            this::isAvailable
        );
    }

    @Override
    @Nonnull
    public RangeHighlighter[] getAllHighlighters() {
        List<RangeHighlighter> list = ContainerUtil.filter(myDelegate.getAllHighlighters(), IS_AVAILABLE);
        return list.toArray(RangeHighlighter.EMPTY_ARRAY);
    }

    @Override
    public void dispose() {
    }

    @Nullable
    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int lineNumber, int layer) {
        return myDelegate.addPersistentLineHighlighter(textAttributesKey, lineNumber, layer);
    }

    @Override
    @Nonnull
    public Document getDocument() {
        return myDelegate.getDocument();
    }

    @Override
    public void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener) {
        myDelegate.addMarkupModelListener(parentDisposable, listener);
    }

    @Override
    public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {
        myDelegate.fireAfterAdded(segmentHighlighter);
    }

    @Override
    public void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter) {
        myDelegate.fireBeforeRemoved(segmentHighlighter);
    }

    @Override
    @Nullable
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        return myDelegate.addPersistentLineHighlighter(lineNumber, layer, textAttributes);
    }

    @Override
    @Nonnull
    public RangeHighlighter addRangeHighlighter(
        int startOffset,
        int endOffset,
        int layer,
        @Nullable TextAttributes textAttributes,
        @Nonnull HighlighterTargetArea targetArea
    ) {
        return myDelegate.addRangeHighlighter(startOffset, endOffset, layer, textAttributes, targetArea);
    }

    @Nonnull
    @Override
    public RangeHighlighter addRangeHighlighter(
        @Nullable TextAttributesKey textAttributesKey,
        int startOffset,
        int endOffset,
        int layer,
        @Nonnull HighlighterTargetArea targetArea
    ) {
        return myDelegate.addRangeHighlighter(textAttributesKey, startOffset, endOffset, layer, targetArea);
    }

    @Override
    @Nonnull
    public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
        return myDelegate.addLineHighlighter(line, layer, textAttributes);
    }

    @Nonnull
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(
        @Nullable TextAttributesKey textAttributesKey,
        int startOffset,
        int endOffset,
        int layer,
        @Nonnull HighlighterTargetArea targetArea,
        boolean isPersistent,
        @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction
    ) {
        return myDelegate.addRangeHighlighterAndChangeAttributes(textAttributesKey,
            startOffset,
            endOffset,
            layer,
            targetArea,
            isPersistent, changeAttributesAction
        );
    }

    @Override
    public void setRangeHighlighterAttributes(@Nonnull RangeHighlighter highlighter, @Nonnull TextAttributes textAttributes) {
        myDelegate.setRangeHighlighterAttributes(highlighter, textAttributes);
    }

    @Override
    public void changeAttributesInBatch(
        @Nonnull RangeHighlighterEx highlighter,
        @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction
    ) {
        myDelegate.changeAttributesInBatch(highlighter, changeAttributesAction);
    }

    @Override
    public void removeHighlighter(@Nonnull RangeHighlighter rangeHighlighter) {
        myDelegate.removeHighlighter(rangeHighlighter);
    }

    @Override
    public void removeAllHighlighters() {
        myDelegate.removeAllHighlighters();
    }

    @Override
    @Nullable
    public <T> T getUserData(@Nonnull Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        myDelegate.putUserData(key, value);
    }
}
