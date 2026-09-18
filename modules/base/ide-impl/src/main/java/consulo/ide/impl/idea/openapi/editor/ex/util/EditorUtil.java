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

import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.codeEditor.util.AWTEditorUtil;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentBulkUpdateListener;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.util.FileEditorUtil;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AWTConstants;
import consulo.ui.image.Image;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

@Deprecated
public final class EditorUtil {
    private EditorUtil() {
    }

    @Deprecated
    public static boolean isRealFileEditor(@Nullable Editor editor) {
        return FileEditorUtil.isRealFileEditor(editor);
    }

    @Deprecated
    public static boolean isPasswordEditor(@Nullable Editor editor) {
        return AWTEditorUtil.isPasswordEditor(editor);
    }

    @Deprecated
    public static @Nullable EditorEx getEditorEx(@Nullable FileEditor fileEditor) {
        return FileEditorUtil.getEditorEx(fileEditor);
    }

    @Deprecated
    public static int getLastVisualLineColumnNumber(Editor editor, int line) {
        return EditorImplUtil.getLastVisualLineColumnNumber(editor, line);
    }

    @Deprecated
    public static int getVisualLineEndOffset(Editor editor, int line) {
        return EditorImplUtil.getVisualLineEndOffset(editor, line);
    }

    public static float calcVerticalScrollProportion(Editor editor) {
        Rectangle viewArea = editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
        if (viewArea.height == 0) {
            return 0;
        }
        LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
        Point location = editor.logicalPositionToXY(pos);
        return (location.y - viewArea.y) / (float) viewArea.height;
    }

    public static void setVerticalScrollProportion(Editor editor, float proportion) {
        Rectangle viewArea = editor.getScrollingModel().getVisibleArea();
        LogicalPosition caretPosition = editor.getCaretModel().getLogicalPosition();
        Point caretLocation = editor.logicalPositionToXY(caretPosition);
        int yPos = caretLocation.y;
        yPos -= viewArea.height * proportion;
        editor.getScrollingModel().scrollVertically(yPos);
    }

    @Deprecated
    @RequiredUIAccess
    public static void fillVirtualSpaceUntilCaret(Editor editor) {
        EditorModificationUtil.fillVirtualSpaceUntilCaret(editor);
    }

    @Deprecated
    @RequiredUIAccess
    public static void fillVirtualSpaceUntil(Editor editor, int columnNumber, int lineNumber) {
        EditorModificationUtil.fillVirtualSpaceUntil(editor, columnNumber, lineNumber);
    }

    @Deprecated
    public static int calcColumnNumber(Editor editor, CharSequence text, int start, int offset) {
        return consulo.codeEditor.util.EditorUtil.calcColumnNumber(editor, text, start, offset);
    }

    @Deprecated
    public static int calcColumnNumber(@Nullable Editor editor, CharSequence text, int start, int offset, int tabSize) {
        return consulo.codeEditor.util.EditorUtil.calcColumnNumber(editor, text, start, offset, tabSize);
    }

    public static void setHandCursor(Editor view) {
        Cursor c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        // XXX: Workaround, simply view.getContentComponent().setCursor(c) doesn't work
        if (view.getContentComponent().getCursor() != c) {
            view.getContentComponent().setCursor(c);
        }
    }

    @Deprecated
    public static FontInfo fontForChar(char c, @AWTConstants.FontStyle int style, Editor editor) {
        return EditorImplUtil.fontForChar(c, style, editor);
    }

    @Deprecated
    public static Image scaleIconAccordingEditorFont(Image icon, Editor editor) {
        return AWTEditorUtil.scaleIconAccordingEditorFont(icon, editor);
    }

    @Deprecated
    public static int charWidth(char c, @AWTConstants.FontStyle int fontType, Editor editor) {
        return EditorImplUtil.charWidth(c, fontType, editor);
    }

