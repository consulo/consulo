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

package consulo.codeEditor.impl;

import consulo.application.ApplicationManager;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.disposer.Disposable;
import consulo.disposer.util.DisposableList;
import consulo.document.Document;
import consulo.document.MarkupIterator;
import consulo.document.impl.IntervalTreeImpl;
import consulo.document.impl.TextRangeInterval;
import consulo.document.internal.DocumentEx;
import consulo.document.util.DocumentUtil;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MarkupModelImpl extends UserDataHolderBase implements MarkupModelEx {
    private static final Logger LOG = Logger.getInstance(MarkupModelImpl.class);
    private final DocumentEx myDocument;

    private RangeHighlighter[] myCachedHighlighters;
    private final DisposableList<MarkupModelListener> myListeners = DisposableList.create();
    private final RangeHighlighterTree myHighlighterTree;          // this tree holds regular highlighters with target = HighlighterTargetArea.EXACT_RANGE
    private final RangeHighlighterTree myHighlighterTreeForLines;  // this tree holds line range highlighters with target = HighlighterTargetArea.LINES_IN_RANGE

    public MarkupModelImpl(@Nonnull DocumentEx document) {
        myDocument = document;
        myHighlighterTree = new RangeHighlighterTree(document, this);
        myHighlighterTreeForLines = new RangeHighlighterTree(document, this);
    }

    @Override
    public void dispose() {
        myHighlighterTree.dispose(myDocument);
        myHighlighterTreeForLines.dispose(myDocument);
    }

    @Override
    @Nonnull
    public RangeHighlighter addLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        if (isNotValidLine(lineNumber)) {
            throw new IndexOutOfBoundsException("lineNumber:" + lineNumber + ". Must be in [0, " + (getDocument().getLineCount() - 1) + "]");
        }

        int offset = DocumentUtil.getFirstNonSpaceCharOffset(getDocument(), lineNumber);
        return addRangeHighlighter(offset, offset, layer, textAttributes, HighlighterTargetArea.LINES_IN_RANGE);
    }

    @Override
    @Nullable
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        if (isNotValidLine(lineNumber)) {
            return null;
        }

        int offset = DocumentUtil.getFirstNonSpaceCharOffset(getDocument(), lineNumber);
        return addRangeHighlighter(PersistentRangeHighlighterImpl.create(this, offset, layer, HighlighterTargetArea.LINES_IN_RANGE, textAttributes, false), null);
    }

    private boolean isNotValidLine(int lineNumber) {
        return lineNumber >= getDocument().getLineCount() || lineNumber < 0;
    }

    // NB: Can return invalid highlighters
    @Override
    @Nonnull
    public RangeHighlighter[] getAllHighlighters() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (myCachedHighlighters == null) {
            int size = myHighlighterTree.size() + myHighlighterTreeForLines.size();
            if (size == 0) {
                return RangeHighlighter.EMPTY_ARRAY;
            }
            List<RangeHighlighterEx> list = new ArrayList<>(size);
            CommonProcessors.CollectProcessor<RangeHighlighterEx> collectProcessor = new CommonProcessors.CollectProcessor<>(list);
            myHighlighterTree.processAll(collectProcessor);
            myHighlighterTreeForLines.processAll(collectProcessor);
            myCachedHighlighters = list.toArray(RangeHighlighter.EMPTY_ARRAY);
        }
        return myCachedHighlighters;
    }

    @Nonnull
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                     int endOffset,
                                                                     int layer,
                                                                     TextAttributes textAttributes,
                                                                     @Nonnull HighlighterTargetArea targetArea,
                                                                     boolean isPersistent,
                                                                     @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        return addRangeHighlighter(isPersistent
            ? PersistentRangeHighlighterImpl.create(this, startOffset, layer, targetArea, textAttributes, true)
            : new RangeHighlighterImpl(this, startOffset, endOffset, layer, targetArea, textAttributes, false, false), changeAttributesAction);
    }

    @Nonnull
    private RangeHighlighterEx addRangeHighlighter(@Nonnull RangeHighlighterImpl highlighter, @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myCachedHighlighters = null;
        if (changeAttributesAction != null) {
            highlighter.changeAttributesNoEvents(changeAttributesAction);
        }
        fireAfterAdded(highlighter);
        return highlighter;
    }

    @Override
    public void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        UIAccess.assertIsUIThread();

        byte changeStatus = ((RangeHighlighterImpl) highlighter).changeAttributesNoEvents(changeAttributesAction);
        if (BitUtil.isSet(changeStatus, RangeHighlighterImpl.CHANGED_MASK)) {
            fireAttributesChanged(highlighter, BitUtil.isSet(changeStatus, RangeHighlighterImpl.RENDERERS_CHANGED_MASK), BitUtil.isSet(changeStatus, RangeHighlighterImpl.FONT_STYLE_OR_COLOR_CHANGED_MASK));
        }
    }

    @Nonnull
    @Override
    public RangeHighlighter addRangeHighlighter(@Nullable TextAttributesKey textAttributesKey, int startOffset, int endOffset, int layer, @Nonnull HighlighterTargetArea targetArea) {
        TextAttributes attributes = EditorColorsManager.getInstance().getCurrentScheme().getAttributes(textAttributesKey);
        // TODO this method must be changed, using TextAttributesKey as is
        return addRangeHighlighter(startOffset, endOffset, layer, attributes, targetArea);
    }

    @Override
    public void addRangeHighlighter(@Nonnull RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
        UIAccess.assertIsUIThread();

        treeFor(marker).addInterval(marker, start, end, greedyToLeft, greedyToRight, false, layer);
    }

    RangeHighlighterTree treeFor(RangeHighlighter marker) {
        return marker.getTargetArea() == HighlighterTargetArea.EXACT_RANGE ? myHighlighterTree : myHighlighterTreeForLines;
    }

    @Override
    @Nonnull
    public RangeHighlighter addRangeHighlighter(int startOffset, int endOffset, int layer, TextAttributes textAttributes, @Nonnull HighlighterTargetArea targetArea) {
        return addRangeHighlighterAndChangeAttributes(startOffset, endOffset, layer, textAttributes, targetArea, false, null);
    }

    @Override
    public void removeHighlighter(@Nonnull RangeHighlighter segmentHighlighter) {
        UIAccess.assertIsUIThread();

        myCachedHighlighters = null;
        if (!segmentHighlighter.isValid()) {
            return;
        }

        boolean removed = treeFor(segmentHighlighter).removeInterval((RangeHighlighterEx) segmentHighlighter);
        LOG.assertTrue(removed);
    }

    @Override
    public void removeAllHighlighters() {
        UIAccess.assertIsUIThread();

        for (RangeHighlighter highlighter : getAllHighlighters()) {
            highlighter.dispose();
        }
        myCachedHighlighters = null;
        myHighlighterTree.clear();
        myHighlighterTreeForLines.clear();
    }

    @Override
    @Nonnull
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull final MarkupModelListener listener) {
        myListeners.add(listener, parentDisposable);
    }

    private void removeMarkupModelListener(@Nonnull MarkupModelListener listener) {
        boolean success = myListeners.remove(listener);
        LOG.assertTrue(success);
    }

    @Override
    public void setRangeHighlighterAttributes(@Nonnull final RangeHighlighter highlighter, @Nonnull final TextAttributes textAttributes) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        ((RangeHighlighterEx) highlighter).setTextAttributes(textAttributes);
    }

    @Override
    public void fireAttributesChanged(@Nonnull RangeHighlighterEx segmentHighlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
        for (MarkupModelListener listener : myListeners) {
            listener.attributesChanged(segmentHighlighter, renderersChanged, fontStyleOrColorChanged);
        }
    }

    @Override
    public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.afterAdded(segmentHighlighter);
        }
    }

    @Override
    public void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.beforeRemoved(segmentHighlighter);
        }
    }

    @Override
    public boolean containsHighlighter(@Nonnull final RangeHighlighter highlighter) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Processor<RangeHighlighterEx> equalId = h -> h.getId() != ((RangeHighlighterEx) highlighter).getId();
        return !treeFor(highlighter).processOverlappingWith(highlighter.getStartOffset(), highlighter.getEndOffset(), equalId);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
        MarkupIterator<RangeHighlighterEx> iterator = overlappingIterator(start, end);
        try {
            while (iterator.hasNext()) {
                if (!processor.process(iterator.next())) {
                    return false;
                }
            }
            return true;
        }
        finally {
            iterator.dispose();
        }
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
        return myHighlighterTree.processOverlappingWithOutside(start, end, processor) && myHighlighterTreeForLines.processOverlappingWithOutside(start, end, processor);
    }

    @Override
    @Nonnull
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        return IntervalTreeImpl
            .mergingOverlappingIterator(myHighlighterTree, new TextRangeInterval(startOffset, endOffset), myHighlighterTreeForLines, roundToLineBoundaries(getDocument(), startOffset, endOffset),
                RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    }

    @Nonnull
    @Override
    public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar) {
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        MarkupIterator<RangeHighlighterEx> exact = myHighlighterTree.overlappingIterator(new TextRangeInterval(startOffset, endOffset), onlyRenderedInGutter, onlyRenderedInScrollBar);
        MarkupIterator<RangeHighlighterEx> lines = myHighlighterTreeForLines.overlappingIterator(roundToLineBoundaries(getDocument(), startOffset, endOffset), onlyRenderedInGutter, onlyRenderedInScrollBar);
        return MarkupIterator.mergeIterators(exact, lines, RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    }

    @Nonnull
    public static TextRangeInterval roundToLineBoundaries(@Nonnull Document document, int startOffset, int endOffset) {
        int textLength = document.getTextLength();
        int lineStartOffset = startOffset <= 0 ? 0 : startOffset > textLength ? textLength : document.getLineStartOffset(document.getLineNumber(startOffset));
        int lineEndOffset = endOffset <= 0 ? 0 : endOffset >= textLength ? textLength : document.getLineEndOffset(document.getLineNumber(endOffset));
        return new TextRangeInterval(lineStartOffset, lineEndOffset);
    }
}
