// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.review.impl.internal.editor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayModel;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.editor.CodeReviewEditorDocumentUtil;
import consulo.review.editor.CodeReviewEditorGutterControlsModel;
import consulo.review.editor.CodeReviewEditorGutterControlsState;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewMultilineCommentableEditorModel;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The review controls of an editor gutter: the comment mark of every discussed line, and on the hovered line - or
 * the last line of a selection - the toggle of its discussions and the "+" starting a new comment, or the mark
 * cancelling the new comment already open there.
 * <p>
 * Every control is a gutter icon, which each frontend paints and clicks natively: the first
 * control of a line stands on the line, the next one on the inlay below it, and the hovered line is the one the
 * pointer is over in the gutter, which every frontend reports, rather than anywhere in the editor.
 */
public final class CodeReviewEditorGutterControlsRenderer implements Disposable {
    private final CodeReviewEditorGutterControlsModel myModel;
    private final Editor myEditor;

    private CodeReviewEditorGutterControlsState myState;

    private int myHoveredLogicalLine = -1;
    private boolean myColumnHovered;
    private int mySelectionStartLine = -1;
    private int mySelectionEndLine = -1;

    private final Map<CodeReviewGutterActionIconRenderer, RangeHighlighter> myHighlighters = new HashMap<>();
    private final Set<Inlay<?>> myInlaysWithIcons = new HashSet<>();

    private boolean myLayingOut;

    private final EditorMouseMotionListener myHoverHandler = new EditorMouseMotionListener() {
        @Override
        public void mouseMoved(EditorMouseEvent e) {
            EditorMouseEventArea area = e.getArea();
            boolean overGutter = area == EditorMouseEventArea.LINE_MARKERS_AREA
                || area == EditorMouseEventArea.LINE_NUMBERS_AREA
                || area == EditorMouseEventArea.ANNOTATIONS_AREA
                || area == EditorMouseEventArea.FOLDING_OUTLINE_AREA;

            int line = overGutter ? Math.max(0, e.getLogicalPosition().line) : -1;
            setHovered(line < myEditor.getDocument().getLineCount() ? line : -1, area == EditorMouseEventArea.LINE_MARKERS_AREA);
        }
    };

    private final EditorMouseListener myExitHandler = new EditorMouseListener() {
        @Override
        public void mouseExited(EditorMouseEvent e) {
            setHovered(-1, false);
        }
    };

    private final InlayModel.Listener myInlayListener = new InlayModel.Listener() {
        @Override
        public void onAdded(Inlay<?> inlay) {
            relayout();
        }

        @Override
        public void onRemoved(Inlay<?> inlay) {
            myInlaysWithIcons.remove(inlay);
            relayout();
        }
    };

    private final SelectionListener myMultilineCommentSelectionListener = new SelectionListener() {
        @Override
        public void selectionChanged(SelectionEvent e) {
            TextRange newRange = e.getNewRange();
            if (newRange.isEmpty()) {
                mySelectionStartLine = -1;
                mySelectionEndLine = -1;
            }
            else {
                Document document = myEditor.getDocument();
                mySelectionStartLine = document.getLineNumber(newRange.getStartOffset());
                mySelectionEndLine = document.getLineNumber(newRange.getEndOffset());
            }
            relayout();
        }
    };

    @RequiredUIAccess
    private CodeReviewEditorGutterControlsRenderer(CodeReviewEditorGutterControlsModel model,
                                                   Editor editor,
                                                   CodeReviewEditorGutterControlsState initialState) {
        myModel = model;
        myEditor = editor;
        myState = initialState;

        editor.addEditorMouseListener(myExitHandler, this);
        editor.addEditorMouseMotionListener(myHoverHandler);
        editor.getSelectionModel().addSelectionListener(myMultilineCommentSelectionListener, this);
        editor.getInlayModel().addListener(myInlayListener, this);

        relayout();
    }

