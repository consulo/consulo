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

package consulo.codeEditor.internal;

import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.MarkupIterator;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author cdr
 */
public class MarkupModelWindow extends UserDataHolderBase implements MarkupModelEx {
    private final DocumentWindow myDocument;
    private final MarkupModelEx myHostModel;

    public MarkupModelWindow(MarkupModelEx editorMarkupModel, final DocumentWindow document) {
        myDocument = document;
        myHostModel = editorMarkupModel;
    }

    @Override
    @Nonnull
    public Document getDocument() {
        return myDocument;
    }

    @Override
    @Nonnull
    public RangeHighlighter addRangeHighlighter(final int startOffset, final int endOffset, final int layer, final TextAttributes textAttributes, @Nonnull final HighlighterTargetArea targetArea) {
        TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myHostModel.addRangeHighlighter(hostRange.getStartOffset(), hostRange.getEndOffset(), layer, textAttributes, targetArea);
    }

    @Nonnull
    @Override
    public RangeHighlighter addRangeHighlighter(@Nullable TextAttributesKey textAttributesKey, int startOffset, int endOffset, int layer, @Nonnull HighlighterTargetArea targetArea) {
        TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myHostModel.addRangeHighlighter(textAttributesKey, hostRange.getStartOffset(), hostRange.getEndOffset(), layer, targetArea);
    }

    @Override
    public void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        myHostModel.changeAttributesInBatch(highlighter, changeAttributesAction);
    }

    @Override
    public void removeHighlighter(@Nonnull final RangeHighlighter rangeHighlighter) {
        myHostModel.removeHighlighter(rangeHighlighter);
    }

    @Override
    public void removeAllHighlighters() {
        myHostModel.removeAllHighlighters();
    }

    @Override
    @Nonnull
    public RangeHighlighter[] getAllHighlighters() {
        return myHostModel.getAllHighlighters();
    }

    @Override
    public void dispose() {
        myHostModel.dispose();
    }

    @Nullable
    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int lineNumber, int layer) {
        int hostLine = myDocument.injectedToHostLine(lineNumber);
        return myHostModel.addPersistentLineHighlighter(textAttributesKey, hostLine, layer);
    }

    @Nonnull
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(@Nullable TextAttributesKey textAttributesKey, int startOffset, int endOffset, int layer, @Nonnull HighlighterTargetArea targetArea, boolean isPersistent, @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myHostModel.addRangeHighlighterAndChangeAttributes(textAttributesKey,
            hostRange.getStartOffset(),
            hostRange.getEndOffset(),
            layer,
            targetArea,
            isPersistent,
            changeAttributesAction
        );
    }

    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(final int line, final int layer, final TextAttributes textAttributes) {
        int hostLine = myDocument.injectedToHostLine(line);
        return myHostModel.addPersistentLineHighlighter(hostLine, layer, textAttributes);
    }

    @Override
    public boolean containsHighlighter(@Nonnull final RangeHighlighter highlighter) {
        return myHostModel.containsHighlighter(highlighter);
    }

    @Override
    public void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener) {
        myHostModel.addMarkupModelListener(parentDisposable, listener);
    }

    @Override
    public void setRangeHighlighterAttributes(@Nonnull final RangeHighlighter highlighter, @Nonnull final TextAttributes textAttributes) {
        myHostModel.setRangeHighlighterAttributes(highlighter, textAttributes);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
        return false;
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
        return false;
    }

    @Nonnull
    @Override
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        return myHostModel.overlappingIterator(startOffset, endOffset);
    }

    @Override
    @Nonnull
    public RangeHighlighter addLineHighlighter(final @Nullable TextAttributesKey textAttributesKey,
                                               final int line,
                                               final int layer) {
        int hostLine = myDocument.injectedToHostLine(line);
        return myHostModel.addLineHighlighter(textAttributesKey, hostLine, layer);
    }

    @Override
    @Nonnull
    public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
        int hostLine = myDocument.injectedToHostLine(line);
        return myHostModel.addLineHighlighter(hostLine, layer, textAttributes);
    }

    @Nonnull
    @Override
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireAfterRemoved(@Nonnull RangeHighlighterEx highlighter) {

    }
}
