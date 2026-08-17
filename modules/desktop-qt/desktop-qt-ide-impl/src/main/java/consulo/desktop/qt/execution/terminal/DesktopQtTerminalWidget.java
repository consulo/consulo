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
package consulo.desktop.qt.execution.terminal;

import com.jediterm.core.compatibility.Point;
import com.jediterm.terminal.StyledTextConsumer;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.model.CharBuffer;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.SelectionUtil;
import com.jediterm.terminal.model.TerminalSelection;
import com.jediterm.terminal.model.TerminalTextBuffer;
import io.qt.core.QPointF;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QFontDatabase;
import io.qt.gui.QFontMetricsF;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QResizeEvent;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The screen of a terminal, drawn from the {@link TerminalTextBuffer} the emulator writes into - the qt counterpart
 * of the swing {@code TerminalPanel} the awt frontend runs on, which cannot be reused because it draws with swing.
 * <p/>
 * The model, the arithmetic of the scrollback and the way a row is handed over as runs of one style are jediterm's
 * and are kept as they are; what is written here is the drawing and the input.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTerminalWidget extends QWidget {
    private static final int ourWheelRows = 3;

    private final TerminalTextBuffer myTextBuffer;
    private final JediTerminal myTerminal;

    private final Consumer<String> myInputConsumer;
    private final BiConsumer<Integer, Integer> myResizeConsumer;

    private final DesktopQtTerminalPalette myPalette = DesktopQtTerminalPalette.ofGlobalScheme();

    private QColor myDefaultForeground = new QColor(0xCC, 0xCC, 0xCC);
    private QColor myDefaultBackground = new QColor(0x1E, 0x1E, 0x1E);

    private QFont myFont;
    private double myCellWidth;
    private double myCellHeight;
    private double myAscent;

    /**
     * The first line of the buffer which is drawn, counted from the top of the scrollback. Zero is the screen as
     * the process sees it, and a negative value has walked back into what scrolled off.
     */
    private int myScrollOrigin;

    /** what the pointer has marked out, which the ui owns rather than the emulator */
    private @Nullable TerminalSelection mySelection;

    private @Nullable Point mySelectionAnchor;

    public DesktopQtTerminalWidget(
        @Nullable QWidget parent,
        TerminalTextBuffer textBuffer,
        JediTerminal terminal,
        Consumer<String> inputConsumer,
        BiConsumer<Integer, Integer> resizeConsumer
    ) {
        super(parent);

        myTextBuffer = textBuffer;
        myTerminal = terminal;
        myInputConsumer = inputConsumer;
        myResizeConsumer = resizeConsumer;

        setFocusPolicy(Qt.FocusPolicy.StrongFocus);
        setAutoFillBackground(false);

        applyFont(QFontDatabase.systemFont(QFontDatabase.SystemFont.FixedFont));
    }

    public final void applyFont(QFont font) {
        myFont = font;
        setFont(font);

        QFontMetricsF metrics = new QFontMetricsF(font);

        // every cell of a terminal is the same width whatever stands in it, so the advance of one glyph decides
        // the grid rather than the string being drawn
        myCellWidth = metrics.horizontalAdvance("W");
        myCellHeight = metrics.height();
        myAscent = metrics.ascent();

        update();
    }

    public int columnCount() {
        return myCellWidth <= 0 ? 0 : Math.max(1, (int) (width() / myCellWidth));
    }

    public int rowCount() {
        return myCellHeight <= 0 ? 0 : Math.max(1, (int) (height() / myCellHeight));
    }

    public int getScrollOrigin() {
        return myScrollOrigin;
    }

    public void setScrollOrigin(int scrollOrigin) {
        int lowest = -myTextBuffer.getHistoryLinesCount();

        myScrollOrigin = Math.max(lowest, Math.min(0, scrollOrigin));

        update();
    }

    @Override
    public QSize sizeHint() {
        return new QSize((int) (myCellWidth * myTextBuffer.getWidth()), (int) (myCellHeight * myTextBuffer.getHeight()));
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);

        myResizeConsumer.accept(columnCount(), rowCount());

        setScrollOrigin(myScrollOrigin);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        myDefaultForeground = toColor(myPalette.getDefaultForeground());
        myDefaultBackground = toColor(myPalette.getDefaultBackground());

        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
            painter.fillRect(rect(), defaultBackground());
            painter.setFont(myFont);

            myTextBuffer.lock();
            try {
                int rows = Math.min(myTextBuffer.getHeight(), rowCount());

                // the history and the screen as one run of lines, so scrolling back is the same walk with another
                // origin rather than a second way of drawing
                myTextBuffer.processHistoryAndScreenLines(myScrollOrigin, rows, new StyledTextConsumer() {
                    @Override
                    public void consume(int x, int y, TextStyle style, CharBuffer characters, int startRow) {
                        int row = y - startRow;
                        String text = characters.toString();

                        drawRun(painter, x, row, style, text);
                        drawSelectedPart(painter, x, row, style, text);
                    }

                    @Override
                    public void consumeNul(int x, int y, int nulIndex, TextStyle style, CharBuffer characters, int startRow) {
                        // the tail of a line the process never wrote to, which still carries the background of the
                        // style it was left in
                        int row = y - startRow;
                        String text = " ".repeat(characters.length());

                        if (intersectSelection(nulIndex, row, columnCount() - nulIndex) != null) {
                            drawRun(painter, x, row, inverse(style), text);
                            return;
                        }

                        drawRun(painter, x, row, style, text);
                    }

                    @Override
                    public void consumeQueue(int x, int y, int nulIndex, int startRow) {
                        int columns = columnCount();
                        if (x < columns) {
                            consumeNul(x, y, nulIndex, TextStyle.EMPTY, new CharBuffer(' ', columns - x), startRow);
                        }
                    }
                });

                drawCursor(painter);
            }
            finally {
                myTextBuffer.unlock();
            }
        }
        finally {
            painter.end();
        }
    }

    private TerminalSelection.@Nullable IntersectResult intersectSelection(int column, int row, int length) {
        TerminalSelection selection = mySelection;
        if (selection == null || length <= 0) {
            return null;
        }

        return selection.intersect(column, row + myScrollOrigin, length);
    }

    private void drawSelectedPart(QPainter painter, int column, int row, TextStyle style, String text) {
        TerminalSelection.IntersectResult interval = intersectSelection(column, row, text.length());
        if (interval == null) {
            return;
        }

        int from = interval.x() - column;
        if (from < 0 || from >= text.length()) {
            return;
        }

        int to = Math.min(from + interval.length(), text.length());
        if (to <= from) {
            return;
        }

        drawRun(painter, interval.x(), row, inverse(style), text.substring(from, to));
    }

    public void setSelection(@Nullable TerminalSelection selection) {
        mySelection = selection;

        update();
    }

    public @Nullable TerminalSelection getSelection() {
        return mySelection;
    }

    private static TextStyle inverse(TextStyle style) {
        TextStyle.Builder builder = style.toBuilder();
        builder.setOption(TextStyle.Option.INVERSE, !style.hasOption(TextStyle.Option.INVERSE));
        return builder.build();
    }

    private void drawRun(QPainter painter, int column, int row, TextStyle style, String text) {
        if (text.isEmpty() || row < 0) {
            return;
        }

        double x = column * myCellWidth;
        double y = row * myCellHeight;

        QColor background = effectiveBackground(style);
        QColor foreground = effectiveForeground(style);

        painter.fillRect(new QRectF(x, y, myCellWidth * text.length(), myCellHeight), background);

        if (text.isBlank()) {
            return;
        }

        painter.setPen(foreground);
        painter.setFont(fontOf(style));

        // one cell at a time, so a proportional glyph which slipped into the font cannot push the rest of the row
        // out of the grid
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\u0000') {
                continue;
            }

            // the point of a drawn string is its baseline, not the corner of the cell it stands in
            painter.drawText(new QPointF(x + i * myCellWidth, y + myAscent), String.valueOf(c));
        }

        if (style.hasOption(TextStyle.Option.UNDERLINED)) {
            double lineY = y + myAscent + 2;
            painter.drawLine((int) x, (int) lineY, (int) (x + myCellWidth * text.length()), (int) lineY);
        }
    }

    private QFont fontOf(TextStyle style) {
        QFont font = new QFont(myFont);
        font.setBold(style.hasOption(TextStyle.Option.BOLD));
        font.setItalic(style.hasOption(TextStyle.Option.ITALIC));
        return font;
    }

    private QColor effectiveForeground(TextStyle style) {
        return style.hasOption(TextStyle.Option.INVERSE) ? background(style) : foreground(style);
    }

    private QColor effectiveBackground(TextStyle style) {
        return style.hasOption(TextStyle.Option.INVERSE) ? foreground(style) : background(style);
    }

    private QColor foreground(TextStyle style) {
        TerminalColor color = style.getForeground();
        return color == null ? defaultForeground() : toColor(myPalette.getForeground(color));
    }

    private QColor background(TextStyle style) {
        TerminalColor color = style.getBackground();
        return color == null ? defaultBackground() : toColor(myPalette.getBackground(color));
    }

    private static QColor toColor(com.jediterm.core.Color color) {
        return new QColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    private QColor defaultForeground() {
        return myDefaultForeground;
    }

    private QColor defaultBackground() {
        return myDefaultBackground;
    }

    /**
     * The caret, which stands where the emulator says it does and moves with the scrollback rather than with the
     * screen - walking back through the history leaves it behind.
     */
    private void drawCursor(QPainter painter) {
        int row = myTerminal.getCursorY() - 1 - myScrollOrigin;
        if (row < 0 || row >= rowCount()) {
            return;
        }

        double x = myTerminal.getCursorX() * myCellWidth;
        double y = row * myCellHeight;

        painter.save();
        painter.setCompositionMode(QPainter.CompositionMode.RasterOp_SourceXorDestination);
        painter.fillRect(new QRectF(x, y, myCellWidth, myCellHeight), defaultForeground());
        painter.restore();
    }

    @Override
    protected void wheelEvent(QWheelEvent event) {
        int degrees = event.angleDelta().y();
        if (degrees == 0) {
            super.wheelEvent(event);
            return;
        }

        setScrollOrigin(myScrollOrigin - Integer.signum(degrees) * ourWheelRows);

        event.accept();
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mousePressEvent(event);
            return;
        }

        mySelectionAnchor = pointAt(event.position());
        setSelection(null);

        setFocus();
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        Point anchor = mySelectionAnchor;
        if (anchor == null || !event.buttons().testFlag(Qt.MouseButton.LeftButton)) {
            super.mouseMoveEvent(event);
            return;
        }

        TerminalSelection selection = mySelection;
        if (selection == null) {
            selection = new TerminalSelection(new Point(anchor.x, anchor.y));
            mySelection = selection;
        }

        selection.updateEnd(pointAt(event.position()));

        update();
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        mySelectionAnchor = null;

        super.mouseReleaseEvent(event);
    }

    private Point pointAt(QPointF position) {
        int column = myCellWidth <= 0 ? 0 : (int) (position.x() / myCellWidth);
        int row = myCellHeight <= 0 ? 0 : (int) (position.y() / myCellHeight);

        return new Point(
            Math.max(0, Math.min(column, Math.max(0, columnCount() - 1))),
            Math.min(row, Math.max(0, rowCount() - 1)) + myScrollOrigin
        );
    }

    public @Nullable String getSelectionText() {
        TerminalSelection selection = mySelection;
        if (selection == null) {
            return null;
        }

        myTextBuffer.lock();
        try {
            return SelectionUtil.getSelectionText(selection, myTextBuffer);
        }
        finally {
            myTextBuffer.unlock();
        }
    }

    private boolean copySelection() {
        String text = getSelectionText();
        if (text == null || text.isEmpty()) {
            return false;
        }

        QGuiApplication.clipboard().setText(text);

        return true;
    }

    private boolean pasteClipboard() {
        String text = QGuiApplication.clipboard().text();
        if (text == null || text.isEmpty()) {
            return false;
        }

        setScrollOrigin(0);

        myInputConsumer.accept(text);

        return true;
    }

    private boolean handleClipboardKey(QKeyEvent event) {
        if (!event.modifiers().testFlag(Qt.KeyboardModifier.ControlModifier)) {
            return false;
        }

        int key = event.key();
        if (key == Qt.Key.Key_C.value()) {
            return copySelection();
        }
        if (key == Qt.Key.Key_V.value()) {
            return pasteClipboard();
        }

        return false;
    }

    @Override
    protected void keyPressEvent(QKeyEvent event) {
        if (handleClipboardKey(event)) {
            event.accept();
            return;
        }

        String sequence = toSequence(event);
        if (sequence == null) {
            super.keyPressEvent(event);
            return;
        }

        // anything typed puts the screen back where the process is writing, the way every terminal behaves
        setScrollOrigin(0);

        myInputConsumer.accept(sequence);
    }

    /**
     * What a key press means to the process on the other end. A terminal speaks bytes, so a key standing for
     * something other than the character it types - an arrow, or a control combination - is written out as the
     * escape sequence the pty expects for it.
     */
    private static @Nullable String toSequence(QKeyEvent event) {
        int key = event.key();

        if (event.modifiers().testFlag(Qt.KeyboardModifier.ControlModifier)) {
            if (key >= Qt.Key.Key_A.value() && key <= Qt.Key.Key_Z.value()) {
                return String.valueOf((char) (key - Qt.Key.Key_A.value() + 1));
            }
        }

        if (key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value()) {
            return "\r";
        }
        if (key == Qt.Key.Key_Backspace.value()) {
            return "\u007f";
        }
        if (key == Qt.Key.Key_Tab.value()) {
            return "\t";
        }
        if (key == Qt.Key.Key_Escape.value()) {
            return "\u001b";
        }
        if (key == Qt.Key.Key_Up.value()) {
            return "\u001b[A";
        }
        if (key == Qt.Key.Key_Down.value()) {
            return "\u001b[B";
        }
        if (key == Qt.Key.Key_Right.value()) {
            return "\u001b[C";
        }
        if (key == Qt.Key.Key_Left.value()) {
            return "\u001b[D";
        }
        if (key == Qt.Key.Key_Home.value()) {
            return "\u001b[H";
        }
        if (key == Qt.Key.Key_End.value()) {
            return "\u001b[F";
        }
        if (key == Qt.Key.Key_Delete.value()) {
            return "\u001b[3~";
        }
        if (key == Qt.Key.Key_PageUp.value()) {
            return "\u001b[5~";
        }
        if (key == Qt.Key.Key_PageDown.value()) {
            return "\u001b[6~";
        }

        String text = event.text();
        return text == null || text.isEmpty() ? null : text;
    }
}