    /**
     * Shows the controls of {@code model} while its state is known, until {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    public static void setupIn(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterControlsModel model) {
        AtomicReference<@Nullable CodeReviewEditorGutterControlsRenderer> renderer = new AtomicReference<>();
        ReviewUIUtil.bind(parentDisposable, model.getGutterControlsState(), state -> {
            CodeReviewEditorGutterControlsRenderer current = renderer.get();
            if (state != null) {
                if (current == null) {
                    current = new CodeReviewEditorGutterControlsRenderer(model, editor, state);
                    renderer.set(current);
                    Disposer.register(parentDisposable, current);
                }
                else {
                    current.setState(state);
                }
            }
            else if (current != null) {
                renderer.set(null);
                Disposer.dispose(current);
            }
        });
    }

    @RequiredUIAccess
    private void setState(CodeReviewEditorGutterControlsState state) {
        if (!state.equals(myState)) {
            myState = state;
            relayout();
        }
    }

    @RequiredUIAccess
    private void setHovered(int line, boolean columnHovered) {
        if (myHoveredLogicalLine == line && myColumnHovered == columnHovered) {
            return;
        }
        myHoveredLogicalLine = line;
        myColumnHovered = line >= 0 && columnHovered;
        relayout();
    }

    @Override
    public void dispose() {
        myEditor.removeEditorMouseMotionListener(myHoverHandler);
        for (RangeHighlighter highlighter : myHighlighters.values()) {
            highlighter.dispose();
        }
        myHighlighters.clear();

        for (Inlay<?> inlay : myInlaysWithIcons) {
            if (inlay.getRenderer() instanceof CodeReviewComponentInlayRenderer renderer) {
                renderer.setGutterIconRenderer(inlay, null);
            }
        }
        myInlaysWithIcons.clear();
    }

    /**
     * Puts the controls the current state asks for on the gutter, keeping the marks which did not change.
     */
    @RequiredUIAccess
    private void relayout() {
        if (myEditor.isDisposed() || myLayingOut) {
            return;
        }

        myLayingOut = true;
        try {
            Set<CodeReviewGutterActionIconRenderer> lineIcons = new LinkedHashSet<>();
            Map<Inlay<?>, CodeReviewGutterActionIconRenderer> inlayIcons = new LinkedHashMap<>();
            int lineCount = myEditor.getDocument().getLineCount();

            // comment icons on each line containing discussions
            for (int lineIdx : myState.getLinesWithComments()) {
                if (lineIdx >= 0 && lineIdx < lineCount && lineIdx != myHoveredLogicalLine) {
                    lineIcons.add(toggleCommentAction(lineIdx));
                }
            }

            // a new comment icon on hovered line if line is not folded and if there's enough vertical space
            if (myHoveredLogicalLine >= 0 && myHoveredLogicalLine < lineCount) {
                layoutActions(myHoveredLogicalLine, myColumnHovered, lineIcons, inlayIcons);
            }

            // a new comment icon on the last line of selected range as a setup for multiline comment
            if (mySelectionEndLine >= 0 && mySelectionEndLine < lineCount && mySelectionEndLine != myHoveredLogicalLine) {
                layoutActions(mySelectionEndLine, false, lineIcons, inlayIcons);
            }

            applyLineIcons(lineIcons);
            applyInlayIcons(inlayIcons);
        }
        finally {
            myLayingOut = false;
        }
    }

    private void layoutActions(int logicalLine,
                               boolean columnHovered,
                               Set<CodeReviewGutterActionIconRenderer> lineIcons,
                               Map<Inlay<?>, CodeReviewGutterActionIconRenderer> inlayIcons) {
        List<CodeReviewGutterActionIconRenderer> actions = getActions(logicalLine, columnHovered);
        if (actions.isEmpty()) {
            return;
        }

        int visualLine = myEditor.offsetToVisualPosition(myEditor.getDocument().getLineEndOffset(logicalLine), false, true).line;
        List<Inlay<?>> below = myEditor.getInlayModel().getBlockElementsForVisualLine(visualLine, false);

        for (int i = 0; i < actions.size(); i++) {
            CodeReviewGutterActionIconRenderer action = actions.get(i);

            @Nullable Inlay<?> inlay;
            if (action.getType() == CodeReviewGutterActionType.CLOSE_NEW_COMMENT) {
                // makes sure that icons don't overlap
                inlay = below.isEmpty() ? null : below.get(below.size() - 1);
            }
            else {
                inlay = i == 0 || below.isEmpty() ? null : below.get(0);
            }

            if (inlay != null) {
                if (inlay.getRenderer() instanceof CodeReviewComponentInlayRenderer) {
                    inlayIcons.put(inlay, action);
                }
            }
            else if (i == 0) {
                lineIcons.add(action);
            }
        }
    }

    @RequiredUIAccess
    private void applyLineIcons(Set<CodeReviewGutterActionIconRenderer> wanted) {
        Iterator<Map.Entry<CodeReviewGutterActionIconRenderer, RangeHighlighter>> iterator = myHighlighters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CodeReviewGutterActionIconRenderer, RangeHighlighter> entry = iterator.next();
            if (!wanted.contains(entry.getKey())) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }

