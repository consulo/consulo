// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import consulo.application.AllIcons;
import consulo.codeEditor.*;
import consulo.codeEditor.event.*;
import consulo.codeEditor.markup.*;
import consulo.codeEditor.util.EditorUtil;
import consulo.diff.util.DiffUtil;
import consulo.diff.util.LineRange;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import icons.CollaborationToolsIcons;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.NonCancellable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * Draws and handles review controls in gutter
 */
public final class CodeReviewEditorGutterControlsRenderer
    implements LineMarkerRenderer, LineMarkerRendererEx, ActiveGutterRenderer, Disposable {

    private static final int ICON_AREA_WIDTH = 16;

    private final CodeReviewEditorGutterControlsModel model;
    private final EditorEx editor;
    private @Nullable Integer hoveredLogicalLine;
    private boolean columnHovered;
    private @Nullable LineRange selectedRangeForMultilineComment;

    private CodeReviewEditorGutterControlsModel.ControlsState state;

    private final HoverHandler hoverHandler;
    private final MultilineCommentSelectionListener multilineCommentSelectionListener;

    CodeReviewEditorGutterControlsRenderer(
        @Nonnull CodeReviewEditorGutterControlsModel model,
        @Nonnull EditorEx editor,
        @Nonnull CodeReviewEditorGutterControlsModel.ControlsState initialState
    ) {
        this.model = model;
        this.editor = editor;
        this.state = initialState;
        this.hoverHandler = new HoverHandler(editor);
        this.multilineCommentSelectionListener = new MultilineCommentSelectionListener();

        editor.addEditorMouseListener(hoverHandler);
        editor.addEditorMouseMotionListener(hoverHandler);
        editor.getSelectionModel().addSelectionListener(multilineCommentSelectionListener);

        editor.getGutterComponentEx().reserveLeftFreePaintersAreaWidth(this, ICON_AREA_WIDTH);
    }

    @RequiresEdt
    void setState(@Nonnull CodeReviewEditorGutterControlsModel.ControlsState newState) {
        CodeReviewEditorGutterControlsModel.ControlsState oldState = this.state;
        this.state = newState;
        if (!newState.equals(oldState)) {
            repaintColumn(editor, null);
        }
    }

    @Override
    public void dispose() {
        editor.removeEditorMouseListener(hoverHandler);
        editor.removeEditorMouseMotionListener(hoverHandler);
        editor.getSelectionModel().removeSelectionListener(multilineCommentSelectionListener);
    }

    @Override
    public void paint(@Nonnull Editor editor, @Nonnull Graphics g, @Nonnull Rectangle r) {
        if (!(editor instanceof EditorImpl editorImpl)) {
            return;
        }
        paintCommentIcons(editorImpl, g, r);
        paintHoveredLineIcons(editorImpl, g, r);
        paintIconForMultiline(editorImpl, g, r);
    }

    /**
     * Paint comment icons on each line containing discussion renderers
     */
    private void paintCommentIcons(@Nonnull EditorImpl editor, @Nonnull Graphics g, @Nonnull Rectangle r) {
        LogicalLineData hoveredData = hoverHandler.calcHoveredLineData();
        Integer hoveredLine = hoveredData != null ? hoveredData.logicalLine : null;
        Icon icon = EditorUIUtil.scaleIcon(CollaborationToolsIcons.Comment, editor);
        for (int lineIdx : state.getLinesWithComments()) {
            if (lineIdx >= 0 && lineIdx < editor.getDocument().getLineCount() && !Integer.valueOf(lineIdx).equals(hoveredLine)) {
                var yRange = EditorUtil.logicalLineToYRange(editor, lineIdx).getFirst();
                int lineCenter = yRange.intervalStart() + editor.getLineHeight() / 2;
                int y = lineCenter - icon.getIconWidth() / 2;
                icon.paintIcon(null, g, r.x, y);
            }
        }
    }

    /**
     * Paint a new comment icon on hovered line if line is not folded and if there's enough vertical space
     */
    private void paintHoveredLineIcons(@Nonnull EditorImpl editor, @Nonnull Graphics g, @Nonnull Rectangle r) {
        LogicalLineData lineData = hoverHandler.calcHoveredLineData();
        if (lineData == null) {
            return;
        }

        Map<GutterAction, int[]> actions = layoutActions(lineData, editor.getLineHeight());
        for (Map.Entry<GutterAction, int[]> entry : actions.entrySet()) {
            GutterAction action = entry.getKey();
            int[] yRange = entry.getValue();
            Icon rawIcon = lineData.columnHovered ? action.hoveredIcon : action.icon;
            Icon icon = EditorUIUtil.scaleIcon(rawIcon, editor);
            icon.paintIcon(null, g, r.x, yRange[0]);
        }
    }

    /**
     * Paint a new comment icon on the last line of selected range as a setup for multiline comment
     */
    private void paintIconForMultiline(@Nonnull EditorImpl editor, @Nonnull Graphics g, @Nonnull Rectangle r) {
        LineRange range = selectedRangeForMultilineComment;
        if (range == null) {
            return;
        }
        int selectionEndLine = range.end;
        if (selectionEndLine >= 0 && selectionEndLine < editor.getDocument().getLineCount()) {
            LogicalLineData lineData = new LogicalLineData(this.editor, state, selectionEndLine, false);
            Map<GutterAction, int[]> actions = layoutActions(lineData, editor.getLineHeight());
            for (Map.Entry<GutterAction, int[]> entry : actions.entrySet()) {
                GutterAction action = entry.getKey();
                int[] yRange = entry.getValue();
                Icon rawIcon = lineData.columnHovered ? action.hoveredIcon : action.icon;
                Icon icon = EditorUIUtil.scaleIcon(rawIcon, editor);
                icon.paintIcon(null, g, r.x, yRange[0]);
            }
        }
    }

    @Override
    public boolean canDoAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
        LogicalLineData lineData = hoverHandler.calcHoveredLineData();
        if (lineData == null) {
            return false;
        }
        if (!lineData.columnHovered) {
            return false;
        }

        Map<GutterAction, int[]> actions = layoutActions(lineData, editor.getLineHeight());
        for (int[] yRange : actions.values()) {
            if (e.getY() >= yRange[0] && e.getY() <= yRange[1]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
        LogicalLineData lineData = hoverHandler.calcHoveredLineData();
        if (lineData == null) {
            return;
        }
        if (!lineData.columnHovered) {
            return;
        }

        Map<GutterAction, int[]> actions = layoutActions(lineData, editor.getLineHeight());
        for (Map.Entry<GutterAction, int[]> entry : actions.entrySet()) {
            int[] yRange = entry.getValue();
            if (e.getY() >= yRange[0] && e.getY() <= yRange[1]) {
                entry.getKey().doAction.run();
                e.consume();
                return;
            }
        }
    }

    private @Nonnull Map<GutterAction, int[]> layoutActions(@Nonnull LogicalLineData lineData, int lineHeight) {
        List<GutterAction> actions = getActions(lineData);
        int[] yRangeWithInlays = lineData.getYRangeWithInlays();

        if (actions.isEmpty()) {
            return Collections.emptyMap();
        }

        int y = yRangeWithInlays[0];
        Map<GutterAction, int[]> result = new LinkedHashMap<>();
        for (GutterAction action : actions) {
            int iconHeight = action.icon.getIconHeight();
            int iconPadding = (lineHeight - iconHeight) / 2;

            if (action.actionType == GutterAction.ActionType.CLOSE_NEW_COMMENT) {
                int visualLine = editor.offsetToVisualLine(editor.getDocument().getLineEndOffset(lineData.logicalLine), true);
                var lastThreadList = editor.getInlayModel().getBlockElementsForVisualLine(visualLine, false);
                Inlay<?> lastThread = lastThreadList.isEmpty() ? null : lastThreadList.get(lastThreadList.size() - 1);

                int minY = lastThread != null && lastThread.getBounds() != null
                    ? lastThread.getBounds().y - iconPadding
                    : y;
                y = Math.max(y, minY) + JBUI.scale(CodeReviewChatItemUIUtil.THREAD_TOP_MARGIN);
            }

            int rangeStart = y + iconPadding;
            int rangeEnd = y + iconPadding + iconHeight;
            if (rangeEnd > yRangeWithInlays[1]) {
                continue;
            }

            result.put(action, new int[]{rangeStart, rangeEnd});
            y += lineHeight;
        }
        return result;
    }

    private void unfoldOrToggle(@Nonnull LogicalLineData lineData) {
        FoldRegion foldedRegion = lineData.getFoldedRegion();
        if (foldedRegion != null) {
            unfoldRegion(foldedRegion);
        }
        else {
            model.toggleComments(lineData.logicalLine);
        }
    }

    private void unfoldRegion(@Nonnull FoldRegion region) {
        if (region instanceof CustomFoldRegion cfr) {
            var renderer = cfr.getRenderer();
            if (renderer instanceof DocRenderer docRenderer) {
                docRenderer.getItem().toggle();
                return;
            }
        }
        else {
            editor.getFoldingModel().runBatchFoldingOperation(() -> region.setExpanded(true));
        }
    }

    @Override
    public @Nonnull LineMarkerRendererEx.Position getPosition() {
        return LineMarkerRendererEx.Position.LEFT;
    }

    private @Nonnull List<GutterAction> getActions(@Nonnull LogicalLineData lineData) {
        List<GutterAction> actions = new ArrayList<>();
        if (lineData.hasComments()) {
            actions.add(new GutterAction(CollaborationToolsIcons.Comment, GutterAction.ActionType.TOGGLE_COMMENT,
                CollaborationToolsIcons.Comment, () -> unfoldOrToggle(lineData)
            ));
        }
        if (lineData.isCommentable() && !lineData.hasCommentsUnderFoldedRegion()) {
            if (lineData.hasNewComment()) {
                actions.add(new GutterAction(
                    PlatformIconGroup.diffRemove(),
                    GutterAction.ActionType.CLOSE_NEW_COMMENT,
                    PlatformIconGroup.diffRemove(),
                    () -> {
                        model.cancelNewComment(lineData.logicalLine);
                        repaintColumn(editor, null);
                    }
                ));
            }
            else if (selectedRangeForMultilineComment == null) {
                actions.add(new GutterAction(
                    AllIcons.General.InlineAdd,
                    GutterAction.ActionType.START_NEW_COMMENT,
                    AllIcons.General.InlineAddHover,
                    () -> requestNewComment(lineData.logicalLine)
                ));
            }
            else if (lineData.logicalLine < selectedRangeForMultilineComment.start ||
                lineData.logicalLine > selectedRangeForMultilineComment.end) {
                actions.add(new GutterAction(
                    AllIcons.General.InlineAdd,
                    GutterAction.ActionType.START_NEW_COMMENT,
                    AllIcons.General.InlineAddHover,
                    () -> requestNewComment(lineData.logicalLine)
                ));
            }
            else if (lineData.logicalLine == selectedRangeForMultilineComment.end) {
                actions.add(new GutterAction(
                    AllIcons.General.InlineAdd,
                    GutterAction.ActionType.START_NEW_COMMENT,
                    AllIcons.General.InlineAddHover,
                    () -> requestNewComment(lineData.logicalLine)
                ));
            }
        }
        return actions;
    }

    private void requestNewComment(int logicalLine) {
        if (editor.getCaretModel().getLogicalPosition().line != logicalLine) {
            editor.getCaretModel().moveToOffset(editor.getDocument().getLineEndOffset(logicalLine));
        }

        int logicalPosition = editor.logicalPositionToOffset(
            new LogicalPosition(logicalLine, editor.getDocument().getLineEndOffset(logicalLine)));
        FoldRegion collapsedRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(logicalPosition);
        if (collapsedRegion != null) {
            editor.getFoldingModel().runBatchFoldingOperation(() -> collapsedRegion.setExpanded(true));
        }

        if (model instanceof CodeReviewCommentableEditorModel.WithMultilineComments multilineModel) {
            LineRange selectedRange = selectedRangeForMultilineComment;
            if (selectedRange != null && logicalLine == selectedRange.end && multilineModel.canCreateComment(selectedRange)) {
                var scrollingModel = editor.getScrollingModel();
                scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
                scrollingModel.runActionOnScrollingFinished(() -> multilineModel.requestNewComment(selectedRange));
                return;
            }
        }
        model.requestNewComment(logicalLine);
    }

    private static void repaintColumn(@Nonnull EditorEx editor, @Nullable Integer line) {
        int[] xRange = getIconColumnXRange(editor);
        int yStart;
        int yHeight;
        if (line != null && line > 0) {
            yStart = editor.logicalPositionToXY(new LogicalPosition(line, 0)).y;
            yHeight = editor.getLineHeight() * 2;
        }
        else {
            yStart = 0;
            yHeight = editor.getGutterComponentEx().getHeight();
        }
        editor.getGutterComponentEx().repaint(xRange[0], yStart, xRange[1] - xRange[0], yHeight);
    }

    private static int @Nonnull [] getIconColumnXRange(@Nonnull EditorEx editor) {
        var gutter = editor.getGutterComponentEx();
        int uiScaledIconAreaWidth = JBUIScale.scale(ICON_AREA_WIDTH);
        int iconAreaWidth;
        if (editor instanceof EditorImpl && ExperimentalUI.isNewUI()) {
            iconAreaWidth = EditorUIUtil.scaleWidth(uiScaledIconAreaWidth, (EditorImpl) editor) + 2;
        }
        else {
            iconAreaWidth = uiScaledIconAreaWidth;
        }
        int iconStart;
        if (editor.getVerticalScrollbarOrientation() == EditorEx.VERTICAL_SCROLLBAR_RIGHT) {
            iconStart = gutter.getLineMarkerAreaOffset();
        }
        else {
            iconStart = gutter.getWidth() - gutter.getLineMarkerAreaOffset() - iconAreaWidth;
        }
        int iconEnd = iconStart + iconAreaWidth - 1;
        return new int[]{iconStart, iconEnd};
    }

    private static boolean isIconColumnHovered(@Nonnull EditorEx editor, @Nonnull MouseEvent e) {
        if (e.getComponent() != editor.getGutter()) {
            return false;
        }
        int[] xRange = getIconColumnXRange(editor);
        return e.getX() >= xRange[0] && e.getX() <= xRange[1];
    }

    /**
     * Handles the hover state of the rendered icons.
     */
    private class HoverHandler implements EditorMouseListener, EditorMouseMotionListener {
        private final EditorEx editor;

        HoverHandler(@Nonnull EditorEx editor) {
            this.editor = editor;
        }

        @Nullable
        LogicalLineData calcHoveredLineData() {
            Integer line = hoveredLogicalLine;
            if (line == null || line < 0 || line >= editor.getDocument().getLineCount()) {
                return null;
            }
            return new LogicalLineData(editor, state, line, columnHovered);
        }

        @Override
        @RequiredUIAccess
        public void mouseMoved(@Nonnull EditorMouseEvent e) {
            int line = Math.max(0, e.getLogicalPosition().line);
            Integer prevLine = (line != (hoveredLogicalLine != null ? hoveredLogicalLine : -1)) ? hoveredLogicalLine : null;
            if (line >= 0 && line < DiffUtil.getLineCount(editor.getDocument())) {
                hoveredLogicalLine = line;
            }
            else {
                hoveredLogicalLine = null;
            }
            columnHovered = isIconColumnHovered(editor, e.getMouseEvent());
            if (prevLine != null) {
                repaintColumn(editor, prevLine);
            }
            repaintColumn(editor, e.getLogicalPosition().line);
        }

        @Override
        public void mouseExited(@Nonnull EditorMouseEvent e) {
            repaintColumn(editor, hoveredLogicalLine);
            hoveredLogicalLine = null;
            columnHovered = false;
        }
    }

    private class MultilineCommentSelectionListener implements SelectionListener {
        @Override
        public void selectionChanged(@Nonnull SelectionEvent e) {
            if (e.getNewRange().isEmpty()) {
                selectedRangeForMultilineComment = null;
                return;
            }
            var newRange = e.getNewRange();
            if (newRange.getLength() > 0) {
                selectedRangeForMultilineComment = new LineRange(
                    editor.offsetToLogicalPosition(newRange.getStartOffset()).line,
                    editor.offsetToLogicalPosition(newRange.getEndOffset()).line
                );
            }
            else {
                selectedRangeForMultilineComment = null;
            }
        }
    }

    private static final class LogicalLineData {
        final int logicalLine;
        final boolean columnHovered;

        private final EditorEx editor;
        private final CodeReviewEditorGutterControlsModel.ControlsState state;
        private final int lineStartOffset;
        private final int lineEndOffset;

        LogicalLineData(
            @Nonnull EditorEx editor,
            @Nonnull CodeReviewEditorGutterControlsModel.ControlsState state,
            int logicalLine,
            boolean columnHovered
        ) {
            this.editor = editor;
            this.state = state;
            this.logicalLine = logicalLine;
            this.columnHovered = columnHovered;
            this.lineStartOffset = editor.getDocument().getLineStartOffset(logicalLine);
            this.lineEndOffset = editor.getDocument().getLineEndOffset(logicalLine);
        }

        private int @Nonnull [] getYRange() {
            int startVisualLine = editor.offsetToVisualLine(lineStartOffset, false);
            int softWrapCount = editor.getSoftWrapModel().getSoftWrapsForRange(lineStartOffset + 1, lineEndOffset - 1).size();
            int endVisualLine = startVisualLine + softWrapCount;
            int startY = editor.visualLineToY(startVisualLine);
            int endY = (endVisualLine == startVisualLine ? startY : editor.visualLineToY(endVisualLine)) + editor.getLineHeight();
            return new int[]{startY, endY};
        }

        @Nullable
        FoldRegion getFoldedRegion() {
            return editor.getFoldingModel().getCollapsedRegionAtOffset(lineEndOffset);
        }

        int @Nonnull [] getYRangeWithInlays() {
            int[] yRange = getYRange();
            int visualLine = editor.offsetToVisualLine(lineEndOffset, false);
            int inlaysBelowHeight = 0;
            for (Inlay<?> inlay : editor.getInlayModel().getBlockElementsForVisualLine(visualLine, false)) {
                inlaysBelowHeight += inlay.getHeightInPixels();
            }
            return new int[]{yRange[0], yRange[1] + Math.max(0, inlaysBelowHeight)};
        }

        boolean hasComments() {
            return state.getLinesWithComments().contains(logicalLine);
        }

        boolean hasCommentsUnderFoldedRegion() {
            FoldRegion region = getFoldedRegion();
            if (region == null) {
                return false;
            }

            int frStart = editor.getDocument().getLineNumber(region.getStartOffset());
            int frEnd = editor.getDocument().getLineNumber(region.getEndOffset());

            for (int line = frStart; line <= frEnd; line++) {
                if (state.getLinesWithComments().contains(line)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasNewComment() {
            return state.getLinesWithNewComments().contains(logicalLine);
        }

        boolean isCommentable() {
            return state.isLineCommentable(logicalLine);
        }
    }

    private record GutterAction(@Nonnull Icon icon, @Nonnull ActionType actionType, @Nonnull Icon hoveredIcon,
                                @Nonnull Runnable doAction) {
        enum ActionType {
            TOGGLE_COMMENT,
            CLOSE_NEW_COMMENT,
            START_NEW_COMMENT
        }
    }

    /**
     * Suspending render method - kept for Kotlin coroutine callers.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object render(
        @Nonnull CodeReviewEditorGutterControlsModel model,
        @Nonnull EditorEx editor,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // This method delegates to a coroutine-based implementation.
        // It manages the lifecycle of an InstalledRenderer through the gutterControlsState flow.
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.getMain().immediate(),
            (scope, cont) -> {
                // This is a placeholder for the coroutine-based render method.
                // The actual implementation should be called from Kotlin code.
                return Unit.INSTANCE;
            },
            continuation
        );
    }

    static final class InstalledRenderer implements Disposable {
        private final CodeReviewEditorGutterControlsRenderer renderer;
        private final RangeHighlighter highlighter;

        InstalledRenderer(
            @Nonnull CodeReviewEditorGutterControlsModel model,
            @Nonnull EditorEx editor,
            @Nonnull CodeReviewEditorGutterControlsModel.ControlsState initialState
        ) {
            this.renderer = new CodeReviewEditorGutterControlsRenderer(model, editor, initialState);
            this.highlighter = editor.getMarkupModel().addRangeHighlighter(
                null, 0, editor.getDocument().getTextLength(),
                DiffDrawUtil.LST_LINE_MARKER_LAYER,
                HighlighterTargetArea.LINES_IN_RANGE
            );
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
            highlighter.setLineMarkerRenderer(renderer);
            Disposer.register(this, renderer);
        }

        void setState(@Nonnull CodeReviewEditorGutterControlsModel.ControlsState state) {
            renderer.setState(state);
        }

        @Override
        public void dispose() {
            highlighter.dispose();
        }
    }
}
