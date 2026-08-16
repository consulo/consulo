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

import consulo.codeEditor.Caret;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import io.qt.core.QSize;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QAbstractScrollArea;
import io.qt.widgets.QApplication;
import io.qt.widgets.QSizePolicy;
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
    private final DesktopQtEditorGutterWidget myGutter;
    private final DesktopQtEditorErrorStripeWidget myErrorStripe;
    private final DesktopQtEditorStatusPanelWidget myStatusPanel;
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
        myGutter = new DesktopQtEditorGutterWidget(this, editor);
        myErrorStripe = new DesktopQtEditorErrorStripeWidget(this, editor);
        myStatusPanel = new DesktopQtEditorStatusPanelWidget(this, editor);

        viewport().setAutoFillBackground(false);
        viewport().setCursor(new QCursor(Qt.CursorShape.IBeamCursor));
        setFrameShape(Shape.NoFrame);
        setFocusPolicy(Qt.FocusPolicy.StrongFocus);
        setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding);

        myCaretBlinkTimer = new QTimer(this);
        // qt reports a full on-off cycle, so each half of it is one toggle
        myCaretBlinkTimer.setInterval(Math.max(100, QApplication.cursorFlashTime() / 2));
        myCaretBlinkTimer.timeout.connect(this::toggleCaretBlink);
        myCaretBlinkTimer.start();
    }

    public DesktopQtEditorGutterWidget getGutter() {
        return myGutter;
    }

    public DesktopQtEditorErrorStripeWidget getErrorStripe() {
        return myErrorStripe;
    }

    public DesktopQtEditorStatusPanelWidget getStatusPanel() {
        return myStatusPanel;
    }

    /**
     * Takes the room the gutter and the error strip need out of the viewport and puts them in it. Called whenever
     * anything either width depends on moves - the line count growing a digit widens the gutter, and the text has
     * to step aside for that.
     */
    public void updateSideAreas() {
        int gutterWidth = myGutter.preferredWidth();
        int stripeWidth = myErrorStripe.preferredWidth();

        if (viewportMargins().left() != gutterWidth || viewportMargins().right() != stripeWidth) {
            setViewportMargins(gutterWidth, 0, stripeWidth, 0);
        }

        myGutter.setGeometry(0, 0, gutterWidth, height());
        myGutter.setVisible(gutterWidth > 0);
        myGutter.update();

        myErrorStripe.setGeometry(width() - stripeWidth, 0, stripeWidth, height());
        myErrorStripe.setVisible(stripeWidth > 0);
        myErrorStripe.update();

        // the counters float over the top right of the text, clear of the strip - they are not given room of
        // their own, since taking a line off the top of every editor for them would be worse than overlapping
        int statusWidth = myStatusPanel.preferredWidth();
        if (statusWidth > 0) {
            int statusHeight = myStatusPanel.preferredHeight();

            myStatusPanel.setGeometry(width() - stripeWidth - statusWidth, 0, statusWidth, statusHeight);
            myStatusPanel.raise();
        }
    }

    public void updateScrollRanges() {
        Document document = myEditor.getDocument();

        int lineHeight = myEditor.getLineHeight();
        // rows on screen, not lines in the file - a collapsed region takes several of the second and one of the first
        int documentHeight = myEditor.getVisualLines().getVisualLineCount() * lineHeight;
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

    /**
     * Repaints the band a range of lines occupies and nothing else, the counterpart of the awt {@code doRepaint}.
     * <p>
     * The whole surface is one widget, so a repaint that named no range would redraw every visible line - which
     * is what made a selection drag or a highlighting pass crawl. Qt coalesces the regions of several
     * {@code update} calls before the next paint, so asking often is cheap.
     */
    public void repaintLines(int startLine, int endLine) {
        int lineHeight = myEditor.getLineHeight();
        int scrollY = verticalScrollBar().value();

        int y = Math.min(startLine, endLine) * lineHeight - scrollY;
        int height = (Math.abs(endLine - startLine) + 1) * lineHeight;

        // a caret sits at the very edge of its line and an underline may sit just below it, so the band is
        // stretched by a pixel on each side rather than clipping either away
        viewport().update(0, y - 1, viewport().width(), height + 2);
        myGutter.update(0, y - 1, myGutter.width(), height + 2);
    }

    /**
     * Puts the caret back on and restarts its cycle, without saying what to redraw - the caller repaints the
     * lines it touched. A blink that reset the timer and repainted everything would undo the incremental repaint.
     */
    public void restartCaretBlink() {
        myCaretBlinkOn = true;
        myCaretBlinkTimer.start();
    }

    private void toggleCaretBlink() {
        myCaretBlinkOn = !myCaretBlinkOn;

        repaintCarets();
    }

    /**
     * Redraws only the rows the carets are on. The blink fires twice a second forever, so repainting the whole
     * surface for it kept the editor busy even while nothing was happening.
     */
    public void repaintCarets() {
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            int line = caret.getLogicalPosition().line;

            repaintLines(line, line);
        }
    }

    /**
     * A scroll area asks for room enough to show a couple of hundred pixels of its content and refuses to go below
     * what its frame and scroll bars occupy. Either would be answered by the container growing, which is backwards
     * here: it is the window that decides how much of the document is on screen, and the surface scrolls to the
     * rest. So the surface names no size of its own and takes whatever the layout has left.
     */
    @Override
    public QSize sizeHint() {
        return new QSize(0, 0);
    }

    @Override
    public QSize minimumSizeHint() {
        return new QSize(0, 0);
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);

        updateSideAreas();
        updateScrollRanges();
    }

    @Override
    protected void scrollContentsBy(int dx, int dy) {
        viewport().update();

        // the strip scrolls with the text vertically and stays put horizontally
        if (dy != 0) {
            myGutter.update();
        }
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == Qt.MouseButton.RightButton) {
            // the menu is built out of the data context, so the caret has to have reached the click before qt
            // asks for it. a click inside the selection is meant for the selection and leaves the caret alone
            placeCaretForContextMenu(offsetAt(event));

            super.mousePressEvent(event);
            return;
        }

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

        // qt keeps sending moves to whoever took the press, and dropping the press would hand the drag to the
        // scroll area instead
        event.accept();
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        if (!mySelecting) {
            super.mouseMoveEvent(event);
            return;
        }

        moveCaretTo(offsetAt(event));

        event.accept();
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        mySelecting = false;

        super.mouseReleaseEvent(event);
    }

    @Override
    protected void mouseDoubleClickEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mouseDoubleClickEvent(event);
            return;
        }

        EditorSettings settings = myEditor.getSettings();

        myEditor.getCaretModel().moveToOffset(offsetAt(event));
        myEditor.getCaretModel().getCurrentCaret()
            .selectWordAtCaret(settings.isMouseClickSelectionHonorsCamelWords() && settings.isCamelWords());

        // the word the click landed on is the whole selection, so a drag going on from here starts over rather
        // than stretching what the first click of the double click anchored
        mySelectionAnchor = -1;
        mySelecting = false;

        restartCaretBlink();

        event.accept();
    }

    private void placeCaretForContextMenu(int offset) {
        SelectionModel selectionModel = myEditor.getSelectionModel();

        if (selectionModel.hasSelection() && offset >= selectionModel.getSelectionStart() && offset <= selectionModel.getSelectionEnd()) {
            return;
        }

        selectionModel.removeSelection();
        mySelectionAnchor = offset;

        myEditor.getCaretModel().moveToOffset(offset);

        restartCaretBlink();
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
