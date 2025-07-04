// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.LineWrapPositionStrategy;
import consulo.codeEditor.SoftWrap;
import consulo.codeEditor.SoftWrapDrawingType;
import consulo.codeEditor.internal.TextChangeImpl;
import consulo.codeEditor.impl.softwrap.SoftWrapImpl;
import consulo.codeEditor.impl.softwrap.SoftWrapPainter;
import consulo.codeEditor.impl.softwrap.SoftWrapsStorage;
import consulo.codeEditor.impl.softwrap.mapping.CachingSoftWrapDataMapper;
import consulo.codeEditor.impl.softwrap.mapping.IncrementalCacheUpdateEvent;
import consulo.desktop.awt.editor.impl.view.CharacterGrid;
import consulo.desktop.awt.editor.impl.view.EditorViewImpl;
import consulo.desktop.awt.editor.impl.view.WrapElementMeasuringIterator;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.editor.LanguageLineWrapPositionStrategy;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * Class that calculates soft wrap positions for a given text fragment and available visible width.
 */
public class SoftWrapEngine {
    private static final int BASIC_LOOK_BACK_LENGTH = 10;

    private final DesktopEditorImpl myEditor;
    private final Document myDocument;
    private final CharSequence myText;
    private final EditorViewImpl myView;
    private final SoftWrapsStorage myStorage;
    private final CachingSoftWrapDataMapper myDataMapper;
    private final int myVisibleWidth;
    private final int myMaxWidthAtWrap;
    private final int mySoftWrapWidth;
    private final IncrementalCacheUpdateEvent myEvent;
    private final int myRelativeIndent;

    private LineWrapPositionStrategy myLineWrapPositionStrategy;

    public SoftWrapEngine(@Nonnull DesktopEditorImpl editor,
                          @Nonnull SoftWrapPainter painter,
                          @Nonnull SoftWrapsStorage storage,
                          @Nonnull CachingSoftWrapDataMapper dataMapper,
                          @Nonnull IncrementalCacheUpdateEvent event,
                          int visibleWidth,
                          int relativeIndent) {
        myEditor = editor;
        myDocument = editor.getDocument();
        myText = myDocument.getImmutableCharSequence();
        myView = editor.myView;
        myStorage = storage;
        myDataMapper = dataMapper;
        myVisibleWidth = visibleWidth;
        myMaxWidthAtWrap = visibleWidth - painter.getMinDrawingWidth(SoftWrapDrawingType.BEFORE_SOFT_WRAP_LINE_FEED);
        mySoftWrapWidth = painter.getMinDrawingWidth(SoftWrapDrawingType.AFTER_SOFT_WRAP);
        myEvent = event;
        myRelativeIndent = relativeIndent;
    }

    public void generate() {
        int startOffset = myEvent.getStartOffset();
        int minEndOffset = myEvent.getMandatoryEndOffset();
        int maxEndOffset = getEndOffsetUpperEstimate();
        var inlineInlays = myEditor.getInlayModel().getInlineElementsInRange(startOffset, maxEndOffset);
        var afterLineEndInlays = ContainerUtil.filter(
            myEditor.getInlayModel().getAfterLineEndElementsInRange(DocumentUtil.getLineStartOffset(startOffset, myDocument), maxEndOffset),
            inlay -> !inlay.getProperties().isSoftWrappingDisabled()
        );
        var grid = myEditor.getCharacterGrid();
        if (grid != null && inlineInlays.isEmpty() && afterLineEndInlays.isEmpty()) {
            generateGridSoftWraps(grid, startOffset, minEndOffset, maxEndOffset);
            return;
        }

        SoftWrap lastSoftWrap = null;
        boolean minWrapOffsetAtFolding = false;
        int minWrapOffset = -1;
        int maxWrapOffset = -1;
        float nonWhitespaceStartX = 0;
        int nonWhitespaceStartOffset = -1;

        float x;
        if (startOffset == 0) {
            x = myView.getPrefixTextWidthInPixels();
        }
        else {
            lastSoftWrap = myStorage.getSoftWrap(startOffset);
            x = lastSoftWrap == null ? 0 : lastSoftWrap.getIndentInPixels();
        }

        WrapElementMeasuringIterator it = new WrapElementMeasuringIterator(myView, startOffset, maxEndOffset, inlineInlays, afterLineEndInlays);
        while (!it.atEnd()) {
            if (it.isLineBreak()) {
                minWrapOffset = -1;
                maxWrapOffset = -1;
                x = 0;
                lastSoftWrap = null;
                nonWhitespaceStartOffset = -1;
                if (it.getElementEndOffset() > minEndOffset) {
                    myEvent.setActualEndOffset(it.getElementEndOffset());
                    return;
                }
            }
            else {
                if (myRelativeIndent >= 0 && nonWhitespaceStartOffset == -1 && !it.isWhitespace()) {
                    nonWhitespaceStartX = x;
                    nonWhitespaceStartOffset = it.getElementStartOffset();
                }
                x = it.getElementEndX(x);
                if (minWrapOffset < 0 || x <= myMaxWidthAtWrap && it.isFoldRegion()) {
                    minWrapOffset = it.getElementEndOffset();
                    minWrapOffsetAtFolding = it.isFoldRegion();
                }
                else {
                    if (x > myMaxWidthAtWrap && maxWrapOffset < 0) {
                        maxWrapOffset = it.getElementStartOffset();
                        if (maxWrapOffset > minWrapOffset && it.isFoldRegion()) {
                            minWrapOffset = maxWrapOffset;
                        }
                    }
                    if (x > myVisibleWidth) {
                        lastSoftWrap = createSoftWrap(lastSoftWrap, minWrapOffset, maxWrapOffset, minWrapOffsetAtFolding,
                            nonWhitespaceStartOffset, nonWhitespaceStartX);
                        int wrapOffset = lastSoftWrap.getStart();
                        if (wrapOffset > minEndOffset && myDataMapper.matchesOldSoftWrap(lastSoftWrap, myEvent.getLengthDiff())) {
                            myEvent.setActualEndOffset(wrapOffset);
                            return;
                        }
                        minWrapOffset = -1;
                        maxWrapOffset = -1;
                        x = lastSoftWrap.getIndentInPixels();
                        if (wrapOffset <= it.getElementStartOffset()) {
                            it.retreat(wrapOffset);
                            continue;
                        }
                    }
                }
            }
            it.advance();
        }
        myEvent.setActualEndOffset(maxEndOffset);
    }