    @Deprecated
    public static int getSpaceWidth(@AWTConstants.FontStyle int fontType, Editor editor) {
        return EditorImplUtil.getSpaceWidth(fontType, editor);
    }

    @Deprecated
    public static int getPlainSpaceWidth(Editor editor) {
        return EditorImplUtil.getPlainSpaceWidth(editor);
    }

    @Deprecated
    public static int getTabSize(Editor editor) {
        return consulo.codeEditor.util.EditorUtil.getTabSize(editor);
    }

    @Deprecated
    public static int nextTabStop(int x, Editor editor) {
        return EditorImplUtil.nextTabStop(x, editor);
    }

    @Deprecated
    public static int nextTabStop(int x, Editor editor, int tabSize) {
        return EditorImplUtil.nextTabStop(x, editor, tabSize);
    }

    @Deprecated
    public static int nextTabStop(int x, int plainSpaceWidth, int tabSize) {
        return EditorImplUtil.nextTabStop(x, plainSpaceWidth, tabSize);
    }

    @Deprecated
    public static float nextTabStop(float x, float plainSpaceWidth, int tabSize) {
        return AWTEditorUtil.nextTabStop(x, plainSpaceWidth, tabSize);
    }

    @Deprecated
    public static int textWidthInColumns(Editor editor, CharSequence text, int start, int end, int x) {
        return EditorImplUtil.textWidthInColumns(editor, text, start, end, x);
    }

    @Deprecated
    public static int columnsNumber(int width, int plainSpaceSize) {
        return EditorImplUtil.columnsNumber(width, plainSpaceSize);
    }