        for (CodeReviewGutterActionIconRenderer renderer : wanted) {
            if (!myHighlighters.containsKey(renderer)) {
                RangeHighlighter highlighter =
                    myEditor.getMarkupModel().addLineHighlighter(renderer.getLine(), HighlighterLayer.LAST, null);
                highlighter.setGutterIconRenderer(renderer);
                myHighlighters.put(renderer, highlighter);
            }
        }
    }

    @RequiredUIAccess
    private void applyInlayIcons(Map<Inlay<?>, CodeReviewGutterActionIconRenderer> wanted) {
        for (Inlay<?> inlay : myInlaysWithIcons.toArray(new Inlay<?>[0])) {
            if (!wanted.containsKey(inlay) && inlay.getRenderer() instanceof CodeReviewComponentInlayRenderer renderer) {
                renderer.setGutterIconRenderer(inlay, null);
            }
        }
        myInlaysWithIcons.clear();

        for (Map.Entry<Inlay<?>, CodeReviewGutterActionIconRenderer> entry : wanted.entrySet()) {
            Inlay<?> inlay = entry.getKey();
            if (inlay.getRenderer() instanceof CodeReviewComponentInlayRenderer renderer) {
                renderer.setGutterIconRenderer(inlay, entry.getValue());
                myInlaysWithIcons.add(inlay);
            }
        }
    }

    private List<CodeReviewGutterActionIconRenderer> getActions(int logicalLine, boolean columnHovered) {
        List<CodeReviewGutterActionIconRenderer> actions = new ArrayList<>(2);
        if (myState.getLinesWithComments().contains(logicalLine)) {
            actions.add(toggleCommentAction(logicalLine));
        }

        if (isCommentable(logicalLine) && !hasCommentsUnderFoldedRegion(logicalLine)) {
            if (myState.getLinesWithNewComments().contains(logicalLine)) {
                actions.add(closeNewCommentAction(logicalLine));
            }
            else if (mySelectionEndLine < 0) {
                actions.add(startNewCommentAction(logicalLine, columnHovered));
            }
            // lines in the selected range are not commentable except the last one
            else if (logicalLine < mySelectionStartLine || logicalLine > mySelectionEndLine) {
                actions.add(startNewCommentAction(logicalLine, columnHovered));
            }
            else if (logicalLine == mySelectionEndLine) {
                actions.add(startNewCommentAction(logicalLine, columnHovered));
            }
        }
        return actions;
    }

    private CodeReviewGutterActionIconRenderer toggleCommentAction(int logicalLine) {
        return new CodeReviewGutterActionIconRenderer(logicalLine,
            CodeReviewGutterActionType.TOGGLE_COMMENT,
            ReviewIconGroup.comment(),
            ReviewLocalize.reviewEditorActionShowCommentsText(),
            () -> unfoldOrToggle(logicalLine));
    }

    private CodeReviewGutterActionIconRenderer closeNewCommentAction(int logicalLine) {
        return new CodeReviewGutterActionIconRenderer(logicalLine,
            CodeReviewGutterActionType.CLOSE_NEW_COMMENT,
            PlatformIconGroup.diffRemove(),
            ReviewLocalize.reviewCommentCancel(),
            () -> myModel.cancelNewComment(logicalLine));
    }

    private CodeReviewGutterActionIconRenderer startNewCommentAction(int logicalLine, boolean columnHovered) {
        return new CodeReviewGutterActionIconRenderer(logicalLine,
            CodeReviewGutterActionType.START_NEW_COMMENT,
            columnHovered ? PlatformIconGroup.generalInlineaddhover() : PlatformIconGroup.generalInlineadd(),
            ReviewLocalize.reviewEditorActionAddCommentText(),
            () -> requestNewComment(logicalLine));
    }

    @RequiredUIAccess
    private void unfoldOrToggle(int logicalLine) {
        FoldRegion foldedRegion = foldedRegion(logicalLine);
        if (foldedRegion != null) {
            myEditor.getFoldingModel().runBatchFoldingOperation(() -> foldedRegion.setExpanded(true));
        }
        else {
            myModel.toggleComments(logicalLine);
        }
    }

    @RequiredUIAccess
    private void requestNewComment(int logicalLine) {
        CodeReviewLineRange selectedRange = mySelectionStartLine >= 0 && logicalLine == mySelectionEndLine
            ? new CodeReviewLineRange(mySelectionStartLine, mySelectionEndLine)
            : null;

        Document document = myEditor.getDocument();
        if (myEditor.getCaretModel().getLogicalPosition().line != logicalLine) {
            myEditor.getCaretModel().moveToOffset(document.getLineEndOffset(logicalLine));
        }

        FoldingModel foldingModel = myEditor.getFoldingModel();
        FoldRegion collapsedRegion = foldingModel.getCollapsedRegionAtOffset(document.getLineEndOffset(logicalLine));
        if (collapsedRegion != null) {
            foldingModel.runBatchFoldingOperation(() -> collapsedRegion.setExpanded(true));
        }

        if (myModel instanceof CodeReviewMultilineCommentableEditorModel multilineModel && selectedRange != null) {
            if (multilineModel.canCreateComment(selectedRange)) {
                myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                multilineModel.requestNewComment(selectedRange);
                return;
            }
        }

        myModel.requestNewComment(logicalLine);
    }

    private @Nullable FoldRegion foldedRegion(int logicalLine) {
        return myEditor.getFoldingModel().getCollapsedRegionAtOffset(myEditor.getDocument().getLineEndOffset(logicalLine));
    }

    private boolean hasCommentsUnderFoldedRegion(int logicalLine) {
        FoldRegion region = foldedRegion(logicalLine);
        if (region == null) {
            return false;
        }

        Document document = myEditor.getDocument();
        int start = document.getLineNumber(region.getStartOffset());
        int end = document.getLineNumber(region.getEndOffset());
        for (int line = start; line <= end; line++) {
            if (myState.getLinesWithComments().contains(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommentable(int logicalLine) {
        if (CodeReviewEditorDocumentUtil.isLastBlankLine(myEditor.getDocument(), logicalLine)) {
            return false;
        }
        return myState.isLineCommentable(logicalLine);
    }
}
