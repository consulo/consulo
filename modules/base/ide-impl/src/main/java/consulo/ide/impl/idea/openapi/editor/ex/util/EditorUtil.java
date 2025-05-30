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
package consulo.ide.impl.idea.openapi.editor.ex.util;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.event.*;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentBulkUpdateListener;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.TextEditorImpl;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

public final class EditorUtil {
    private static final Logger LOG = Logger.getInstance(EditorUtil.class);

    private EditorUtil() {
    }

    /**
     * @return true if the editor is in fact an ordinary file editor;
     * false if the editor is part of EditorTextField, CommitMessage and etc.
     */
    public static boolean isRealFileEditor(@Nullable Editor editor) {
        return editor != null && TextEditorProvider.getInstance().getTextEditor(editor) instanceof TextEditorImpl;
    }

    public static boolean isPasswordEditor(@Nullable Editor editor) {
        return editor != null && editor.getContentComponent() instanceof JPasswordField;
    }

    @Nullable
    public static EditorEx getEditorEx(@Nullable FileEditor fileEditor) {
        Editor editor = fileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
        return editor instanceof EditorEx editorEx ? editorEx : null;
    }

    public static int getLastVisualLineColumnNumber(@Nonnull Editor editor, final int line) {
        return EditorImplUtil.getLastVisualLineColumnNumber(editor, line);
    }

    public static int getVisualLineEndOffset(@Nonnull Editor editor, int line) {
        VisualPosition endLineVisualPosition = new VisualPosition(line, getLastVisualLineColumnNumber(editor, line));
        LogicalPosition endLineLogicalPosition = editor.visualToLogicalPosition(endLineVisualPosition);
        return editor.logicalPositionToOffset(endLineLogicalPosition);
    }

    public static float calcVerticalScrollProportion(@Nonnull Editor editor) {
        Rectangle viewArea = editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
        if (viewArea.height == 0) {
            return 0;
        }
        LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
        Point location = editor.logicalPositionToXY(pos);
        return (location.y - viewArea.y) / (float) viewArea.height;
    }

    public static void setVerticalScrollProportion(@Nonnull Editor editor, float proportion) {
        Rectangle viewArea = editor.getScrollingModel().getVisibleArea();
        LogicalPosition caretPosition = editor.getCaretModel().getLogicalPosition();
        Point caretLocation = editor.logicalPositionToXY(caretPosition);
        int yPos = caretLocation.y;
        yPos -= viewArea.height * proportion;
        editor.getScrollingModel().scrollVertically(yPos);
    }

    public static int calcRelativeCaretPosition(@Nonnull Editor editor) {
        int caretY = editor.getCaretModel().getVisualPosition().line * editor.getLineHeight();
        int viewAreaPosition = editor.getScrollingModel().getVisibleAreaOnScrollingFinished().y;
        return caretY - viewAreaPosition;
    }

    public static void setRelativeCaretPosition(@Nonnull Editor editor, int position) {
        int caretY = editor.getCaretModel().getVisualPosition().line * editor.getLineHeight();
        editor.getScrollingModel().scrollVertically(caretY - position);
    }

    public static void fillVirtualSpaceUntilCaret(@Nonnull Editor editor) {
        final LogicalPosition position = editor.getCaretModel().getLogicalPosition();
        fillVirtualSpaceUntil(editor, position.column, position.line);
    }

    public static void fillVirtualSpaceUntil(@Nonnull final Editor editor, int columnNumber, int lineNumber) {
        final int offset = editor.logicalPositionToOffset(new LogicalPosition(lineNumber, columnNumber));
        final String filler = EditorModificationUtil.calcStringToFillVirtualSpace(editor);
        if (!filler.isEmpty()) {
            WriteAction.run(() -> {
                editor.getDocument().insertString(offset, filler);
                editor.getCaretModel().moveToOffset(offset + filler.length());
            });
        }
    }

    public static int calcColumnNumber(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int offset) {
        return calcColumnNumber(editor, text, start, offset, getTabSize(editor));
    }

    public static int calcColumnNumber(@Nullable Editor editor,
                                       @Nonnull CharSequence text,
                                       final int start,
                                       final int offset,
                                       final int tabSize) {
        return EditorImplUtil.calcColumnNumber(editor, text, start, offset, tabSize);
    }

