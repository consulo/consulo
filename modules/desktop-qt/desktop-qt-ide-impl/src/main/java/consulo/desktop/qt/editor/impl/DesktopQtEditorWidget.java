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

import consulo.document.Document;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QAbstractScrollArea;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;

import java.awt.Point;

/**
 * The editor surface itself: a scroll area whose viewport is painted by the editor rather than by child widgets.
 * <p>
 * Unlike the awt frontend - where {@code EditorComponentImpl} is a plain {@code JComponent} sized to the whole
 * document and a separate {@code JScrollPane} moves it around - qt keeps the surface viewport-sized and applies the
 * scroll offset while painting. The awt painter already expresses that translation through its own y shift, so the
 * paint code ports across unchanged.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorWidget extends QAbstractScrollArea {
    private final DesktopQtEditorImpl myEditor;
    private final DesktopQtEditorPainter myPainter;
    private final DesktopQtEditorKeyHandler myKeyHandler;
    private final QTimer myCaretBlinkTimer;

    private boolean myCaretBlinkOn = true;
    private boolean mySelecting;

    /**
     * Where the current selection is being dragged from. Negative when there is nothing to extend.
     */
    private int mySelectionAnchor = -1;

    public DesktopQtEditorWidget(QWidget parent, DesktopQtEditorImpl editor) {
        super(parent);
        myEditor = editor;
        myPainter = new DesktopQtEditorPainter(editor);
        myKeyHandler = new DesktopQtEditorKeyHandler(editor);

        viewport().setAutoFillBackground(false);
        setFrameShape(Shape.NoFrame);
        setFocusPolicy(Qt.FocusPolicy.StrongFocus);

        myCaretBlinkTimer = new QTimer(this);
        // qt reports a full on-off cycle, so each half of it is one toggle
        myCaretBlinkTimer.setInterval(Math.max(100, QApplication.cursorFlashTime() / 2));
        myCaretBlinkTimer.timeout.connect(this::toggleCaretBlink);
        myCaretBlinkTimer.start();
    }

    public void updateScrollRanges() {
        Document document = myEditor.getDocument();

        int lineHeight = myEditor.getLineHeight();
        int documentHeight = document.getLineCount() * lineHeight;
        int viewportHeight = viewport().height();

        verticalScrollBar().setSingleStep(lineHeight);
        verticalScrollBar().setPageStep(viewportHeight);
        verticalScrollBar().setRange(0, Math.max(0, documentHeight - viewportHeight));

        int documentWidth = myEditor.getDocumentWidth();
        int viewportWidth = viewport().width();

        horizontalScrollBar().setSingleStep((int) Math.ceil(myEditor.getFontMetrics().getSpaceWidth()));
        horizontalScrollBar().setPageStep(viewportWidth);
        horizontalScrollBar().setRange(0, Math.max(0, documentWidth - viewportWidth));
    }

    public void restartCaretBlink() {
        myCaretBlinkOn = true;
        myCaretBlinkTimer.start();

        viewport().update();
    }

    private void toggleCaretBlink() {
        myCaretBlinkOn = !myCaretBlinkOn;

        viewport().update();
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);

        updateScrollRanges();
    }

    @Override
    protected void scrollContentsBy(int dx, int dy) {
        viewport().update();
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mousePressEvent(event);
            return;
        }

        int offset = offsetAt(event);

        if (event.modifiers().testFlag(Qt.KeyboardModifier.ShiftModifier)) {
            // shift keeps whatever the selection was anchored to, so a click extends the run rather than starting one
            if (mySelectionAnchor < 0) {
                mySelectionAnchor = myEditor.getCaretModel().getOffset();
            }
        }
        else {
            mySelectionAnchor = offset;
            myEditor.getSelectionModel().removeSelection();
        }

        moveCaretTo(offset);
        mySelecting = true;
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        if (!mySelecting) {
            super.mouseMoveEvent(event);
            return;
        }

        moveCaretTo(offsetAt(event));
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        mySelecting = false;

        super.mouseReleaseEvent(event);
    }

    private void moveCaretTo(int offset) {
        myEditor.getCaretModel().moveToOffset(offset);

        if (mySelectionAnchor >= 0 && mySelectionAnchor != offset) {
            myEditor.getSelectionModel().setSelection(mySelectionAnchor, offset);
        }

        restartCaretBlink();
    }

    private int offsetAt(QMouseEvent event) {
        Point point = new Point(event.pos().x() + horizontalScrollBar().value(), event.pos().y() + verticalScrollBar().value());

        return myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(point));
    }

    @Override
    protected void keyPressEvent(QKeyEvent event) {
        if (myKeyHandler.handle(event)) {
            event.accept();
            restartCaretBlink();
            return;
        }

        super.keyPressEvent(event);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(viewport());
        try {
            myPainter.paint(
                painter,
                event.rect(),
                horizontalScrollBar().value(),
                verticalScrollBar().value(),
                myCaretBlinkOn && myEditor.isCaretVisible()
            );
        }
        finally {
            painter.end();
        }
    }
}