    @Deprecated
    public static int columnsNumber(float width, float plainSpaceSize) {
        return AWTEditorUtil.columnsNumber(width, plainSpaceSize);
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
    public static int textWidth(Editor editor, CharSequence text, int start, int end, @AWTConstants.FontStyle int fontType, int x) {
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

    @Deprecated
    public static Couple<LogicalPosition> calcCaretLineRange(Editor editor) {
        return consulo.codeEditor.util.EditorUtil.calcCaretLineRange(editor);
    }

    @Deprecated
    public static Couple<LogicalPosition> calcCaretLineRange(Caret caret) {
        return consulo.codeEditor.util.EditorUtil.calcCaretLineRange(caret);
    }

    @Deprecated
    public static Couple<LogicalPosition> calcSurroundingRange(Editor editor, VisualPosition start, VisualPosition end) {
        return consulo.codeEditor.util.EditorUtil.calcSurroundingRange(editor, start, end);
    }

    @Deprecated
    public static int getNotFoldedLineStartOffset(Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.getNotFoldedLineStartOffset(editor, offset);
    }

    @Deprecated
    public static int getNotFoldedLineEndOffset(Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.getNotFoldedLineEndOffset(editor, offset);
    }

    @Deprecated
    public static void scrollToTheEnd(Editor editor) {
        consulo.codeEditor.util.EditorUtil.scrollToTheEnd(editor);
    }

    @Deprecated
    public static void scrollToTheEnd(Editor editor, boolean preferVerticalScroll) {
        consulo.codeEditor.util.EditorUtil.scrollToTheEnd(editor, preferVerticalScroll);
    }

    @Deprecated
    public static boolean isChangeFontSize(MouseWheelEvent e) {
        return AWTEditorUtil.isChangeFontSize(e);
    }

    @Deprecated
    public static boolean inVirtualSpace(Editor editor, LogicalPosition logicalPosition) {
        return consulo.codeEditor.util.EditorUtil.inVirtualSpace(editor, logicalPosition);
    }

    @Deprecated
    public static void reinitSettings() {
        EditorFactory.getInstance().refreshAllEditors();
    }

    @Deprecated
    public static TextRange getSelectionInAnyMode(Editor editor) {
        return consulo.codeEditor.util.EditorUtil.getSelectionInAnyMode(editor);
    }

    @Deprecated
    public static int yPositionToLogicalLine(Editor editor, MouseEvent event) {
        return AWTEditorUtil.yPositionToLogicalLine(editor, event);
    }

    @Deprecated
    public static int yPositionToLogicalLine(Editor editor, Point point) {
        return AWTEditorUtil.yPositionToLogicalLine(editor, point);
    }

    @Deprecated
    public static int yPositionToLogicalLine(Editor editor, int y) {
        return consulo.codeEditor.util.EditorUtil.yPositionToLogicalLine(editor, y);
    }

    @Deprecated
    public static boolean isAtLineEnd(Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.isAtLineEnd(editor, offset);
    }

    /**
     * Setting selection using {@link SelectionModel#setSelection(int, int)} or {@link Caret#setSelection(int, int)} methods can result
     * in resulting selection range to be larger than requested (in case requested range intersects with collapsed fold regions).
     * This method will make sure interfering collapsed regions are expanded first, so that resulting selection range is exactly as
     * requested.
     */
    public static void setSelectionExpandingFoldedRegionsIfNeeded(Editor editor, int startOffset, int endOffset) {
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
            FoldRegion finalStartFoldRegion = startFoldRegion;
            FoldRegion finalEndFoldRegion = endFoldRegion;
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

    @Deprecated
    public static Font getEditorFont() {
        return AWTLanguageEditorUtil.getEditorFont();
    }

    @Deprecated
    public static int getDefaultCaretWidth() {
        return consulo.codeEditor.util.EditorUtil.getDefaultCaretWidth();
    }

    @Deprecated
    public static int getSoftWrapCountAfterLineStart(Editor editor, LogicalPosition position) {
        return consulo.codeEditor.util.EditorUtil.getSoftWrapCountAfterLineStart(editor, position);
    }

    @Deprecated
    public static boolean attributesImpactFontStyleOrColor(@Nullable TextAttributes attributes) {
        return consulo.codeEditor.util.EditorUtil.attributesImpactFontStyleOrColor(attributes);
    }

    @Deprecated
    public static boolean isCurrentCaretPrimary(Editor editor) {
        return consulo.codeEditor.util.EditorUtil.isCurrentCaretPrimary(editor);
    }

    @RequiredUIAccess
    public static void disposeWithEditor(Editor editor, Disposable disposable) {
        consulo.codeEditor.util.EditorUtil.disposeWithEditor(editor, disposable);
    }

    @RequiredUIAccess
    public static void runBatchFoldingOperationOutsideOfBulkUpdate(Editor editor, Runnable operation) {
        DocumentEx document = ObjectUtil.tryCast(editor.getDocument(), DocumentEx.class);
        if (document != null && document.isInBulkUpdate()) {
            MessageBusConnection connection = Application.get().getMessageBus().connect();
            disposeWithEditor(editor, connection::disconnect);
            connection.subscribe(DocumentBulkUpdateListener.class, new DocumentBulkUpdateListener.Adapter() {
                @Override
                public void updateFinished(Document doc) {
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

    @Deprecated
    public static boolean isPrimaryCaretVisible(Editor editor) {
        return AWTEditorUtil.isPrimaryCaretVisible(editor);
    }

    @Deprecated
    public static VisualPosition inlayAwareOffsetToVisualPosition(Editor editor, int offset) {
        return consulo.codeEditor.util.EditorUtil.inlayAwareOffsetToVisualPosition(editor, offset);
    }

    @Deprecated
    public static int getTotalInlaysHeight(List<? extends Inlay> inlays) {
        return consulo.codeEditor.util.EditorUtil.getTotalInlaysHeight(inlays);
    }

    @Deprecated
    public static boolean isPointOverText(Editor editor, Point point) {
        return AWTEditorUtil.isPointOverText(editor, point);
    }

    @Deprecated
    public static void addBulkSelectionListener(Editor editor, SelectionListener listener, Disposable disposable) {
        EditorImplUtil.addBulkSelectionListener(editor, listener, disposable);
    }
}
