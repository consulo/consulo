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
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.application.AllIcons;
import consulo.application.progress.DumbProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.*;
import consulo.desktop.awt.internal.diff.merge.MergeModelBase;
import consulo.diff.comparison.ByWord;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.impl.internal.util.DiffGutterRenderer;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.diff.DiffDrawUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch.HunkStatus;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.tool.PatchChangeBuilder;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusMarkerRenderer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

class ApplyPatchChange {
    @Nonnull
    private final ApplyPatchViewer myViewer;
    private final int myIndex; // index in myModelChanges

    @Nonnull
    private final LineRange myPatchDeletionRange;
    @Nonnull
    private final LineRange myPatchInsertionRange;
    @Nonnull
    private final HunkStatus myStatus;

    @Nullable
    private final List<DiffFragment> myPatchInnerDifferences;
    @Nonnull
    private final List<MyGutterOperation> myOperations = new ArrayList<>();

    @Nonnull
    private final List<RangeHighlighter> myHighlighters = new ArrayList<>();

    private boolean myResolved;

    public ApplyPatchChange(@Nonnull PatchChangeBuilder.Hunk hunk, int index, @Nonnull ApplyPatchViewer viewer) {
        myIndex = index;
        myViewer = viewer;
        myPatchDeletionRange = hunk.getPatchDeletionRange();
        myPatchInsertionRange = hunk.getPatchInsertionRange();
        myStatus = hunk.getStatus();

        myPatchInnerDifferences = calcPatchInnerDifferences(hunk, viewer);
    }

    @Nullable
    private static List<DiffFragment> calcPatchInnerDifferences(
        @Nonnull PatchChangeBuilder.Hunk hunk,
        @Nonnull ApplyPatchViewer viewer
    ) {
        LineRange deletionRange = hunk.getPatchDeletionRange();
        LineRange insertionRange = hunk.getPatchInsertionRange();

        if (deletionRange.isEmpty() || insertionRange.isEmpty()) {
            return null;
        }

        try {
            Document patchDocument = viewer.getPatchEditor().getDocument();
            CharSequence deleted = DiffImplUtil.getLinesContent(patchDocument, deletionRange.start, deletionRange.end);
            CharSequence inserted = DiffImplUtil.getLinesContent(patchDocument, insertionRange.start, insertionRange.end);

            return ByWord.compare(deleted, inserted, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);
        }
        catch (DiffTooBigException ignore) {
            return null;
        }
    }

    public void reinstallHighlighters() {
        destroyHighlighters();
        installHighlighters();

        myViewer.repaintDivider();
    }

    private void installHighlighters() {
        createResultHighlighters();
        createPatchHighlighters();
        createStatusHighlighter();
        createOperations();
    }

    private void createPatchHighlighters() {
        EditorEx patchEditor = myViewer.getPatchEditor();
        myHighlighters.addAll(DiffDrawUtil.createUnifiedChunkHighlighters(
            patchEditor,
            myPatchDeletionRange,
            myPatchInsertionRange,
            myPatchInnerDifferences
        ));
    }

    private void createResultHighlighters() {
        LineRange resultRange = getResultRange();
        if (resultRange == null) {
            return;
        }
        EditorEx editor = myViewer.getResultEditor();

        int startLine = resultRange.start;
        int endLine = resultRange.end;

        TextDiffType type = getDiffType();
        boolean resolved = isRangeApplied();

        myHighlighters.addAll(DiffDrawUtil.createHighlighter(editor, startLine, endLine, type, false, resolved, false));
    }

    private void createStatusHighlighter() {
        int line1 = myPatchDeletionRange.start;
        int line2 = myPatchInsertionRange.end;

        Color color = getStatusColor();
        if (isResolved()) {
            color = ColorUtil.mix(color, myViewer.getPatchEditor().getGutterComponentEx().getComponent().getBackground(), 0.6f);
        }

        String tooltip = getStatusText();

        EditorEx patchEditor = myViewer.getPatchEditor();
        Document document = patchEditor.getDocument();
        MarkupModelEx markupModel = patchEditor.getMarkupModel();
        TextRange textRange = DiffImplUtil.getLinesRange(document, line1, line2);

        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
            textRange.getStartOffset(),
            textRange.getEndOffset(),
            HighlighterLayer.LAST,
            null,
            HighlighterTargetArea.LINES_IN_RANGE
        );

        BiConsumer<Editor, MouseEvent> clickHandler = getResultRange() != null
            ? (e, event) -> myViewer.scrollToChange(this, Side.RIGHT, false)
            : null;
        highlighter.setLineMarkerRenderer(LineStatusMarkerRenderer.createRenderer(
            line1,
            line2,
            TargetAWT.from(color),
            LocalizeValue.ofNullable(tooltip),
            clickHandler
        ));