    public static void setHandCursor(@Nonnull Editor view) {
        Cursor c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        // XXX: Workaround, simply view.getContentComponent().setCursor(c) doesn't work
        if (view.getContentComponent().getCursor() != c) {
            view.getContentComponent().setCursor(c);
        }
    }

    @Nonnull
    public static FontInfo fontForChar(final char c, @JdkConstants.FontStyle int style, @Nonnull Editor editor) {
        return EditorImplUtil.fontForChar(c, style, editor);
    }

    public static Image scaleIconAccordingEditorFont(@Nonnull Image icon, Editor editor) {
        if (Registry.is("editor.scale.gutter.icons") && editor instanceof RealEditor) {
            float scale = ((RealEditor) editor).getScale();
            if (Math.abs(1f - scale) > 0.1f) {
                return ImageEffects.resize(icon, scale);
            }
        }
        return icon;
    }

    public static int charWidth(char c, @JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
        return EditorImplUtil.charWidth(c, fontType, editor);
    }

    public static int getSpaceWidth(@JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
        return EditorImplUtil.getSpaceWidth(fontType, editor);
    }

    public static int getPlainSpaceWidth(@Nonnull Editor editor) {
        return getSpaceWidth(Font.PLAIN, editor);
    }

    public static int getTabSize(@Nonnull Editor editor) {
        return consulo.codeEditor.util.EditorUtil.getTabSize(editor);
    }

    public static int nextTabStop(int x, @Nonnull Editor editor) {
        int tabSize = getTabSize(editor);
        if (tabSize <= 0) {
            tabSize = 1;
        }
        return nextTabStop(x, editor, tabSize);
    }

    public static int nextTabStop(int x, @Nonnull Editor editor, int tabSize) {
        int leftInset = editor.getContentComponent().getInsets().left;
        return nextTabStop(x - leftInset, getSpaceWidth(Font.PLAIN, editor), tabSize) + leftInset;
    }

    public static int nextTabStop(int x, int plainSpaceWidth, int tabSize) {
        if (tabSize <= 0) {
            return x + plainSpaceWidth;
        }
        tabSize *= plainSpaceWidth;

        int nTabs = x / tabSize;
        return (nTabs + 1) * tabSize;
    }

    public static float nextTabStop(float x, float plainSpaceWidth, int tabSize) {
        if (tabSize <= 0) {
            return x + plainSpaceWidth;
        }
        tabSize *= plainSpaceWidth;

        int nTabs = (int) (x / tabSize);
        return (nTabs + 1) * tabSize;
    }

    public static int textWidthInColumns(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int end, int x) {
        return EditorImplUtil.textWidthInColumns(editor, text, start, end, x);
    }

    /**
     * Allows to answer how many visual columns are occupied by the given width.
     *
     * @param width          target width
     * @param plainSpaceSize width of the single space symbol within the target editor (in plain font style)
     * @return number of visual columns are occupied by the given width
     */
    public static int columnsNumber(int width, int plainSpaceSize) {
        int result = width / plainSpaceSize;
        if (width % plainSpaceSize > 0) {
            result++;
        }
        return result;
    }

    public static int columnsNumber(float width, float plainSpaceSize) {
        return (int) Math.ceil(width / plainSpaceSize);
    }

    /**
     * Allows to answer what width in pixels is required to draw fragment of the given char array from <code>[start; end)</code> interval
     * at the given editor.
     * <p>
     * Tabulation symbols is processed specially, i.e. it's ta
     * <p>
     * <b>Note:</b> it's assumed that target text fragment remains to the single line, i.e. line feed symbols within it are not
     * treated specially.
     *
     * @param editor   editor that will be used for target text representation
     * @param text     target text holder
     * @param start    offset within the given char array that points to target text start (inclusive)
     * @param end      offset within the given char array that points to target text end (exclusive)
     * @param fontType font type to use for target text representation
     * @param x        <code>'x'</code> coordinate that should be used as a starting point for target text representation.
     *                 It's necessity is implied by the fact that IDEA editor may represent tabulation symbols in any range
     *                 from <code>[1; tab size]</code> (check {@link #nextTabStop(int, Editor)} for more details)
     * @return width in pixels required for target text representation
     */
    public static int textWidth(@Nonnull Editor editor,
                                @Nonnull CharSequence text,
                                int start,
                                int end,
                                @JdkConstants.FontStyle int fontType,
                                int x) {
        int result = 0;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c != '\t') {
                FontInfo font = fontForChar(c, fontType, editor);
                result += font.charWidth(c);
                continue;
            }

