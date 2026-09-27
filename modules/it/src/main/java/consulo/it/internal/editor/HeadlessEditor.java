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
package consulo.it.internal.editor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.TextDrawingCallback;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorCaretModelBase;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import consulo.codeEditor.impl.CodeEditorSelectionModelBase;
import consulo.codeEditor.impl.CodeEditorSoftWrapModelBase;
import consulo.codeEditor.impl.LogicalPositionCache;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * A real editor without a rendering surface: the document, the highlighter, the markup and every model behave as
 * they do in a frontend, while everything which would need pixels answers with a degenerate but valid value - one
 * pixel per line, one pixel per column, the origin for any point which cannot be known.
 * <p>
 * Must be constructed on the UI thread: {@link CodeEditorBase} asserts it, and the caret, folding and markup
 * models it wires up expect to be created there.
 *
 * @author VISTALL
 * @since 2026-09-26
 */
public class HeadlessEditor extends CodeEditorBase {
    private final TextDrawingCallback myTextDrawingCallback = (g, data, start, end, x, y, color, fontInfo) -> {
    };

    /**
     * Built on demand: the markup listener the base constructor installs already asks for the gutter, and a field
     * initializer of this class only runs once that constructor has returned.
     */
    private @Nullable HeadlessEditorGutter myGutter;

    private @Nullable Component myUIComponent;

    private @Nullable LogicalPositionCache myLogicalPositionCache;

    private boolean myCaretVisible = true;

    private boolean myEmbeddedIntoDialogWrapper;

    private int myVerticalScrollbarOrientation = VERTICAL_SCROLLBAR_RIGHT;

    @RequiredUIAccess
    public HeadlessEditor(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
        super(document, viewer, project, kind);

        LogicalPositionCache logicalPositionCache = new LogicalPositionCache(this, () -> EditorUtil.getTabSize(this));
        Disposer.register(myDisposable, logicalPositionCache);
        myLogicalPositionCache = logicalPositionCache;
    }

    @Override
    protected CodeEditorSelectionModelBase createSelectionModel() {
        return new HeadlessSelectionModel(this);
    }

    @Override
    protected MarkupModelImpl createMarkupModel() {
        return new HeadlessMarkupModel(this);
    }

    @Override
    protected CodeEditorFoldingModelBase createFoldingModel() {
        return new HeadlessFoldingModel(this);
    }

    @Override
    protected CodeEditorCaretModelBase createCaretModel() {
        return new HeadlessCaretModel(this);
    }

    @Override
    protected CodeEditorScrollingModelBase createScrollingModel() {
        return new HeadlessScrollingModel(this);
    }

    @Override
    protected CodeEditorInlayModelBase createInlayModel() {
        return new HeadlessInlayModel(this);
    }

