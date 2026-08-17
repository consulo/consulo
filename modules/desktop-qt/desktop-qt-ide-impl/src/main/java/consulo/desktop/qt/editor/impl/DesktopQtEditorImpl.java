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
package consulo.desktop.qt.editor.impl;

import consulo.application.Application;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.TextDrawingCallback;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.impl.CodeEditorCaretModelBase;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import consulo.codeEditor.impl.CodeEditorSelectionModelBase;
import consulo.codeEditor.impl.CodeEditorSoftWrapModelBase;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.colorScheme.internal.FontPreferencesManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.action.DesktopQtActionContextMenu;
import consulo.desktop.qt.ui.impl.base.DesktopQtAwtBridgeComponent;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.util.List;
import java.awt.Point;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorImpl extends CodeEditorBase implements RealEditor, CaretPixelLocationProvider {
    private final DesktopQtEditorComponent myComponent;

    private final DesktopQtEditorGutterComponentImpl myGutterComponent;

    private final DesktopQtEditorFontMetrics myFontMetrics = new DesktopQtEditorFontMetrics(this);

    private final DesktopQtEditorVisualLines myVisualLines = new DesktopQtEditorVisualLines(this);

    private final DesktopQtEditorCoordinateMapper myCoordinateMapper = new DesktopQtEditorCoordinateMapper(this, myVisualLines);

    private boolean myCaretVisible = true;

    /**
     * Negative means "remeasure". Measuring the whole document is linear in its length, so it must not happen once
     * per repaint - the layout cache that would make it incremental is the next piece of the view layer.
     */
    private int myDocumentWidth = -1;

    public DesktopQtEditorImpl(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
        super(document, viewer, project, kind);

        myComponent = new DesktopQtEditorComponent(this);
        myGutterComponent = new DesktopQtEditorGutterComponentImpl(this);

        // the caret and the selection are painted, so moving either is a visual change the surface has to hear
        // about - but only the rows either of them left or arrived at, never the whole document
        getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                repaintLines(event.getOldPosition().line, event.getNewPosition().line);
            }
        });
        getSelectionModel().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(SelectionEvent event) {
                repaintRange(event.getOldRange());
                repaintRange(event.getNewRange());
            }
        });

        DesktopQtActionContextMenu.install(myComponent, this::getContextMenuGroup, ActionPlaces.EDITOR_POPUP, this::getDataContext);
    }

    /**
     * The group is looked up per click rather than kept, since a plugin may replace what the id stands for and
     * the editor may be pointed at a group of its own at any time.
     */
    private @Nullable ActionGroup getContextMenuGroup() {
        String groupId = getContextMenuGroupId();
        if (groupId == null) {
            return null;
        }

        return ActionManager.getInstance().getAction(groupId) instanceof ActionGroup group ? group : null;
    }

    private void repaintRange(@Nullable TextRange range) {
        if (range == null) {
            return;
        }

        Document document = getDocument();
        int length = document.getTextLength();

        repaintLines(
            document.getLineNumber(Math.max(0, Math.min(range.getStartOffset(), length))),
            document.getLineNumber(Math.max(0, Math.min(range.getEndOffset(), length)))
        );
    }

    /**
     * Redraws the given rows and shows the caret again, which is what every move of the caret or the selection
     * amounts to on screen.
     */
    private void repaintLines(int startLine, int endLine) {
        DesktopQtEditorWidget widget = getSurface();
        if (widget != null) {
            widget.restartCaretBlink();

            // callers speak in lines of the file, the surface draws rows - folding makes the two disagree
            widget.repaintLines(myVisualLines.logicalToVisualLine(startLine), myVisualLines.logicalToVisualLine(endLine));
        }
    }

    /**
     * Where the caret is inside the surface, which is what the completion lookup and everything else anchored to
     * the caret opens against. Without it the platform has nowhere to put them and falls back to the top left of
     * the editor.
     * <p>
     * The coordinates are relative to the surface widget, so the gutter has to be added and the scroll offset
     * taken off - {@link #visualPositionToXY} answers in document space, which is a different thing.
     */
    @Override
    public @Nullable CaretPixelLocation getCaretPixelLocation() {
        DesktopQtEditorWidget widget = getSurface();
        if (widget == null) {
            return null;
        }

        Point point = visualPositionToXY(getCaretModel().getVisualPosition());

        int gutterWidth = widget.getGutter().width();
        int x = gutterWidth + point.x - widget.horizontalScrollBar().value();
        int y = point.y - widget.verticalScrollBar().value();

        return new CaretPixelLocation(x, y, getLineHeight(), gutterWidth);
    }

    /**
     * The surface, or null when there is nothing to draw on.
     * <p>
     * A qt object outlives the native one it stands for - a widget torn down by qt itself, as a closed tab is, is
     * still a live java reference whose every call throws {@link io.qt.QNoNativeResourcesException}. The editor is
     * asked for its geometry after that happens (the history manager reads the scroll position of a tab being
     * closed), so nothing may reach the widget without going through here.
     */
    public @Nullable DesktopQtEditorWidget getSurface() {
        DesktopQtEditorWidget widget = myComponent.toQtComponent();
        return widget != null && !widget.isDisposed() ? widget : null;
    }

    /**
     * The listeners the platform registered for clicks on the editor. The base keeps them protected, and the
     * widgets which actually receive the clicks live outside it, so they reach them through here.
     */
    public List<EditorMouseListener> getEditorMouseListeners() {
        return myMouseListeners;
    }

    /**
     * Hands a press to everything the platform hung off the editor, then lets the popup handlers have it. That
     * second step is how the editor context menu is meant to open: {@code invokePopupIfNeeded} runs the handlers
     * registered for the editing area when the press is a popup trigger, and it can only run if the frontend
     * reports the press at all - which the qt surface did not until now.
     */
    public void fireMousePressed(EditorMouseEvent event) {
        for (EditorMouseListener listener : myMouseListeners) {
            listener.mousePressed(event);
        }

        invokePopupIfNeeded(event);
    }

    public void fireMouseReleased(EditorMouseEvent event) {
        for (EditorMouseListener listener : myMouseListeners) {
            listener.mouseReleased(event);
        }
    }

    public void fireMouseMoved(EditorMouseEvent event) {
        for (EditorMouseMotionListener listener : myMouseMotionListeners) {
            listener.mouseMoved(event);
        }
    }

    public DesktopQtEditorVisualLines getVisualLines() {
        return myVisualLines;
    }

    public DesktopQtEditorFontMetrics getFontMetrics() {
        return myFontMetrics;
    }

    public int getDocumentWidth() {
        if (myDocumentWidth < 0) {
            myDocumentWidth = getMaxWidthInRange(0, getDocument().getTextLength());
        }
        return myDocumentWidth;
    }

    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public Component getContentUIComponent() {
        return myComponent;
    }

    /**
     * Platform code which has not been migrated off swing - brace highlighting, word selection, parameter info -
     * asks the editor for a {@link JComponent} and throws away every frontend that has none. The bridge is that
     * component, and it still carries the qt widget back, so nothing is lost on the round trip.
     */
    @Override
    public JComponent getComponent() {
        return DesktopQtAwtBridgeComponent.of(myComponent);
    }

    @Override
    public JComponent getContentComponent() {
        return DesktopQtAwtBridgeComponent.of(myComponent);
    }

    @Override
    public boolean isShowing() {
        return true;
    }

    @Override
    protected CodeEditorSelectionModelBase createSelectionModel() {
        return new DesktopQtCodeEditorSelectionModelImpl(this);
    }

    @Override
    protected MarkupModelImpl createMarkupModel() {
        return new DesktopQtMarkupModelImpl(this);
    }

    @Override
    protected CodeEditorFoldingModelBase createFoldingModel() {
        return new DesktopQtCodeEditorFoldingModelImpl(this);
    }

    @Override
    protected CodeEditorCaretModelBase createCaretModel() {
        return new DesktopQtCodeEditorCaretModelImpl(this);
    }

    @Override
    protected CodeEditorScrollingModelBase createScrollingModel() {
        return new DesktopQtCodeEditorScrollingModelImpl(this);
    }

    @Override
    protected CodeEditorInlayModelBase createInlayModel() {
        return new DesktopQtCodeEditorInlayModelImpl(this);
    }

    @Override
    protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
        return new DesktopQtCodeEditorSoftWrapModelImpl(this, Application.get().getInstance(FontPreferencesManager.class));
    }

    @Override
    protected DataContext getComponentContext() {
        return DataManager.getInstance().getDataContext(getUIComponent());
    }

    @Override
    protected void stopDumb() {
    }

    @Override
    public void release() {
    }

    @Override
    public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
        return myCoordinateMapper.offsetToVisualLine(offset);
    }

    @Override
    public int visualLineStartOffset(int visualLine) {
        return myCoordinateMapper.visualLineStartOffset(visualLine);
    }

    @Override
    public void startDumb() {
    }

    @Override
    public EditorGutterComponentEx getGutterComponentEx() {
        return myGutterComponent;
    }

    @Override
    public void setVerticalScrollbarOrientation(@MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT}) int type) {
    }

    @Override
    public int getVerticalScrollbarOrientation() {
        return 0;
    }

    @Override
    public void setVerticalScrollbarVisible(boolean b) {
    }

    @Override
    public void setHorizontalScrollbarVisible(boolean b) {
    }

    /**
     * The hook the platform uses to say what changed, and the reason highlighting has to be cheap: the daemon
     * calls it once per highlighter it adds or removes. Redrawing the whole surface here - and worse, throwing
     * the measured width away, which costs a pass over every line of the document - is what made selecting and
     * highlighting crawl. Only the lines named are repainted, the way the awt {@code doRepaint} does it.
     */
    @Override
    public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {
        Document document = getDocument();
        if (document.isInBulkUpdate()) {
            return;
        }

        DesktopQtEditorWidget widget = getSurface();
        if (widget == null) {
            return;
        }

        int end = Math.max(0, Math.min(endOffset, document.getTextLength()));
        int start = Math.max(0, Math.min(startOffset, end));

        // only a change to the text itself can change how wide the document is or how many digits the gutter
        // needs; markup laid over unchanged text cannot
        if (invalidateTextLayout) {
            widenDocumentWidth(start, end);

            widget.updateSideAreas();
            widget.updateScrollRanges();
        }

        widget.repaintLines(document.getLineNumber(start), document.getLineNumber(end));
    }

    /**
     * Grows the measured width by what the changed lines now need, instead of measuring the document again.
     * <p>
     * It never shrinks, so deleting the longest line leaves the horizontal scroll bar longer than it has to be
     * until something resets the measurement. That is the trade for not walking the whole document on every
     * keystroke; the layout cache which would make it exact is the next piece of the view layer.
     */
    private void widenDocumentWidth(int startOffset, int endOffset) {
        if (myDocumentWidth >= 0) {
            myDocumentWidth = Math.max(myDocumentWidth, getMaxWidthInRange(startOffset, endOffset));
        }
    }

    @Override
    public void reinitSettings() {
        myFontMetrics.reset();
        myDocumentWidth = -1;

        DesktopQtEditorWidget widget = getSurface();
        if (widget != null) {
            widget.updateSideAreas();
            widget.updateScrollRanges();
            widget.viewport().update();
        }
    }

    @Override
    public int getMaxWidthInRange(int startOffset, int endOffset) {
        Document document = getDocument();

        int firstLine = document.getLineNumber(Math.max(0, Math.min(startOffset, document.getTextLength())));
        int lastLine = document.getLineNumber(Math.max(0, Math.min(endOffset, document.getTextLength())));

        double maxWidth = 0;
        for (int line = firstLine; line <= lastLine; line++) {
            CharSequence lineText = document.getCharsSequence().subSequence(document.getLineStartOffset(line), document.getLineEndOffset(line));

            maxWidth = Math.max(maxWidth, myFontMetrics.getTextWidth(lineText));
        }

        return (int) Math.ceil(maxWidth);
    }

    public boolean isCaretVisible() {
        return myCaretVisible;
    }

    @Override
    public boolean setCaretVisible(boolean b) {
        boolean old = myCaretVisible;
        myCaretVisible = b;

        DesktopQtEditorWidget widget = getSurface();
        if (widget != null) {
            widget.restartCaretBlink();
            widget.repaintCarets();
        }

        return old;
    }

    @Override
    public boolean setCaretEnabled(boolean enabled) {
        return setCaretVisible(enabled);
    }

    @Override
    public void setFontSize(int fontSize) {
    }

    @Override
    public boolean isEmbeddedIntoDialogWrapper() {
        return false;
    }

    @Override
    public void setEmbeddedIntoDialogWrapper(boolean b) {
    }

    @Override
    public TextDrawingCallback getTextDrawingCallback() {
        return null;
    }

    @Override
    public int getPrefixTextWidthInPixels() {
        return 0;
    }

    @Override
    public void setCustomCursor(Object requestor, @Nullable Cursor cursor) {
    }

    @Override
    public int getLineHeight() {
        return myFontMetrics.getLineHeight();
    }

    @Override
    public int logicalPositionToOffset(LogicalPosition pos) {
        return myCoordinateMapper.logicalPositionToOffset(pos);
    }

    @Override
    public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
        return myCoordinateMapper.logicalToVisualPosition(logicalPos);
    }

    @Override
    public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
        return myCoordinateMapper.visualToLogicalPosition(visiblePos);
    }

    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        return myCoordinateMapper.offsetToLogicalPosition(offset);
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset) {
        return myCoordinateMapper.logicalToVisualPosition(offsetToLogicalPosition(offset));
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
        return offsetToVisualPosition(offset);
    }

    @Override
    public EditorGutter getGutter() {
        return myGutterComponent;
    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Override
    public int getAscent() {
        return myFontMetrics.getAscent();
    }

    public LogicalPosition xyToLogicalPosition(Point p) {
        return myCoordinateMapper.xyToLogicalPosition(p);
    }

    public Point visualPositionToXY(VisualPosition visible) {
        return myCoordinateMapper.visualPositionToXY(visible);
    }
}
