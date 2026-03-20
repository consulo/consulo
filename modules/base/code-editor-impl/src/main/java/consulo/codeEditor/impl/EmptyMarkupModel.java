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

import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.MarkupIterator;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This is mock implementation to be used in null-object pattern where necessary.
 *
 * @author max
 */
public class EmptyMarkupModel implements MarkupModelEx {
    private final Document myDocument;

    public EmptyMarkupModel(Document document) {
        myDocument = document;
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    
    public RangeHighlighter addRangeHighlighter(int startOffset, int endOffset, int layer, @Nullable TextAttributes textAttributes, HighlighterTargetArea targetArea) {
        throw new ProcessCanceledException();
    }

    
    @Override
    public RangeHighlighter addRangeHighlighter(@Nullable TextAttributesKey textAttributesKey, int startOffset, int endOffset, int layer, HighlighterTargetArea targetArea) {
        throw new ProcessCanceledException();
    }

    @Override
    public @Nullable RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int lineNumber, int layer) {
        throw new ProcessCanceledException();
    }

    
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(@Nullable TextAttributesKey textAttributesKey, int startOffset, int endOffset, int layer, HighlighterTargetArea targetArea, boolean isPersistent, @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        throw new ProcessCanceledException();
    }

    @Override
    public void changeAttributesInBatch(RangeHighlighterEx highlighter, Consumer<? super RangeHighlighterEx> changeAttributesAction) {
    }

    @Override
    
    public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
        throw new ProcessCanceledException();
    }

    @Override
    
    public RangeHighlighter addLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int line, int layer) {
        throw new ProcessCanceledException();
    }

    @Override
    public void removeHighlighter(RangeHighlighter rangeHighlighter) {
    }

    @Override
    public void removeAllHighlighters() {
    }

    @Override
    
    public RangeHighlighter[] getAllHighlighters() {
        return RangeHighlighter.EMPTY_ARRAY;
    }

    @Override
    public <T> T getUserData(Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(Key<T> key, T value) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        return null;
    }

    @Override
    public boolean containsHighlighter(RangeHighlighter highlighter) {
        return false;
    }

    @Override
    public void addMarkupModelListener(Disposable parentDisposable, MarkupModelListener listener) {
    }

    @Override
    public void setRangeHighlighterAttributes(RangeHighlighter highlighter, TextAttributes textAttributes) {

    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, Predicate<? super RangeHighlighterEx> processor) {
        return false;
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end, Predicate<? super RangeHighlighterEx> processor) {
        return false;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        return MarkupIterator.EMPTY;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar) {
        return MarkupIterator.EMPTY;
    }

    @Override
    public void fireAfterAdded(RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireBeforeRemoved(RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireAfterRemoved(RangeHighlighterEx highlighter) {

    }
}