    @Override
    protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
        return new HeadlessSoftWrapModel(this);
    }

    @Override
    protected DataContext getComponentContext() {
        return DataContext.builder()
            .add(Editor.KEY, this)
            .add(EditorGutter.KEY, gutter())
            .build();
    }

    @Override
    protected void stopDumb() {
    }

    @Override
    public void startDumb() {
    }

    @Override
    public void release() {
        assertIsDispatchThread();
        if (isReleased) {
            throwDisposalError("Double release of editor:");
        }
        myTraceableDisposable.kill(null);

        isReleased = true;

        myFoldingModel.dispose();
        mySoftWrapModel.release();
        myMarkupModel.dispose();
        myScrollingModel.dispose();

        Disposer.dispose(myCaretModel);
        Disposer.dispose(mySoftWrapModel);

        myFocusListeners.clear();
        myMouseListeners.clear();
        myMouseMotionListeners.clear();

        Disposer.dispose(myDisposable);
    }

    @Override
    public void reinitSettings() {
        if (myScheme instanceof MyColorSchemeDelegate schemeDelegate) {
            schemeDelegate.updateGlobalScheme();
        }

        mySettings.reinitSettings();

        EditorHighlighter highlighter = myHighlighter;
        if (highlighter != null) {
            highlighter.setColorScheme(myScheme);
        }

        LogicalPositionCache logicalPositionCache = myLogicalPositionCache;
        if (logicalPositionCache != null) {
            logicalPositionCache.reset(true);
        }

        myCaretModel.reinitSettings();
        mySelectionModel.reinitSettings();
        mySoftWrapModel.reinitSettings();

        gutter().dropRenderersCache();
    }

    @Override
    public void setOneLineMode(boolean isOneLineMode) {
        myIsOneLineMode = isOneLineMode;
        reinitSettings();
    }

    @Override
    public EditorGutter getGutter() {
        return gutter();
    }

    @Override
    public EditorGutterComponentEx getGutterComponentEx() {
        return gutter();
    }

    private HeadlessEditorGutter gutter() {
        HeadlessEditorGutter gutter = myGutter;
        if (gutter == null) {
            gutter = new HeadlessEditorGutter(this);
            myGutter = gutter;
        }
        return gutter;
    }

    @Override
    public TextDrawingCallback getTextDrawingCallback() {
        return myTextDrawingCallback;
    }

    @Override
    public boolean isShowing() {
        return true;
    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Override
    public @Nullable JComponent getHeaderComponent() {
        return null;
    }

    @Override
    public void setHeaderComponent(@Nullable JComponent header) {
    }

    @Override
    public JComponent getComponent() {
        throw new UnsupportedOperationException("headless: the editor has no component");
    }

    @Override
    public JComponent getContentComponent() {
        throw new UnsupportedOperationException("headless: the editor has no content component");
    }

    @Override
    public Component getUIComponent() {
        return uiComponent();
    }

    @Override
    public Component getContentUIComponent() {
        return uiComponent();
    }

    private Component uiComponent() {
        Component uiComponent = myUIComponent;
        if (uiComponent == null) {
            uiComponent = VerticalLayout.create();
            myUIComponent = uiComponent;
        }
        return uiComponent;
    }

    @Override
    public void setBorder(@Nullable Border border) {
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public @Nullable EditorMouseEventArea getMouseEventArea(MouseEvent e) {
        return EditorMouseEventArea.EDITING_AREA;
    }

    @Override
    public Dimension getContentSize() {
        return new Dimension(getMaxWidthInRange(0, myDocument.getTextLength()), getVisibleLineCount() * getLineHeight());
    }

    @Override
    public ColorValue getBackgroundColor() {
        return myScheme.getDefaultBackground();
    }

    @Override
    public void setBackgroundColor(ColorValue color) {
    }

    @Override
    public int getLineHeight() {
        return 1;
    }

    @Override
    public int getAscent() {
        return 0;
    }

    @Override
    public int getPrefixTextWidthInPixels() {
        return 0;
    }

    /**
     * One pixel per column, so the widest line in the range is its length in columns.
     */
    @Override
    public int getMaxWidthInRange(int startOffset, int endOffset) {
        int textLength = myDocument.getTextLength();
        int firstLine = myDocument.getLineNumber(Math.max(0, Math.min(startOffset, textLength)));
        int lastLine = myDocument.getLineNumber(Math.max(0, Math.min(endOffset, textLength)));

        int maxWidth = 0;
        for (int line = firstLine; line <= lastLine; line++) {
            maxWidth = Math.max(maxWidth, myDocument.getLineEndOffset(line) - myDocument.getLineStartOffset(line));
        }

        return maxWidth;
    }

    @Override
    public int logicalPositionToOffset(LogicalPosition pos) {
        return positionCache().logicalPositionToOffset(pos);
    }

    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        return positionCache().offsetToLogicalPosition(offset);
    }

    @Override
    public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
        return new VisualPosition(logicalToVisualLine(logicalPos.line), logicalPos.column, logicalPos.visualPositionLeansRight);
    }

    @Override
    public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
        return new LogicalPosition(visualToLogicalLine(visiblePos.getLine()), visiblePos.getColumn(), visiblePos.leansRight);
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset) {
        return logicalToVisualPosition(offsetToLogicalPosition(offset));
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
        return offsetToVisualPosition(offset);
    }

    @Override
    public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
        return logicalToVisualLine(myDocument.getLineNumber(clampOffset(offset)));
    }

    @Override
    public int visualLineStartOffset(int visualLine) {
        int lineCount = myDocument.getLineCount();
        if (lineCount == 0) {
            return 0;
        }

        int logicalLine = Math.max(0, Math.min(visualToLogicalLine(visualLine), lineCount - 1));

        return myDocument.getLineStartOffset(logicalLine);
    }

    /**
     * What a collapsed region takes out of the document stands between a line and the row it is drawn at, so the
     * two only agree while nothing is folded.
     */
    private int logicalToVisualLine(int logicalLine) {
        int lineCount = myDocument.getLineCount();
        if (logicalLine <= 0 || lineCount == 0) {
            return Math.max(0, logicalLine);
        }

        int offset = myDocument.getLineStartOffset(Math.min(logicalLine, lineCount - 1));

        return Math.max(0, logicalLine - myFoldingModel.getFoldedLinesCountBefore(offset));
    }

    private int visualToLogicalLine(int visualLine) {
        if (visualLine <= 0) {
            return Math.max(0, visualLine);
        }

        int logicalLine = visualLine;

        for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
            if (region.isExpanded() || !region.isValid()) {
                continue;
            }

            int startLine = myDocument.getLineNumber(region.getStartOffset());
            if (startLine >= logicalLine) {
                break;
            }

            logicalLine += Math.max(0, myDocument.getLineNumber(region.getEndOffset()) - startLine);
        }

        return Math.min(logicalLine, Math.max(0, myDocument.getLineCount() - 1));
    }

    private int clampOffset(int offset) {
        return Math.max(0, Math.min(offset, myDocument.getTextLength()));
    }

    private LogicalPositionCache positionCache() {
        LogicalPositionCache logicalPositionCache = myLogicalPositionCache;
        if (logicalPositionCache == null) {
            throw new IllegalStateException("headless: position cache is asked for before the editor is constructed");
        }
        return logicalPositionCache;
    }

    @Override
    public int visualLineToY(int visualLine) {
        return visualLine * getLineHeight();
    }

    @Override
    public Point visualPositionToXY(VisualPosition visible) {
        return new Point(visible.getColumn(), visualLineToY(visible.getLine()));
    }

    @Override
    public VisualPosition xyToVisualPosition(Point p) {
        return new VisualPosition(Math.max(0, p.y / getLineHeight()), Math.max(0, p.x));
    }

    @Override
    public Point2D visualPositionToPoint2D(VisualPosition pos) {
        return visualPositionToXY(pos);
    }

    @Override
    public Point logicalPositionToXY(LogicalPosition pos) {
        return visualPositionToXY(logicalToVisualPosition(pos));
    }

    @Override
    public VisualPosition xyToVisualPosition(Point2D p) {
        return xyToVisualPosition(new Point((int) p.getX(), (int) p.getY()));
    }

    /**
     * There is no surface to measure against, so any point is the start of the document. The visible range the
     * highlighting passes compute goes through here, and an editor which cannot answer stops them dead.
     */
    @Override
    public LogicalPosition xyToLogicalPosition(Point p) {
        return new LogicalPosition(0, 0);
    }

    @Override
    public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {
        gutter().dropRenderersCache();
    }

    @Override
    public void setVerticalScrollbarOrientation(int type) {
        myVerticalScrollbarOrientation = type;
    }

    @Override
    public int getVerticalScrollbarOrientation() {
        return myVerticalScrollbarOrientation;
    }

    @Override
    public void setVerticalScrollbarVisible(boolean b) {
    }

    @Override
    public void setHorizontalScrollbarVisible(boolean b) {
    }

    @Override
    public boolean setCaretVisible(boolean b) {
        boolean old = myCaretVisible;
        myCaretVisible = b;
        return old;
    }

    @Override
    public boolean setCaretEnabled(boolean enabled) {
        return setCaretVisible(enabled);
    }

    public boolean isCaretVisible() {
        return myCaretVisible;
    }

    @Override
    public void setFontSize(int fontSize) {
        int oldFontSize = myScheme.getEditorFontSize();
        if (oldFontSize == fontSize) {
            return;
        }

        myScheme.setEditorFontSize(fontSize);
        myPropertyChangeSupport.firePropertyChange(PROP_FONT_SIZE, oldFontSize, fontSize);
        reinitSettings();
    }

    @Override
    public boolean isEmbeddedIntoDialogWrapper() {
        return myEmbeddedIntoDialogWrapper;
    }

    @Override
    public void setEmbeddedIntoDialogWrapper(boolean b) {
        myEmbeddedIntoDialogWrapper = b;
    }

    @Override
    public void setCustomCursor(Object requestor, @Nullable Cursor cursor) {
    }
}