            result += nextTabStop(x + result, editor) - result - x;
        }
        return result;
    }

    /**
     * Delegates to the {@link #calcSurroundingRange(Editor, VisualPosition, VisualPosition)} with the
     * {@link CaretModel#getVisualPosition() caret visual position} as an argument.
     *
     * @param editor target editor
     * @return surrounding logical positions
     * @see #calcSurroundingRange(Editor, VisualPosition, VisualPosition)
     */
    public static consulo.util.lang.Pair<LogicalPosition, LogicalPosition> calcCaretLineRange(@Nonnull Editor editor) {
        return consulo.codeEditor.util.EditorUtil.calcCaretLineRange(editor);
    }

    public static consulo.util.lang.Pair<LogicalPosition, LogicalPosition> calcCaretLineRange(@Nonnull Caret caret) {
        return consulo.codeEditor.util.EditorUtil.calcCaretLineRange(caret);
    }

    /**
     * Calculates logical positions that surround given visual positions and conform to the following criteria:
     * <pre>
     * <ul>
     *   <li>located at the start or the end of the visual line;</li>
     *   <li>doesn't have soft wrap at the target offset;</li>
     * </ul>
     * </pre>
     * Example:
     * <pre>
     *   first line [soft-wrap] some [start-position] text [end-position] [fold-start] fold line 1
     *   fold line 2
     *   fold line 3[fold-end] [soft-wrap] end text
     * </pre>
     * The very first and the last positions will be returned here.
     *
     * @param editor target editor to use
     * @param start  target start coordinate
     * @param end    target end coordinate
     * @return pair of the closest surrounding non-soft-wrapped logical positions for the visual line start and end
     * @see #getNotFoldedLineStartOffset(Editor, int)
     * @see #getNotFoldedLineEndOffset(Editor, int)
     */
    @SuppressWarnings("AssignmentToForLoopParameter")
    public static consulo.util.lang.Pair<LogicalPosition, LogicalPosition> calcSurroundingRange(@Nonnull Editor editor,
                                                                                                @Nonnull VisualPosition start,
                                                                                                @Nonnull VisualPosition end) {
        return consulo.codeEditor.util.EditorUtil.calcSurroundingRange(editor, start, end);
    }

    /**
     * Finds the start offset of visual line at which given offset is located, not taking soft wraps into account.
     */
    public static int getNotFoldedLineStartOffset(@Nonnull Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.getNotFoldedLineStartOffset(editor, offset);
    }

    /**
     * Finds the end offset of visual line at which given offset is located, not taking soft wraps into account.
     */
    public static int getNotFoldedLineEndOffset(@Nonnull Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.getNotFoldedLineEndOffset(editor, offset);
    }

    public static void scrollToTheEnd(@Nonnull Editor editor) {
        scrollToTheEnd(editor, false);
    }

    public static void scrollToTheEnd(@Nonnull Editor editor, boolean preferVerticalScroll) {
        editor.getSelectionModel().removeSelection();
        Document document = editor.getDocument();
        int lastLine = Math.max(0, document.getLineCount() - 1);
        boolean caretWasAtLastLine = editor.getCaretModel().getLogicalPosition().line == lastLine;
        editor.getCaretModel().moveToOffset(document.getTextLength());
        ScrollingModel scrollingModel = editor.getScrollingModel();
        if (preferVerticalScroll && document.getLineStartOffset(lastLine) == document.getLineEndOffset(lastLine)) {
            // don't move 'focus' to empty last line
            int scrollOffset;
            if (editor instanceof EditorEx) {
                JScrollBar verticalScrollBar = ((EditorEx) editor).getScrollPane().getVerticalScrollBar();
                scrollOffset = verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
            }
            else {
                scrollOffset = editor.getContentComponent().getHeight() - scrollingModel.getVisibleArea().height;
            }
            scrollingModel.scrollVertically(scrollOffset);
        }
        else if (!caretWasAtLastLine) {
            // don't scroll to the end of the last line (IDEA-124688)...
            scrollingModel.scrollTo(new LogicalPosition(lastLine, 0), ScrollType.RELATIVE);
        }
        else {
            // ...unless the caret was already on the last line - then scroll to the end of it.
            scrollingModel.scrollToCaret(ScrollType.RELATIVE);
        }
    }

    public static boolean isChangeFontSize(@Nonnull MouseWheelEvent e) {
        if (e.getWheelRotation() == 0) {
            return false;
        }
        return Platform.current().os().isMac() ? !e.isControlDown() && e.isMetaDown() && !e.isAltDown() && !e.isShiftDown() : e.isControlDown() && !e.isMetaDown() && !e
            .isAltDown() && !e.isShiftDown();
    }

    public static boolean inVirtualSpace(@Nonnull Editor editor, @Nonnull LogicalPosition logicalPosition) {
        return consulo.codeEditor.util.EditorUtil.inVirtualSpace(editor, logicalPosition);
    }

    public static void reinitSettings() {
        EditorFactory.getInstance().refreshAllEditors();
    }

    @Nonnull
    public static TextRange getSelectionInAnyMode(Editor editor) {
        SelectionModel selection = editor.getSelectionModel();
        int[] starts = selection.getBlockSelectionStarts();
        int[] ends = selection.getBlockSelectionEnds();
        int start = starts.length > 0 ? starts[0] : selection.getSelectionStart();
        int end = ends.length > 0 ? ends[ends.length - 1] : selection.getSelectionEnd();
        return TextRange.create(start, end);
    }

    public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull MouseEvent event) {
        return yPositionToLogicalLine(editor, event.getY());
    }

    public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull Point point) {
        return yPositionToLogicalLine(editor, point.y);
    }

    public static int yPositionToLogicalLine(@Nonnull Editor editor, int y) {
        return consulo.codeEditor.util.EditorUtil.yPositionToLogicalLine(editor, y);
    }

    public static boolean isAtLineEnd(@Nonnull Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.isAtLineEnd(editor, offset);
    }

    /**
     * Setting selection using {@link SelectionModel#setSelection(int, int)} or {@link Caret#setSelection(int, int)} methods can result
     * in resulting selection range to be larger than requested (in case requested range intersects with collapsed fold regions).
     * This method will make sure interfering collapsed regions are expanded first, so that resulting selection range is exactly as
     * requested.
     */
    public static void setSelectionExpandingFoldedRegionsIfNeeded(@Nonnull Editor editor, int startOffset, int endOffset) {
        FoldingModel foldingModel = editor.getFoldingModel();
        FoldRegion startFoldRegion = foldingModel.getCollapsedRegionAtOffset(startOffset);
        if (startFoldRegion != null && (startFoldRegion.getStartOffset() == startOffset || startFoldRegion.isExpanded())) {
            startFoldRegion = null;
        }
        FoldRegion endFoldRegion = foldingModel.getCollapsedRegionAtOffset(endOffset);
        if (endFoldRegion != null && (endFoldRegion.getStartOffset() == endOffset || endFoldRegion.isExpanded())) {
            endFoldRegion = null;
        }
        if (startFoldRegion != null || endFoldRegion != null) {
            final FoldRegion finalStartFoldRegion = startFoldRegion;
            final FoldRegion finalEndFoldRegion = endFoldRegion;
            foldingModel.runBatchFoldingOperation(() -> {
                if (finalStartFoldRegion != null) {
                    finalStartFoldRegion.setExpanded(true);
                }
                if (finalEndFoldRegion != null) {
                    finalEndFoldRegion.setExpanded(true);
                }
            });
        }
        editor.getSelectionModel().setSelection(startOffset, endOffset);
    }

    public static Font getEditorFont() {
        return AWTLanguageEditorUtil.getEditorFont();
    }

    public static int getDefaultCaretWidth() {
        return Registry.intValue("editor.caret.width", 2);
    }

    /**
     * Number of virtual soft wrap introduced lines on a current logical line before the visual position that corresponds
     * to the current logical position.
     *
     * @see LogicalPosition#softWrapLinesOnCurrentLogicalLine
     */
    public static int getSoftWrapCountAfterLineStart(@Nonnull Editor editor, @Nonnull LogicalPosition position) {
        return consulo.codeEditor.util.EditorUtil.getSoftWrapCountAfterLineStart(editor, position);
    }

    public static boolean attributesImpactFontStyleOrColor(@Nullable TextAttributes attributes) {
        return consulo.codeEditor.util.EditorUtil.attributesImpactFontStyleOrColor(attributes);
    }

    public static boolean isCurrentCaretPrimary(@Nonnull Editor editor) {
        return editor.getCaretModel().getCurrentCaret() == editor.getCaretModel().getPrimaryCaret();
    }

    @RequiredUIAccess
    public static void disposeWithEditor(@Nonnull Editor editor, @Nonnull Disposable disposable) {
        UIAccess.assertIsUIThread();
        if (Disposer.isDisposed(disposable)) {
            return;
        }
        if (editor.isDisposed()) {
            Disposer.dispose(disposable);
            return;
        }
        // for injected editors disposal will happen only when host editor is disposed,
        // but this seems to be the best we can do (there are no notifications on disposal of injected editor)
        Editor hostEditor = editor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : editor;
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            @Override
            public void editorReleased(@Nonnull EditorFactoryEvent event) {
                if (event.getEditor() == hostEditor) {
                    Disposer.dispose(disposable);
                }
            }
        }, disposable);
    }

    public static void runBatchFoldingOperationOutsideOfBulkUpdate(@Nonnull Editor editor, @Nonnull Runnable operation) {
        DocumentEx document = ObjectUtil.tryCast(editor.getDocument(), DocumentEx.class);
        if (document != null && document.isInBulkUpdate()) {
            MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
            disposeWithEditor(editor, connection::disconnect);
            connection.subscribe(DocumentBulkUpdateListener.class, new DocumentBulkUpdateListener.Adapter() {
                @Override
                public void updateFinished(@Nonnull Document doc) {
                    if (doc == editor.getDocument()) {
                        editor.getFoldingModel().runBatchFoldingOperation(operation);
                        connection.disconnect();
                    }
                }
            });
        }
        else {
            editor.getFoldingModel().runBatchFoldingOperation(operation);
        }
    }

    public static void runWithAnimationDisabled(@Nonnull Editor editor, @Nonnull Runnable taskWithScrolling) {
        ScrollingModel scrollingModel = editor.getScrollingModel();
        if (!(scrollingModel instanceof CodeEditorScrollingModelBase)) {
            taskWithScrolling.run();
        }
        else {
            boolean animationWasEnabled = scrollingModel.isAnimationEnabled();
            scrollingModel.disableAnimation();
            try {
                taskWithScrolling.run();
            }
            finally {
                if (animationWasEnabled) {
                    scrollingModel.enableAnimation();
                }
            }
        }
    }

    public static boolean isPrimaryCaretVisible(@Nonnull Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        Point caretPoint = editor.visualPositionToXY(caret.getVisualPosition());
        return visibleArea.contains(caretPoint);
    }

    /**
     * Performs inlay-aware conversion of offset to visual position in editor. If there are inlays at given position, their
     * 'related to preceding text' property will be taken account to determine resulting position. Specifically, resulting position will
     * match caret's visual position if it's moved to the given offset using {@link Caret#moveToOffset(int)} call.
     * <p>
     * NOTE: if editor is an {@link EditorWindow}, corresponding offset is treated as an offset in injected editor, but returned position
     * is always related to host editor.
     *
     * @see Inlay#isRelatedToPrecedingText()
     */
    @Nonnull
    public static VisualPosition inlayAwareOffsetToVisualPosition(@Nonnull Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.inlayAwareOffsetToVisualPosition(editor, offset);
    }

    public static int getTotalInlaysHeight(@Nonnull List<? extends Inlay> inlays) {
        return consulo.codeEditor.util.EditorUtil.getTotalInlaysHeight(inlays);
    }

    /**
     * Virtual space (after line end, and after end of text), inlays and space between visual lines (where block inlays are located) is
     * excluded
     */
    public static boolean isPointOverText(@Nonnull Editor editor, @Nonnull Point point) {
        return ReadAction.compute(() -> {
            VisualPosition visualPosition = editor.xyToVisualPosition(point);
            int visualLineStartY = editor.visualLineToY(visualPosition.line);
            if (point.y < visualLineStartY || point.y >= visualLineStartY + editor.getLineHeight()) {
                return false; // block inlay space
            }
            if (editor.getSoftWrapModel().isInsideOrBeforeSoftWrap(visualPosition)) {
                return false; // soft wrap
            }
            LogicalPosition logicalPosition = editor.visualToLogicalPosition(visualPosition);
            int offset = editor.logicalPositionToOffset(logicalPosition);
            if (editor.getFoldingModel().getCollapsedRegionAtOffset(offset) instanceof CustomFoldRegion) {
                return false;
            }
            if (!logicalPosition.equals(editor.offsetToLogicalPosition(offset))) {
                return false; // virtual space
            }
            List<Inlay<?>> inlays = editor.getInlayModel().getInlineElementsInRange(offset, offset);
            if (!inlays.isEmpty()) {
                VisualPosition inlaysStart = editor.offsetToVisualPosition(offset);
                if (inlaysStart.line == visualPosition.line) {
                    int relX = point.x - editor.visualPositionToXY(inlaysStart).x;
                    if (relX >= 0 && relX < inlays.stream().mapToInt(Inlay::getWidthInPixels).sum()) {
                        return false; // inline inlay
                    }
                }
            }
            return true;
        });
    }

    /**
     * This is similar to {@link SelectionModel#addSelectionListener(SelectionListener, Disposable)}, but when selection changes happen within
     * the scope of {@link CaretModel#runForEachCaret(CaretAction)} call, there will be only one notification at the end of iteration over
     * carets.
     */
    public static void addBulkSelectionListener(@Nonnull Editor editor, @Nonnull SelectionListener listener, @Nonnull Disposable disposable) {
        Ref<Pair<int[], int[]>> selectionBeforeBulkChange = new Ref<>();
        Ref<Boolean> selectionChangedDuringBulkChange = new Ref<>();
        editor.getSelectionModel().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(@Nonnull SelectionEvent e) {
                if (selectionBeforeBulkChange.isNull()) {
                    listener.selectionChanged(e);
                }
                else {
                    selectionChangedDuringBulkChange.set(Boolean.TRUE);
                }
            }
        }, disposable);
        editor.getCaretModel().addCaretActionListener(new CaretActionListener() {
            @Override
            public void beforeAllCaretsAction() {
                selectionBeforeBulkChange.set(getSelectionOffsets());
                selectionChangedDuringBulkChange.set(null);
            }

            @Override
            public void afterAllCaretsAction() {
                if (!selectionChangedDuringBulkChange.isNull()) {
                    Pair<int[], int[]> beforeBulk = selectionBeforeBulkChange.get();
                    Pair<int[], int[]> afterBulk = getSelectionOffsets();
                    listener.selectionChanged(new SelectionEvent(editor, beforeBulk.first, beforeBulk.second, afterBulk.first, afterBulk.second));
                }
                selectionBeforeBulkChange.set(null);
            }

            private Pair<int[], int[]> getSelectionOffsets() {
                return Pair.create(editor.getSelectionModel().getBlockSelectionStarts(), editor.getSelectionModel().getBlockSelectionEnds());
            }
        }, disposable);
    }


    @Nonnull
    public static DataContext getEditorDataContext(@Nonnull Editor editor) {
        DataContext context = DataManager.getInstance().getDataContext(editor.getContentComponent());
        if (context.getData(Project.KEY) == editor.getProject()) {
            return context;
        }
        return new DataContext() {
            @Nullable
            @Override
            public <T> T getData(@Nonnull Key<T> dataId) {
                if (Project.KEY == dataId) {
                    return (T) editor.getProject();
                }
                return context.getData(dataId);
            }
        };
    }

    public static EditorHighlighter createEmptyHighlighter(@Nullable Project project, @Nonnull Document document) {
        EditorHighlighter highlighter = new EmptyEditorHighlighter(new TextAttributes()) {
            @Override
            public
            @Nonnull
            HighlighterIterator createIterator(int startOffset) {
                setText(document.getImmutableCharSequence());
                return super.createIterator(startOffset);
            }

            @Override
            public void setAttributes(TextAttributes attributes) {
            }

            @Override
            public void setColorScheme(@Nonnull EditorColorsScheme scheme) {
            }
        };
        highlighter.setEditor(new HighlighterClient() {
            @Override
            public Project getProject() {
                return project;
            }

            @Override
            public void repaint(int start, int end) {
            }

            @Override
            public Document getDocument() {
                return document;
            }
        });
        return highlighter;
    }
}
