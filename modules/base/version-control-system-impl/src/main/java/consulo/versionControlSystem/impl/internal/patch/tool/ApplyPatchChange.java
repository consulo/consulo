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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.application.progress.DumbProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.FillColorLineMarkerPresentation;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.LineMarkerPresentationProvider;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.comparison.ByWord;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.impl.internal.merge.MergeModelBase;
import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.impl.internal.util.DiffGutterRenderer;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch.HunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ApplyPatchChange {
    private static final ColorValue ALREADY_APPLIED_COLOR = new LightDarkColorValue(
        ColorValueUtil.darker(StandardColors.YELLOW),
        ColorValueUtil.darker(new RGBColor(138, 138, 0))
    );
    private static final ColorValue EXACTLY_APPLIED_COLOR = new LightDarkColorValue(
        new RGBColor(0, 180, 5),
        new RGBColor(0, 147, 5)
    );
    private static final ColorValue NOT_APPLIED_COLOR = new LightDarkColorValue(
        ColorValueUtil.darker(StandardColors.RED),
        ColorValueUtil.darker(new RGBColor(188, 63, 60))
    );

    private final ApplyPatchViewer myViewer;
    private final int myIndex; // index in myModelChanges

    private final LineRange myPatchDeletionRange;

    private final LineRange myPatchInsertionRange;

    private final HunkStatus myStatus;

    private final @Nullable List<DiffFragment> myPatchInnerDifferences;

    private final List<MyGutterOperation> myOperations = new ArrayList<>();

    private final List<RangeHighlighter> myHighlighters = new ArrayList<>();

    private boolean myResolved;

    public ApplyPatchChange(PatchChangeBuilder.Hunk hunk, int index, ApplyPatchViewer viewer) {
        myIndex = index;
        myViewer = viewer;
        myPatchDeletionRange = hunk.patchDeletionRange();
        myPatchInsertionRange = hunk.patchInsertionRange();
        myStatus = hunk.status();

        myPatchInnerDifferences = calcPatchInnerDifferences(hunk, viewer);
    }

    private static @Nullable List<DiffFragment> calcPatchInnerDifferences(PatchChangeBuilder.Hunk hunk, ApplyPatchViewer viewer) {
        LineRange deletionRange = hunk.patchDeletionRange();
        LineRange insertionRange = hunk.patchInsertionRange();

        if (deletionRange.isEmpty() || insertionRange.isEmpty()) {
            return null;
        }

        try {
            Document patchDocument = viewer.getPatchEditor().getDocument();
            CharSequence deleted = DiffImplUtil.getLinesContent(patchDocument, deletionRange.start(), deletionRange.end());
            CharSequence inserted = DiffImplUtil.getLinesContent(patchDocument, insertionRange.start(), insertionRange.end());

            return ByWord.compare(deleted, inserted, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);
        }
        catch (DiffTooBigException ignore) {
            return null;
        }
    }

    @RequiredUIAccess
    public void reinstallHighlighters() {
        destroyHighlighters();
        installHighlighters();

        myViewer.updateBridge();
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

        int startLine = resultRange.start();
        int endLine = resultRange.end();

        TextDiffType type = getDiffType();
        boolean resolved = isRangeApplied();

        myHighlighters.addAll(DiffDrawUtil.createHighlighter(editor, startLine, endLine, type, false, resolved, false));
    }

    private void createStatusHighlighter() {
        int line1 = myPatchDeletionRange.start();
        int line2 = myPatchInsertionRange.end();

        ColorValue statusColor = getStatusColor();
        boolean resolved = isResolved();

        LocalizeValue tooltip = getStatusText();

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

        boolean navigable = getResultRange() != null;
        highlighter.setLineMarkerPresentationProvider(new LineMarkerPresentationProvider() {
            @Override
            public Set<EditorGutterArea> getUsedAreas() {
                return Set.of(EditorGutterArea.RIGHT_FREE_PAINTERS);
            }

            @Override
            public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
                ColorValue color = statusColor;
                if (resolved) {
                    ColorValue gutterBackground = context.getColor(EditorColors.GUTTER_BACKGROUND);
                    if (gutterBackground != null) {
                        color = ColorValueUtil.mix(color, gutterBackground, 0.6f);
                    }
                }

                return List.of(new FillColorLineMarkerPresentation(
                    line1,
                    line2,
                    EditorGutterArea.RIGHT_FREE_PAINTERS,
                    color,
                    null
                ));
            }

            @Override
            public LocalizeValue getTooltipValue(LineMarkerPresentation presentation) {
                return tooltip;
            }

            @Override
            public boolean canDoAction(LineMarkerPresentation presentation, InputDetails details) {
                return navigable;
            }

            @Override
            @RequiredUIAccess
            public void doAction(Editor editor, LineMarkerPresentation presentation, InputDetails details) {
                myViewer.scrollToChange(ApplyPatchChange.this, Side.RIGHT, false);
            }
        });

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

    public HunkStatus getStatus() {
        return myStatus;
    }

    public LineRange getPatchRange() {
        return new LineRange(myPatchDeletionRange.start(), myPatchInsertionRange.end());
    }

    public LineRange getPatchAffectedRange() {
        return isRangeApplied() ? myPatchInsertionRange : myPatchDeletionRange;
    }

    public LineRange getPatchDeletionRange() {
        return myPatchDeletionRange;
    }

    public LineRange getPatchInsertionRange() {
        return myPatchInsertionRange;
    }

    public @Nullable LineRange getResultRange() {
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

    public TextDiffType getDiffType() {
        return DiffImplUtil.getDiffType(!myPatchDeletionRange.isEmpty(), !myPatchInsertionRange.isEmpty());
    }

    public boolean isRangeApplied() {
        return myResolved || getStatus() == HunkStatus.ALREADY_APPLIED;
    }

    private LocalizeValue getStatusText() {
        return switch (myStatus) {
            case ALREADY_APPLIED -> LocalizeValue.localizeTODO("Already applied");
            case EXACTLY_APPLIED -> LocalizeValue.localizeTODO("Automatically applied");
            case NOT_APPLIED -> LocalizeValue.localizeTODO("Not applied");
            default -> throw new IllegalStateException();
        };
    }

    private ColorValue getStatusColor() {
        return switch (myStatus) {
            case ALREADY_APPLIED -> ALREADY_APPLIED_COLOR;
            case EXACTLY_APPLIED -> EXACTLY_APPLIED_COLOR;
            case NOT_APPLIED -> NOT_APPLIED_COLOR;
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

    private @Nullable MyGutterOperation createOperation(OperationType type) {
        if (isResolved()) {
            return null;
        }

        EditorEx editor = myViewer.getPatchEditor();
        Document document = editor.getDocument();

        int line = getPatchRange().start();
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
        private final RangeHighlighter myHighlighter;

        private final OperationType myType;

        private MyGutterOperation(RangeHighlighter highlighter, OperationType type) {
            myHighlighter = highlighter;
            myType = type;

            myHighlighter.setGutterIconRenderer(createRenderer());
        }

        public void dispose() {
            myHighlighter.dispose();
        }

        public @Nullable GutterIconRenderer createRenderer() {
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
            () -> myViewer.newPatchCommand()
                .name(DiffLocalize.mergeDialogAcceptChangeCommand())
                .run(() -> myViewer.replaceChange(this))
        );
    }

    private GutterIconRenderer createIgnoreRenderer() {
        return createIconRenderer(
            DiffLocalize.mergeDialogIgnoreChangeActionName(),
            PlatformIconGroup.diffRemove(),
            () -> myViewer.newPatchCommand()
                .name(DiffLocalize.mergeDialogIgnoreChangeCommand())
                .run(() -> myViewer.markChangeResolved(this))
        );
    }

    private static GutterIconRenderer createIconRenderer(LocalizeValue text, Image icon, @RequiredUIAccess Runnable perform) {
        String tooltipText = DiffImplUtil.createTooltipText(text.get(), null);
        return new DiffGutterRenderer(icon, tooltipText) {
            @Override
            @RequiredUIAccess
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

    public State storeState() {
        LineRange resultRange = getResultRange();
        return new State(
            myIndex,
            resultRange != null ? resultRange.start() : -1,
            resultRange != null ? resultRange.end() : -1,
            myResolved
        );
    }

    public void restoreState(State state) {
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
