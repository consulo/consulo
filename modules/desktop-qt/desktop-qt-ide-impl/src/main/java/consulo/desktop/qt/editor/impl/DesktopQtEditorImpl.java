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
import consulo.codeEditor.impl.CodeEditorCaretModelBase;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import consulo.codeEditor.impl.CodeEditorSelectionModelBase;
import consulo.codeEditor.impl.CodeEditorSoftWrapModelBase;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.colorScheme.internal.FontPreferencesManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.Component;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorImpl extends CodeEditorBase implements RealEditor {
    private final DesktopQtEditorComponent myComponent;

    private final DesktopQtEditorGutterComponentImpl myGutterComponent;

    private final DesktopQtEditorFontMetrics myFontMetrics = new DesktopQtEditorFontMetrics(this);

    private final DesktopQtEditorCoordinateMapper myCoordinateMapper = new DesktopQtEditorCoordinateMapper(this);

    private boolean myCaretVisible = true;

    /**
     * Negative means "remeasure". Measuring the whole document is linear in its length, so it must not happen once
     * per repaint - the layout cache that would make it incremental is the next piece of the view layer.
     */
    private int myDocumentWidth = -1;

    public DesktopQtEditorImpl(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
        super(document, viewer, project, kind);

        myComponent = new DesktopQtEditorComponent(this);
        myGutterComponent = new DesktopQtEditorGutterComponentImpl();

        // the caret and the selection are painted, so moving either is a visual change the surface has to hear about
        getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                repaintSurface();
            }
        });
        getSelectionModel().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(SelectionEvent event) {
                repaintSurface();
            }
        });
    }

    private void repaintSurface() {
        DesktopQtEditorWidget widget = myComponent.toQtComponent();
        if (widget != null) {
            widget.restartCaretBlink();
        }
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

    @Override
    public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {
        myDocumentWidth = -1;

        DesktopQtEditorWidget widget = myComponent.toQtComponent();
        if (widget != null) {
            widget.updateScrollRanges();
            widget.viewport().update();
        }
    }

    @Override
    public void reinitSettings() {
        myFontMetrics.reset();
        myDocumentWidth = -1;

        DesktopQtEditorWidget widget = myComponent.toQtComponent();
        if (widget != null) {
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

        DesktopQtEditorWidget widget = myComponent.toQtComponent();
        if (widget != null) {
            widget.restartCaretBlink();
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