    private SoftWrap createSoftWrap(SoftWrap lastSoftWrap, int minWrapOffset, int maxWrapOffset, boolean preferMinOffset, int nonWhitespaceStartOffset, float nonWhitespaceStartX) {
        int wrapOffset = minWrapOffset >= maxWrapOffset ? minWrapOffset : calcSoftWrapOffset(minWrapOffset, maxWrapOffset, preferMinOffset);
        int indentInColumns = 1;
        int indentInPixels = mySoftWrapWidth;
        if (myRelativeIndent >= 0) {
            if (lastSoftWrap == null) {
                if (nonWhitespaceStartOffset >= 0 && nonWhitespaceStartOffset < wrapOffset) {
                    indentInColumns += myEditor.offsetToLogicalPosition(nonWhitespaceStartOffset).column;
                    indentInPixels += nonWhitespaceStartX;
                }
                indentInColumns += myRelativeIndent;
                indentInPixels += myRelativeIndent * myView.getPlainSpaceWidth();
            }
            else {
                indentInColumns = lastSoftWrap.getIndentInColumns();
                indentInPixels = lastSoftWrap.getIndentInPixels();
            }
        }
        SoftWrapImpl result = new SoftWrapImpl(new TextChangeImpl("\n" + StringUtil.repeatSymbol(' ', indentInColumns - 1), wrapOffset), indentInColumns, indentInPixels);
        myStorage.storeOrReplace(result);
        return result;
    }

    private void generateGridSoftWraps(CharacterGrid grid, int startOffset, int minEndOffset, int maxEndOffset) {
        throw new UnsupportedOperationException("CharacterGrid not supported");
    }

    private int calcSoftWrapOffset(int minOffset, int maxOffset, boolean preferMinOffset) {
        if (canBreakBeforeOrAfterCodePoint(Character.codePointAt(myText, maxOffset))) {
            return maxOffset;
        }
        for (int i = 0, offset = maxOffset; i < BASIC_LOOK_BACK_LENGTH && offset >= minOffset; i++) {
            int prevOffset = Character.offsetByCodePoints(myText, offset, -1);
            if (canBreakBeforeOrAfterCodePoint(Character.codePointAt(myText, prevOffset))) {
                return offset;
            }
            //noinspection AssignmentToForLoopParameter
            offset = prevOffset;
        }

        if (myLineWrapPositionStrategy == null) {
            myLineWrapPositionStrategy = LanguageLineWrapPositionStrategy.forEditor(myEditor);
        }

        int wrapOffset = myLineWrapPositionStrategy.calculateWrapPosition(myDocument, myEditor.getProject(), minOffset - 1, maxOffset + 1, maxOffset + 1, false, true);
        if (wrapOffset < 0) {
            return preferMinOffset ? minOffset : maxOffset;
        }
        if (wrapOffset < minOffset) {
            return minOffset;
        }
        if (wrapOffset > maxOffset) {
            return maxOffset;
        }
        if (DocumentUtil.isInsideSurrogatePair(myDocument, wrapOffset)) {
            return wrapOffset - 1;
        }
        return wrapOffset;
    }

    private static boolean canBreakBeforeOrAfterCodePoint(int codePoint) {
        return codePoint == ' ' || codePoint == '\t' || (codePoint >= 0x2f00 && codePoint < 0x10000 /* eastern languages unicode ranges */);
    }

    private int getEndOffsetUpperEstimate() {
        int endOffsetUpperEstimate = EditorUtil.getNotFoldedLineEndOffset(myEditor, myEvent.getMandatoryEndOffset());
        int line = myDocument.getLineNumber(endOffsetUpperEstimate);
        if (line < myDocument.getLineCount() - 1) {
            endOffsetUpperEstimate = myDocument.getLineStartOffset(line + 1);
        }
        return endOffsetUpperEstimate;
    }
}