        myHighlighters.add(highlighter);
    }

    private void destroyHighlighters() {
        for (RangeHighlighter highlighter : myHighlighters) {
            highlighter.dispose();
        }
        myHighlighters.clear();

        for (MyGutterOperation operation : myOperations) {
            operation.dispose();
        }
        myOperations.clear();
    }

    //
    // Getters
    //

    public int getIndex() {
        return myIndex;
    }

    @Nonnull
    public HunkStatus getStatus() {
        return myStatus;
    }

    @Nonnull
    public LineRange getPatchRange() {
        return new LineRange(myPatchDeletionRange.start, myPatchInsertionRange.end);
    }

    @Nonnull
    public LineRange getPatchAffectedRange() {
        return isRangeApplied() ? myPatchInsertionRange : myPatchDeletionRange;
    }

    @Nonnull
    public LineRange getPatchDeletionRange() {
        return myPatchDeletionRange;
    }

    @Nonnull
    public LineRange getPatchInsertionRange() {
        return myPatchInsertionRange;
    }

    @Nullable
    public LineRange getResultRange() {
        ApplyPatchViewer.MyModel model = myViewer.getModel();
        int lineStart = model.getLineStart(myIndex);
        int lineEnd = model.getLineEnd(myIndex);

        if (lineStart != -1 || lineEnd != -1) {
            return new LineRange(lineStart, lineEnd);
        }
        return null;
    }

    public boolean isResolved() {
        return myResolved;
    }

    public void setResolved(boolean resolved) {
        myResolved = resolved;
    }

    @Nonnull
    public TextDiffType getDiffType() {
        return DiffImplUtil.getDiffType(!myPatchDeletionRange.isEmpty(), !myPatchInsertionRange.isEmpty());
    }

    public boolean isRangeApplied() {
        return myResolved || getStatus() == HunkStatus.ALREADY_APPLIED;
    }

    @Nonnull
    private String getStatusText() {
        switch (myStatus) {
            case ALREADY_APPLIED:
                return "Already applied";
            case EXACTLY_APPLIED:
                return "Automatically applied";
            case NOT_APPLIED:
                return "Not applied";
            default:
                throw new IllegalStateException();
        }
    }

    @Nonnull
    private Color getStatusColor() {
        return switch (myStatus) {
            case ALREADY_APPLIED -> JBColor.YELLOW.darker();
            case EXACTLY_APPLIED -> new JBColor(new Color(0, 180, 5), new Color(0, 147, 5));
            case NOT_APPLIED -> JBColor.RED.darker();
            default -> throw new IllegalStateException();
        };
    }

    //
    // Operations
    //

    private void createOperations() {
        if (myViewer.isReadOnly()) {
            return;
        }
        if (isResolved()) {
            return;
        }

        if (myStatus == HunkStatus.EXACTLY_APPLIED) {
            ContainerUtil.addIfNotNull(myOperations, createOperation(OperationType.APPLY));
        }
        ContainerUtil.addIfNotNull(myOperations, createOperation(OperationType.IGNORE));
    }

    @Nullable
    private MyGutterOperation createOperation(@Nonnull OperationType type) {
        if (isResolved()) {
            return null;
        }

        EditorEx editor = myViewer.getPatchEditor();
        Document document = editor.getDocument();

        int line = getPatchRange().start;
        int offset = line == DiffImplUtil.getLineCount(document) ? document.getTextLength() : document.getLineStartOffset(line);

        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
            offset,
            offset,
            HighlighterLayer.ADDITIONAL_SYNTAX,
            null,
            HighlighterTargetArea.LINES_IN_RANGE
        );
        return new MyGutterOperation(highlighter, type);
    }

    private class MyGutterOperation {
        @Nonnull
        private final RangeHighlighter myHighlighter;
        @Nonnull
        private final OperationType myType;

        private MyGutterOperation(@Nonnull RangeHighlighter highlighter, @Nonnull OperationType type) {
            myHighlighter = highlighter;
            myType = type;

            myHighlighter.setGutterIconRenderer(createRenderer());
        }

        public void dispose() {
            myHighlighter.dispose();
        }

        @Nullable
        public GutterIconRenderer createRenderer() {
            return switch (myType) {
                case APPLY -> createApplyRenderer();
                case IGNORE -> createIgnoreRenderer();
                default -> throw new IllegalArgumentException(myType.name());
            };
        }
    }

    private GutterIconRenderer createApplyRenderer() {
        return createIconRenderer(
            DiffLocalize.mergeDialogApplyChangeActionName(),
            DiffImplUtil.getArrowIcon(Side.RIGHT),
            () -> DiffImplUtil.executeWriteCommand(
                myViewer.patchCommand(() -> myViewer.replaceChange(this))
                    .name(DiffLocalize.mergeDialogAcceptChangeCommand())
            )
        );
    }

    private GutterIconRenderer createIgnoreRenderer() {
        return createIconRenderer(
            DiffLocalize.mergeDialogIgnoreChangeActionName(),
            AllIcons.Diff.Remove,
            () -> DiffImplUtil.executeWriteCommand(
                myViewer.patchCommand(() -> myViewer.markChangeResolved(this))
                    .name(DiffLocalize.mergeDialogIgnoreChangeCommand())
            )
        );
    }

    @Nonnull
    private static GutterIconRenderer createIconRenderer(
        @Nonnull LocalizeValue text,
        @Nonnull Image icon,
        @Nonnull final Runnable perform
    ) {
        final String tooltipText = DiffImplUtil.createTooltipText(text.get(), null);
        return new DiffGutterRenderer(icon, tooltipText) {
            @Override
            protected void performAction(AnActionEvent e) {
                perform.run();
            }
        };
    }

    private enum OperationType {
        APPLY,
        IGNORE
    }

    //
    // State
    //

    @Nonnull
    public State storeState() {
        LineRange resultRange = getResultRange();
        return new State(
            myIndex,
            resultRange != null ? resultRange.start : -1,
            resultRange != null ? resultRange.end : -1,
            myResolved
        );
    }

    public void restoreState(@Nonnull State state) {
        myResolved = state.myResolved;
    }

    public static class State extends MergeModelBase.State {
        private final boolean myResolved;

        public State(int index, int startLine, int endLine, boolean resolved) {
            super(index, startLine, endLine);
            myResolved = resolved;
        }
    }
}
