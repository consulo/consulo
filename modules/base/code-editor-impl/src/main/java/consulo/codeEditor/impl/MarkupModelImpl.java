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

import consulo.application.util.function.CommonProcessors;
import consulo.codeEditor.markup.*;
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
import consulo.document.util.ProperTextRange;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MarkupModelImpl extends UserDataHolderBase implements MarkupModelEx {
    private static final Logger LOG = Logger.getInstance(MarkupModelImpl.class);
    private final DocumentEx myDocument;

    private volatile RangeHighlighter[] myCachedHighlighters;
    private final DisposableList<MarkupModelListener> myListeners = DisposableList.create();
    private final RangeHighlighterTree myHighlighterTree;          // this tree holds regular highlighters with target = HighlighterTargetArea.EXACT_RANGE
    private final RangeHighlighterTree myHighlighterTreeForLines;  // this tree holds line range highlighters with target = HighlighterTargetArea.LINES_IN_RANGE

    public MarkupModelImpl(@Nonnull DocumentEx document) {
        myDocument = document;
        myHighlighterTree = new RangeHighlighterTree(this);
        myHighlighterTreeForLines = new RangeHighlighterTree(this);
    }

    @Override
    public void dispose() {
        myHighlighterTree.dispose();
        myHighlighterTreeForLines.dispose();
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

    @Nullable
    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey, int lineNumber, int layer) {
        return addPersistentLineHighlighter(textAttributesKey, null, lineNumber, layer);
    }

    @Override
    @Nullable
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        return addPersistentLineHighlighter(null, textAttributes, lineNumber, layer);
    }

    private @Nullable RangeHighlighterEx addPersistentLineHighlighter(@Nullable TextAttributesKey textAttributesKey,
                                                                      @Nullable TextAttributes textAttributes,
                                                                      int lineNumber,
                                                                      int layer) {
        Document document = getDocument();
        if (!DocumentUtil.isValidLine(lineNumber, document)) {
            return null;
        }
        int offset = DocumentUtil.getFirstNonSpaceCharOffset(document, lineNumber);

        Consumer<RangeHighlighterEx> changeAction = textAttributes == null ? null : ex -> ex.setTextAttributes(textAttributes);

        PersistentRangeHighlighterImpl highlighter = PersistentRangeHighlighterImpl.create(
            this, offset, layer, HighlighterTargetArea.LINES_IN_RANGE, textAttributesKey, false);
        addRangeHighlighter(highlighter, changeAction);
        return highlighter;
    }

    private boolean isNotValidLine(int lineNumber) {
        return lineNumber >= getDocument().getLineCount() || lineNumber < 0;
    }

    // NB: Can return invalid highlighters
    @Nonnull
    @Override
    public RangeHighlighter[] getAllHighlighters() {
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

    @Override
    @Nonnull
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(@Nullable TextAttributesKey textAttributesKey,
                                                                     int startOffset,
                                                                     int endOffset,
                                                                     int layer,
                                                                     @Nonnull HighlighterTargetArea targetArea,
                                                                     boolean isPersistent,
                                                                     @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        RangeHighlighterImpl highlighter = isPersistent ?
            PersistentRangeHighlighterImpl.create(this, startOffset, layer, targetArea, textAttributesKey, true)
            : new RangeHighlighterImpl(this, startOffset, endOffset, layer, targetArea, textAttributesKey, false, false);
        addRangeHighlighter(highlighter, changeAttributesAction);
        return highlighter;
    }

    private void addRangeHighlighter(@Nonnull RangeHighlighterImpl highlighter,
                                     @Nullable Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        myCachedHighlighters = null;
        if (changeAttributesAction != null) {
            highlighter.changeAttributesNoEvents(changeAttributesAction);
        }
        fireAfterAdded(highlighter);
    }

    @Override
    @RequiredUIAccess
    public void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction) {
        byte changeStatus = ((RangeHighlighterImpl) highlighter).changeAttributesNoEvents(changeAttributesAction);
        if (BitUtil.isSet(changeStatus, RangeHighlighterImpl.CHANGED_MASK)) {
            fireAttributesChanged(highlighter,
                BitUtil.isSet(changeStatus, RangeHighlighterImpl.RENDERERS_CHANGED_MASK),
                BitUtil.isSet(changeStatus, RangeHighlighterImpl.FONT_STYLE_CHANGED_MASK),
                BitUtil.isSet(changeStatus, RangeHighlighterImpl.FOREGROUND_COLOR_CHANGED_MASK));
        }
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
        return addRangeHighlighterAndChangeAttributes(textAttributesKey, startOffset, endOffset, layer, targetArea, false, null);
    }

    RangeHighlighterTree treeFor(RangeHighlighter marker) {
        return marker.getTargetArea() == HighlighterTargetArea.EXACT_RANGE ? myHighlighterTree : myHighlighterTreeForLines;
    }

    @Override
    @Nonnull
    public RangeHighlighter addRangeHighlighter(int startOffset,
                                                int endOffset,
                                                int layer,
                                                TextAttributes textAttributes,
                                                @Nonnull HighlighterTargetArea targetArea) {
        Consumer<RangeHighlighterEx> changeAction = textAttributes == null ? null : ex -> ex.setTextAttributes(textAttributes);
        return addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, layer, targetArea, false, changeAction);
    }

    public void addRangeHighlighter(@Nonnull RangeHighlighterEx marker,
                                    int start,
                                    int end,
                                    boolean greedyToLeft,
                                    boolean greedyToRight,
                                    int layer) {
        treeFor(marker).addInterval(marker, start, end, greedyToLeft, greedyToRight, false, layer);
    }

    @Override
    public void removeHighlighter(@Nonnull RangeHighlighter segmentHighlighter) {
        myCachedHighlighters = null;
        if (!segmentHighlighter.isValid()) {
            return;
        }

        boolean removed = treeFor(segmentHighlighter).removeInterval((RangeHighlighterEx) segmentHighlighter);
        LOG.assertTrue(removed);
    }

    @Override
    public void removeAllHighlighters() {
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

    @Override
    public void setRangeHighlighterAttributes(@Nonnull final RangeHighlighter highlighter, @Nonnull final TextAttributes textAttributes) {
        ((RangeHighlighterEx) highlighter).setTextAttributes(textAttributes);
    }

    /**
     * @deprecated use {@code RangeHighlighterEx.setXXX()} methods to fire changes
     */
    @Deprecated
    @Override
    public void fireAttributesChanged(@Nonnull RangeHighlighterEx highlighter,
                                      boolean renderersChanged,
                                      boolean fontStyleOrColorChanged) {
        fireAttributesChanged(highlighter, renderersChanged, fontStyleOrColorChanged, fontStyleOrColorChanged);
    }

    public void fireAttributesChanged(@Nonnull RangeHighlighterEx highlighter,
                                      boolean renderersChanged,
                                      boolean fontStyleChanged,
                                      boolean foregroundColorChanged) {
        if (highlighter.isValid()) {
            for (MarkupModelListener listener : myListeners) {
                listener.attributesChanged(highlighter, renderersChanged, fontStyleChanged, foregroundColorChanged);
            }
        }
    }

    @Override
    public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.afterAdded(segmentHighlighter);
        }
    }

    @Override
    public void fireBeforeRemoved(@Nonnull RangeHighlighterEx highlighter) {
        myCachedHighlighters = null;
        for (MarkupModelListener listener : myListeners) {
            listener.beforeRemoved(highlighter);
        }
    }

    public void fireAfterRemoved(@Nonnull RangeHighlighterEx highlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.afterRemoved(highlighter);
        }
    }

    @Override
    public boolean containsHighlighter(@Nonnull final RangeHighlighter highlighter) {
        Predicate<RangeHighlighterEx> equalId = h -> h.getId() != highlighter.getId();
        return !treeFor(highlighter).processOverlappingWith(highlighter.getStartOffset(), highlighter.getEndOffset(), equalId);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
        MarkupIterator<RangeHighlighterEx> iterator = overlappingIterator(start, end);
        try {
            while (iterator.hasNext()) {
                if (!processor.test(iterator.next())) {
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
    public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Predicate<? super RangeHighlighterEx> processor) {
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
        return IntervalTreeImpl
            .mergingOverlappingIterator(myHighlighterTree, new ProperTextRange(startOffset, endOffset), myHighlighterTreeForLines,
                roundToLineBoundaries(getDocument(), startOffset, endOffset), RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    }

    @Nonnull
    public static TextRangeInterval roundToLineBoundaries(@Nonnull Document document, int startOffset, int endOffset) {
        int textLength = document.getTextLength();
        int lineStartOffset = startOffset <= 0 ? 0 : startOffset > textLength ? textLength : document.getLineStartOffset(document.getLineNumber(startOffset));
        int lineEndOffset = endOffset <= 0 ? 0 : endOffset >= textLength ? textLength : document.getLineEndOffset(document.getLineNumber(endOffset));
        return new TextRangeInterval(lineStartOffset, lineEndOffset);
    }
}
