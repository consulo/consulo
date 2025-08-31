// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.codeEditor.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRangeScalarUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class FoldRegionImpl extends RangeMarkerWithGetterImpl implements FoldRegion {
    private static final Key<Boolean> MUTE_INNER_HIGHLIGHTERS = Key.create("mute.inner.highlighters");
    private static final Key<Boolean> SHOW_GUTTER_MARK_FOR_SINGLE_LINE = Key.create("show.gutter.mark.for.single.line");

    private boolean myIsExpanded;
    private final CodeEditorBase myEditor;
    private String myPlaceholderText;
    private final FoldingGroup myGroup;
    private final boolean myShouldNeverExpand;
    private boolean myDocumentRegionWasChanged;
    public int mySizeBeforeUpdate; // temporary field used during update on document change

    public FoldRegionImpl(@Nonnull CodeEditorBase editor, int startOffset, int endOffset, @Nonnull String placeholder, @Nullable FoldingGroup group, boolean shouldNeverExpand) {
        super(editor.getDocument(), startOffset, endOffset, false);
        myGroup = group;
        myShouldNeverExpand = shouldNeverExpand;
        myIsExpanded = true;
        myEditor = editor;
        myPlaceholderText = placeholder;
    }

    @Override
    public boolean isExpanded() {
        return myIsExpanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        setExpanded(expanded, true);
    }

    public void setExpanded(boolean expanded, boolean notify) {
        CodeEditorFoldingModelBase foldingModel = (CodeEditorFoldingModelBase) myEditor.getFoldingModel();
        if (myGroup == null) {
            doSetExpanded(expanded, foldingModel, this, notify);
        }
        else {
            for (FoldRegion region : foldingModel.getGroupedRegions(myGroup)) {
                doSetExpanded(expanded, foldingModel, region, notify || region != this);
                // There is a possible case that we can't change expanded status of particular fold region (e.g. we can't collapse
                // if it contains caret). So, we revert all changes for the fold regions from the same group then.
                if (region.isExpanded() != expanded) {
                    for (FoldRegion regionToRevert : foldingModel.getGroupedRegions(myGroup)) {
                        if (regionToRevert == region) {
                            break;
                        }
                        doSetExpanded(!expanded, foldingModel, regionToRevert, notify || region != this);
                    }
                    return;
                }
            }
        }
    }

    private static void doSetExpanded(boolean expanded, CodeEditorFoldingModelBase foldingModel, FoldRegion region, boolean notify) {
        if (expanded) {
            foldingModel.expandFoldRegion(region, notify);
        }
        else {
            foldingModel.collapseFoldRegion(region, notify);
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid() && intervalStart() < intervalEnd();
    }

    public void setExpandedInternal(boolean toExpand) {
        myIsExpanded = toExpand;
    }

    @Override
    @Nonnull
    public String getPlaceholderText() {
        return myPlaceholderText;
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    @Override
    @Nullable
    public FoldingGroup getGroup() {
        return myGroup;
    }

    @Override
    public boolean shouldNeverExpand() {
        return myShouldNeverExpand;
    }

    public boolean hasDocumentRegionChanged() {
        return myDocumentRegionWasChanged;
    }

    public void resetDocumentRegionChanged() {
        myDocumentRegionWasChanged = false;
    }

    @Override
    protected void changedUpdateImpl(@Nonnull DocumentEvent e) {
        if (isValid()) {
            int oldStart = intervalStart();
            int oldEnd = intervalEnd();
            int changeStart = e.getOffset();
            int changeEnd = e.getOffset() + e.getOldLength();
            if (changeStart < oldEnd && changeEnd > oldStart) {
                myDocumentRegionWasChanged = true;
            }
        }
        super.changedUpdateImpl(e);
        if (isValid()) {
            alignToValidBoundaries();
        }
    }

    @Override
    protected void onReTarget(@Nonnull DocumentEvent e) {
        alignToValidBoundaries();
    }

    private void alignToValidBoundaries() {
        Document document = getDocument();
        long alignedRange = TextRangeScalarUtil.shift(toScalarRange(),
            DocumentUtil.isInsideCharacterPair(document, getStartOffset()) ? -1 : 0,
            DocumentUtil.isInsideCharacterPair(document, getEndOffset()) ? -1 : 0);

        if (alignedRange != toScalarRange()) {
            myEditor.getFoldingModel().setComplexDocumentChange(true);
        }
        setRange(alignedRange);
    }

    @Override
    public void setInnerHighlightersMuted(boolean value) {
        putUserData(MUTE_INNER_HIGHLIGHTERS, value ? Boolean.TRUE : null);
    }

    @Override
    public boolean areInnerHighlightersMuted() {
        return Boolean.TRUE.equals(getUserData(MUTE_INNER_HIGHLIGHTERS));
    }

    @Override
    public void setGutterMarkEnabledForSingleLine(boolean value) {
        if (value != isGutterMarkEnabledForSingleLine()) {
            putUserData(SHOW_GUTTER_MARK_FOR_SINGLE_LINE, value ? Boolean.TRUE : null);
            myEditor.getGutterComponentEx().repaint();
        }
    }

    @Override
    public boolean isGutterMarkEnabledForSingleLine() {
        return Boolean.TRUE.equals(getUserData(SHOW_GUTTER_MARK_FOR_SINGLE_LINE));
    }

    @Override
    public void setPlaceholderText(@Nonnull String text) {
        myPlaceholderText = text;
        ((CodeEditorFoldingModelBase) myEditor.getFoldingModel()).onPlaceholderTextChanged(this);
    }

    @Override
    public void dispose() {
        ((CodeEditorFoldingModelBase) myEditor.getFoldingModel()).removeRegionFromTree(this);
    }

    @Override
    public String toString() {
        return "FoldRegion " + (myIsExpanded ? "-" : "+") + "(" + getStartOffset() + ":" + getEndOffset() + ")" + (isValid() ? "" : "(invalid)") + ", placeholder='" + myPlaceholderText + "'";
    }
}
