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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.execution.debug.internal.breakpoint.BreakpointEditorUtil;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.action.DesktopQtActionContextMenu;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.action.IdeActions;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.image.Image;
import consulo.util.lang.ref.SimpleReference;
import io.qt.core.QEvent;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The line number strip to the left of the text.
 * <p>
 * A sibling of the viewport rather than something drawn inside it: the room it needs is taken out of the scroll
 * area with {@code setViewportMargins}, so the text never scrolls under it horizontally while both still scroll
 * together vertically. That is the arrangement qt's own editor example uses, and it keeps the gutter out of the
 * painter's clip arithmetic entirely.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorGutterWidget extends QWidget {
    private static final Logger LOG = Logger.getInstance(DesktopQtEditorGutterWidget.class);

    private static final int LEFT_PADDING = 6;
    private static final int RIGHT_PADDING = 8;

    /**
     * The strip between the numbers and the text. It carries the separator rule and is painted in the editor
     * background rather than the gutter one, so the gutter stops short of the text - the same three pixels the
     * awt gutter reserves in {@code getWhitespaceSeparatorOffset}.
     */
    private static final int SEPARATOR_AREA_WIDTH = 3;

    private static final int MARKER_AREA_PADDING = 2;

    /**
     * Smallest icon column, so a gutter which has no icons yet does not jump wider the moment it gets one - the
     * awt gutter starts from the same width.
     */
    private static final int START_ICON_AREA_WIDTH = 17;

    private static final int GAP_BETWEEN_ICONS = 3;

    /**
     * The row height the icons of the gutter are drawn at scale 1, as awt's {@code getScale} measures it.
     */
    private static final double STANDARD_LINE_HEIGHT = 16.0;

    private static final double SCALE_DEADBAND = 0.10;

    private static final double HOVER_MARK_OPACITY = 0.5;

    // EditorMouseEvent still carries an awt event, and nothing downstream reads it when input details are given
    private static final MouseEvent FAKE_MOUSE_EVENT = new MouseEvent(new JLabel("fake"), 0, 0, 0, 0, 0, 1, false);

    private final DesktopQtEditorWidget mySurface;
    private final DesktopQtEditorImpl myEditor;

    private boolean myHovered;

    private int myPressedLine = -1;

    public DesktopQtEditorGutterWidget(DesktopQtEditorWidget surface, DesktopQtEditorImpl editor) {
        super(surface);
        mySurface = surface;
        myEditor = editor;

        // without this qt only reports moves while a button is held, and the anchors have to answer a bare hover
        setMouseTracking(true);

        installPopupMenu();
    }

    /**
     * The menu of the gutter itself. Which group it is depends on where the click landed, and so does the line the
     * actions in it answer for - a breakpoint is set on the line under the pointer, not on the one holding the
     * caret - so both are resolved from the position rather than fixed when the handler is installed.
     */
    private void installPopupMenu() {
        DesktopQtActionContextMenu.installOn(
            this,
            this::gutterPopupGroup,
            ActionPlaces.EDITOR_GUTTER_POPUP,
            this::gutterDataContext
        );
    }

    /**
     * A mark standing on the line carries a menu of its own and it wins - right clicking a breakpoint asks the
     * breakpoint, not the gutter. Only when nothing on the row answers does the menu of the gutter itself apply,
     * which is the order the web frontend resolves it in.
     */
    private @Nullable ActionGroup gutterPopupGroup(QPoint position) {
        for (GutterMark mark : myEditor.getGutterComponentEx().getGutterRenderers(visualLineAt(position.y()))) {
            if (mark instanceof GutterIconRenderer renderer) {
                ActionGroup rendererGroup = renderer.getPopupMenuActions();

                if (rendererGroup != null) {
                    return rendererGroup;
                }
            }
        }

        DesktopQtEditorGutterComponentImpl gutter = (DesktopQtEditorGutterComponentImpl) myEditor.getGutterComponentEx();

        ActionGroup custom = gutter.getGutterPopupGroup();
        if (custom != null) {
            return custom;
        }

        if (!gutter.isShowDefaultGutterPopup()) {
            return null;
        }

        return CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_EDITOR_GUTTER) instanceof ActionGroup group
            ? group
            : null;
    }

    /**
     * What the actions of the menu read. The two keys the awt gutter publishes off its last actionable click are
     * the line under the pointer and the point to hang a further popup from; an action which cannot find them
     * falls back to the caret, which is the wrong line as soon as the two differ.
     */
    private DataContext gutterDataContext(QPoint position) {
        DataManager dataManager = DataManager.getInstance();

        SimpleDataContext.Builder builder = SimpleDataContext.builder()
            .setParent(dataManager.getDataContext(myEditor.getUIComponent()))
            .add(Editor.KEY, myEditor);

        int logicalLine = myEditor.getVisualLines().visualToLogicalLine(visualLineAt(position.y()));

        if (logicalLine >= 0 && logicalLine < myEditor.getDocument().getLineCount()) {
            builder.add(EditorGutter.KEY, myEditor.getGutterComponentEx())
                .add(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR, logicalLine)
                .add(EditorGutterComponentEx.ICON_CENTER_POSITION, new Point(position.x(), position.y()));
        }

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(builder.build());
    }

    private int markerAreaWidth() {
        int widest = Math.max(scaledIconSize(PlatformIconGroup.gutterFold()), scaledIconSize(PlatformIconGroup.gutterUnfold()));

        return widest + 2 * MARKER_AREA_PADDING;
    }

    @Override
    protected void enterEvent(QEnterEvent event) {
        myHovered = true;
        update();
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        int x = event.pos().x();
        int visualLine = visualLineAt(event.pos().y());

        FoldRegion region = x >= markerAreaOffset() ? foldRegionAt(visualLine) : null;

        GutterIconRenderer renderer = region == null ? gutterRendererAt(event.pos()) : null;

        boolean actionable = region != null || renderer != null && renderer.getClickAction() != null;

        setCursor(new QCursor(actionable ? Qt.CursorShape.PointingHandCursor : Qt.CursorShape.ArrowCursor));

        fireMouseMoved(event, visualLine, areaAt(x));

        event.accept();
    }

    /**
     * Tells the platform which line of the gutter the pointer is over. The breakpoint promoter listens for this
     * and answers by putting the icon it wants drawn on the component below, so a gutter which never reports a
     * move shows nothing on hover no matter what it paints.
     */
    private void fireMouseMoved(QMouseEvent event, int visualLine, EditorMouseEventArea area) {
        Document document = myEditor.getDocument();

        int logicalLine = myEditor.getVisualLines().visualToLogicalLine(visualLine);
        if (logicalLine < 0 || logicalLine >= document.getLineCount()) {
            return;
        }

        LogicalPosition logicalPosition = new LogicalPosition(logicalLine, 0);

        EditorMouseEvent editorEvent = new EditorMouseEvent(
            myEditor,
            FAKE_MOUSE_EVENT,
            DesktopQtInputDetails.mouse(this, event),
            false,
            area,
            document.getLineStartOffset(logicalLine),
            logicalPosition,
            myEditor.logicalToVisualPosition(logicalPosition),
            false,
            null,
            null,
            null
        );

        myEditor.fireMouseMoved(editorEvent);
    }

    private int visualLineAt(int y) {
        return (y + mySurface.verticalScrollBar().value()) / myEditor.getLineHeight();
    }

    @Override
    protected void leaveEvent(QEvent event) {
        myHovered = false;
        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
        update();
    }

    /**
     * The room the strip needs, which is what the surface takes out of its viewport. Zero hides it entirely,
     * since a gutter of no width still paints its background over the first pixels of the text.
     */
    public int preferredWidth() {
        if (!myEditor.getSettings().isLineNumbersShown()) {
            return 0;
        }

        return lineNumberAreaWidth() + iconsAreaWidth() + markerAreaWidth() + SEPARATOR_AREA_WIDTH;
    }

    private int lineNumberAreaWidth() {
        int lastLine = Math.max(1, myEditor.getDocument().getLineCount());
        String widest = "0".repeat(String.valueOf(lastLine).length());

        return LEFT_PADDING + (int) Math.ceil(myEditor.getFontMetrics().getTextWidth(widest)) + RIGHT_PADDING;
    }

    /**
     * How wide the icon column has to be for the busiest row in the document. Measured over every row rather than
     * the visible ones, so the text does not shift sideways while scrolling past a line carrying more icons.
     */
    private int iconsAreaWidth() {
        int widest = START_ICON_AREA_WIDTH;

        int lineCount = myEditor.getVisualLines().getVisualLineCount();
        for (int line = 0; line < lineCount; line++) {
            int width = 1;
            int count = 0;

            for (GutterMark mark : myEditor.getGutterComponentEx().getGutterRenderers(line)) {
                if (isMergedWithLineNumbers(mark)) {
                    continue;
                }

                width += scaledIconSize(mark.getIcon()) + (count > 0 ? GAP_BETWEEN_ICONS : 0);
                count++;
            }

            widest = Math.max(widest, width + 1);
        }

        return widest;
    }

    /**
     * Whether the mark is drawn over the line number instead of in the icon column. The awt gutter drops the
     * alignment back to the left when there are no numbers to draw over.
     */
    private boolean isMergedWithLineNumbers(GutterMark mark) {
        return BreakpointEditorUtil.isBreakPointsOnLineNumbers()
            && myEditor.getSettings().isLineNumbersShown()
            && mark instanceof GutterIconRenderer renderer
            && renderer.getAlignment() == GutterIconRenderer.Alignment.LINE_NUMBERS;
    }

    public int iconAreaOffset() {
        return lineNumberAreaOffset() + lineNumberAreaWidth();
    }

    private int lineNumberAreaOffset() {
        return 0;
    }

    /**
     * Where the numbers stop and the markers begin. Everything right of this belongs to the line markers as far
     * as the platform is concerned, which is the area a breakpoint is toggled in.
     */
    public int markerAreaOffset() {
        return Math.max(0, separatorOffset() - markerAreaWidth());
    }

    /**
     * Vertically centres an icon on the text of its row, preferring the baseline when the icon is taller than
     * half the row - the same rule the awt gutter uses.
     */
    private int iconAlignmentShift(int iconSize, int lineHeight) {
        return Math.max((lineHeight - iconSize) / 2, myEditor.getFontMetrics().getAscent() - iconSize);
    }

    /**
     * The icons of a row, laid out the way awt lays them out: left aligned packed from the left of the column,
     * right aligned packed from its right, and centred ones sharing what is left between them.
     */
    private void paintRowIcons(QPainter painter, int visualLine, int scrollY, int lineHeight) {
        layoutRowIcons(visualLine, visualLine * lineHeight - scrollY, lineHeight, (mark, bounds) -> paintIcon(painter, mark.getIcon(), bounds));
    }

    private void layoutRowIcons(int visualLine, int y, int lineHeight, BiConsumer<GutterMark, QRect> consumer) {
        List<GutterMark> row = myEditor.getGutterComponentEx().getGutterRenderers(visualLine);
        if (row.isEmpty()) {
            return;
        }

        int areaOffset = iconAreaOffset();
        int areaWidth = iconsAreaWidth();

        int left = areaOffset + 2;
        int right = areaOffset + areaWidth;

        int centeredWidth = 0;
        int centeredCount = 0;

        for (GutterMark mark : row) {
            if (isMergedWithLineNumbers(mark)) {
                continue;
            }

            int size = scaledIconSize(mark.getIcon());

            switch (alignmentOf(mark)) {
                case LEFT -> {
                    consumer.accept(mark, new QRect(left, y + iconAlignmentShift(size, lineHeight), size, size));
                    left += size + GAP_BETWEEN_ICONS;
                }
                case RIGHT -> {
                    right -= size;
                    consumer.accept(mark, new QRect(right, y + iconAlignmentShift(size, lineHeight), size, size));
                    right -= GAP_BETWEEN_ICONS;
                }
                default -> {
                    centeredWidth += size + GAP_BETWEEN_ICONS;
                    centeredCount++;
                }
            }
        }

        if (centeredCount == 0) {
            return;
        }

        int x = left + Math.max(0, (right - left - (centeredWidth - GAP_BETWEEN_ICONS)) / 2);

        for (GutterMark mark : row) {
            if (!isMergedWithLineNumbers(mark) && alignmentOf(mark) == GutterIconRenderer.Alignment.CENTER) {
                int size = scaledIconSize(mark.getIcon());

                consumer.accept(mark, new QRect(x, y + iconAlignmentShift(size, lineHeight), size, size));
                x += size + GAP_BETWEEN_ICONS;
            }
        }
    }

    /**
     * The mark the pointer stands on, if any. Which row it is has already been decided by the y of the point, so
     * only the column of the icon is tested - a click a few pixels above or below the icon still belongs to it,
     * which is the target the row looks like it offers.
     */
    private @Nullable GutterIconRenderer gutterRendererAt(QPoint position) {
        SimpleReference<GutterIconRenderer> found = SimpleReference.create();

        layoutRowIcons(visualLineAt(position.y()), 0, myEditor.getLineHeight(), (mark, bounds) -> {
            if (found.get() == null
                && mark instanceof GutterIconRenderer renderer
                && position.x() >= bounds.left()
                && position.x() <= bounds.right()) {
                found.set(renderer);
            }
        });

        return found.get();
    }

    /**
     * What a click on the mark under the pointer runs, or null when nothing on the row answers the button.
     */
    private @Nullable AnAction clickActionAt(QMouseEvent event) {
        GutterIconRenderer renderer = gutterRendererAt(event.pos());
        if (renderer == null) {
            return null;
        }

        return event.button() == Qt.MouseButton.MiddleButton ? renderer.getMiddleButtonClickAction() : renderer.getClickAction();
    }

    /**
     * Runs the action a gutter icon is bound to. The click ends in navigation which hangs a popup off the pointer,
     * so the details of the event travel with it - without them the popup opens at the corner of the screen.
     */
    @RequiredUIAccess
    private void performClickAction(AnAction action, QMouseEvent event) {
        DataContext context = myEditor.getDataContext();

        AnActionEvent actionEvent = AnActionEvent.createFromAnAction(
            action,
            null,
            ActionPlaces.EDITOR_GUTTER,
            context,
            DesktopQtInputDetails.mouse(this, event)
        );

        UIAccess uiAccess = UIAccess.current();

        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, actionEvent, true).whenCompleteAsync((enabled, throwable) -> {
            if (throwable != null) {
                LOG.error("Gutter click action update failed: " + action, throwable);
                return;
            }

            if (Boolean.TRUE.equals(enabled)) {
                ActionImplUtil.performActionDumbAwareWithCallbacks(action, actionEvent, context);
                update();
            }
        }, uiAccess);
    }

    /**
     * A mark asking to stand on the line number falls back to the left of the icon column when there are no
     * numbers for it to stand on.
     */
    private GutterIconRenderer.Alignment alignmentOf(GutterMark mark) {
        GutterIconRenderer.Alignment alignment = mark instanceof GutterIconRenderer renderer
            ? renderer.getAlignment()
            : GutterIconRenderer.Alignment.LEFT;

      return alignment == GutterIconRenderer.Alignment.LINE_NUMBERS ? GutterIconRenderer.Alignment.LEFT : alignment;
    }

    private void paintIcon(QPainter painter, @Nullable Image image, QRect bounds) {
        if (image instanceof DesktopQtImage qtImage) {
            qtImage.toQIcon().paint(painter, bounds);
        }
    }

    /**
     * Where the gutter stops and the strip belonging to the editor begins. The awt gutter answers the same
     * question for hit testing and for the free painters, so the platform can ask this one too.
     */
    public int separatorOffset() {
        return Math.max(0, width() - SEPARATOR_AREA_WIDTH);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter, event);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter, QPaintEvent event) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        Document document = myEditor.getDocument();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        int separatorX = separatorOffset();

        // the gutter owns everything left of the separator; the strip right of it belongs to the text, so the
        // two backgrounds meet at the rule rather than the gutter running under the first pixels of the line
        ColorValue background = scheme.getColor(EditorColors.EDITOR_GUTTER_BACKGROUND);
        painter.fillRect(0, 0, separatorX, height(), TargetQt.to(background == null ? scheme.getDefaultBackground() : background));
        painter.fillRect(separatorX, 0, width() - separatorX, height(), TargetQt.to(scheme.getDefaultBackground()));

        ColorValue separatorColor = scheme.getColor(EditorColors.INDENT_GUIDE_COLOR);
        if (separatorColor != null) {
            painter.fillRect(separatorX, 0, 1, height(), TargetQt.to(separatorColor));
        }

        if (!myEditor.getSettings().isLineNumbersShown()) {
            return;
        }

        int lineHeight = metrics.getLineHeight();
        int scrollY = mySurface.verticalScrollBar().value();

        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        int firstLine = Math.max(0, (event.rect().top() + scrollY) / lineHeight);
        int lastLine = Math.min(visualLines.getVisualLineCount() - 1, (event.rect().bottom() + scrollY) / lineHeight);

        if (lastLine < firstLine) {
            return;
        }

        ColorValue numberColor = scheme.getColor(EditorColors.LINE_NUMBERS_COLOR);
        ColorValue caretRowNumberColor = scheme.getColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR);

        QColor plain = TargetQt.to(numberColor == null ? scheme.getDefaultForeground() : numberColor);
        QColor onCaretRow = caretRowNumberColor == null ? plain : TargetQt.to(caretRowNumberColor);

        Set<Integer> caretRows = caretRows();

        painter.setFont(metrics.getFont(Font.PLAIN));

        int right = lineNumberAreaOffset() + lineNumberAreaWidth() - RIGHT_PADDING;
        int baselineOffset = metrics.getAscent();

        int hoverLine = hoverMarkLine();

        for (int line = firstLine; line <= lastLine; line++) {
            // a breakpoint standing on the line takes the place of its number, and one merely being offered
            // under the pointer does the same but faintly - the number is only drawn when neither is there
            Image lineNumberIcon = lineNumbersIcon(line);
            if (lineNumberIcon != null) {
                paintLineNumberIcon(painter, lineNumberIcon, line, scrollY, lineHeight, 1.0);
                continue;
            }

            if (line == hoverLine && paintHoverMark(painter, line, scrollY, lineHeight)) {
                continue;
            }

            // rows are numbered by the line of the file they show, so a collapsed region makes the numbers jump
            // rather than run on - and the strip counts from one while the editor counts from zero
            String number = String.valueOf(visualLines.visualToLogicalLine(line) + 1);
            double x = right - metrics.getTextWidth(number);

            painter.setPen(caretRows.contains(line) ? onCaretRow : plain);
            painter.drawText(new QPointF(x, line * lineHeight - scrollY + baselineOffset), number);
        }

        for (int line = firstLine; line <= lastLine; line++) {
            paintRowIcons(painter, line, scrollY, lineHeight);
        }

        paintFoldAnchors(painter, firstLine, lastLine, scrollY, lineHeight);
    }

    /**
     * How much bigger or smaller than the standard row every icon in the gutter is drawn, so they follow the
     * editor font rather than staying at whatever size the svg happens to declare. Ported from awt's
     * {@code getEditorScaleFactor}, and it applies to every icon here rather than to one of them.
     */
    private double iconScale() {
        if (!Registry.is("editor.scale.gutter.icons")) {
            return 1;
        }

        float lineSpacing = myEditor.getColorsScheme().getLineSpacing();
        double normalizedLineHeight = myEditor.getLineHeight() / (lineSpacing <= 0 ? 1f : lineSpacing);

        double scale = normalizedLineHeight / STANDARD_LINE_HEIGHT;

        // awt leaves an icon at its own size unless the row differs from the standard one by more than a tenth,
        // so a row a pixel or two off does not blow every icon up
        return Math.abs(1 - scale) > SCALE_DEADBAND ? scale : 1;
    }

    private int scaledIconSize(@Nullable Image image) {
        if (image == null) {
            return 0;
        }

        return Math.max(1, (int) Math.round(image.getWidth() * iconScale()));
    }

    /**
     * The row the platform is offering a breakpoint on, or -1. Only while the pointer is in the gutter: the
     * offer belongs to the hover, and awt gates it on the same thing.
     */
    private int hoverMarkLine() {
        if (!myHovered) {
            return -1;
        }

        JComponent properties = myEditor.getGutterComponentEx().getComponent();

        return properties.getClientProperty("active.line.number") instanceof Integer logicalLine
            ? myEditor.getVisualLines().logicalToVisualLine(logicalLine)
            : -1;
    }

    /**
     * The icon of a mark which asked to stand where the line number goes - which is where a breakpoint sits once
     * it is set, while the setting to show them over the numbers is on.
     */
    private @Nullable Image lineNumbersIcon(int visualLine) {
        if (!BreakpointEditorUtil.isBreakPointsOnLineNumbers()) {
            return null;
        }

        for (GutterMark mark : myEditor.getGutterComponentEx().getGutterRenderers(visualLine)) {
            if (mark instanceof GutterIconRenderer renderer
                && renderer.getAlignment() == GutterIconRenderer.Alignment.LINE_NUMBERS) {
                return renderer.getIcon();
            }
        }

        return null;
    }

    private boolean paintHoverMark(QPainter painter, int visualLine, int scrollY, int lineHeight) {
        JComponent properties = myEditor.getGutterComponentEx().getComponent();

        if (!(properties.getClientProperty("line.number.hover.icon") instanceof Image image)) {
            return false;
        }

        // the breakpoint is only being offered, not set, so it is drawn faint the way awt draws it
        return paintLineNumberIcon(painter, image, visualLine, scrollY, lineHeight, HOVER_MARK_OPACITY);
    }

    private boolean paintLineNumberIcon(
        QPainter painter,
        Image image,
        int visualLine,
        int scrollY,
        int lineHeight,
        double opacity
    ) {
        if (!(image instanceof DesktopQtImage qtImage)) {
            return false;
        }

        int size = scaledIconSize(image);
        int y = visualLine * lineHeight - scrollY + (lineHeight - size) / 2;

        painter.save();
        painter.setOpacity(opacity);
        qtImage.toQIcon().paint(painter, new QRect(Math.max(0, lineNumberAreaOffset() + lineNumberAreaWidth() - RIGHT_PADDING - size), y, size, size));
        painter.restore();

        return true;
    }

    private void paintFoldAnchors(QPainter painter, int firstLine, int lastLine, int scrollY, int lineHeight) {
        if (!myHovered) {
            return;
        }

        int areaOffset = markerAreaOffset();
        int areaWidth = markerAreaWidth();

        for (int line = firstLine; line <= lastLine; line++) {
            FoldRegion region = foldRegionAt(line);
            if (region == null) {
                continue;
            }

            Image image = region.isExpanded() ? PlatformIconGroup.gutterFold() : PlatformIconGroup.gutterUnfold();
            if (!(image instanceof DesktopQtImage qtImage)) {
                continue;
            }

            int iconWidth = Math.min(scaledIconSize(image), areaWidth - 2 * MARKER_AREA_PADDING);
            int iconHeight = Math.min(iconWidth, lineHeight);

            int x = areaOffset + (areaWidth - iconWidth) / 2;
            int y = line * lineHeight - scrollY + (lineHeight - iconHeight) / 2;

            qtImage.toQIcon().paint(painter, new QRect(x, y, iconWidth, iconHeight));
        }
    }

    /**
     * The region that starts on the given row, or null when nothing on it can be folded. Only the row a region
     * begins on carries an anchor, which is what makes a collapsed region show exactly one.
     */
    private @Nullable FoldRegion foldRegionAt(int visualLine) {
        Document document = myEditor.getDocument();

        FoldRegion[] regions = ((FoldingModelEx) myEditor.getFoldingModel()).fetchVisible();
        if (regions == null) {
            return null;
        }

        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        for (FoldRegion region : regions) {
            if (!region.isValid() || region.shouldNeverExpand()) {
                continue;
            }

            int startOffset = region.getStartOffset();

            // a region living inside one line carries no anchor unless it asked for one
            if (document.getLineNumber(startOffset) == document.getLineNumber(region.getEndOffset())
                && !region.isGutterMarkEnabledForSingleLine()) {
                continue;
            }

            // the row the region opens on, which is not the row of its start line once folding moved it
            if (visualLines.offsetToVisualLine(startOffset) == visualLine && leadsGroup(region, regions)) {
                return region;
            }
        }

        return null;
    }

    /**
     * Whether this is the region an anchor should be drawn for. A one line method is folded by several regions
     * banded into a group - one hiding the break after the brace, another the break before the closing one - and
     * they open and close as a unit, so the group answers with one anchor rather than one per region.
     */
    private static boolean leadsGroup(FoldRegion region, FoldRegion[] regions) {
        FoldingGroup group = region.getGroup();
        if (group == null) {
            return true;
        }

        for (FoldRegion other : regions) {
            if (other != region && group.equals(other.getGroup()) && other.getStartOffset() < region.getStartOffset()) {
                return false;
            }
        }

        return true;
    }

    private void toggleFold(FoldRegion region) {
        FoldingModelEx foldingModel = (FoldingModelEx) myEditor.getFoldingModel();

        FoldingGroup group = region.getGroup();
        List<FoldRegion> grouped = group == null ? List.of(region) : foldingModel.getGroupedRegions(group);

        boolean expanded = !region.isExpanded();

        foldingModel.runBatchFoldingOperation(() -> {
            for (FoldRegion grouping : grouped) {
                grouping.setExpanded(expanded);
            }
        });
    }

    /**
     * Clicks are handed to the platform as editor mouse events naming the area they landed in, which is how
     * anything that lives in the gutter hears about them - a breakpoint is toggled by the debugger listening for
     * a click in the line marker area, not by the gutter knowing what a breakpoint is.
     */
    @Override
    protected void mousePressEvent(QMouseEvent event) {
        int x = event.pos().x();
        int visualLine = visualLineAt(event.pos().y());

        // an anchor takes the click for itself, since folding is the gutter's own business
        if (x >= markerAreaOffset() && event.button() == Qt.MouseButton.LeftButton) {
            FoldRegion region = foldRegionAt(visualLine);
            if (region != null) {
                toggleFold(region);

                event.accept();
                return;
            }
        }

        myPressedLine = visualLine;

        EditorMouseEvent editorEvent = gutterMouseEvent(event, visualLine, areaAt(x));
        if (editorEvent != null) {
            myEditor.fireMousePressed(editorEvent);
        }

        event.accept();
    }

    /**
     * The event the platform listeners read. It has to carry the real logical position: the short constructor of
     * {@link EditorMouseEvent} fills a zero one, and everything reading a line off a gutter click - toggling a
     * breakpoint above all - takes it from there whenever input details are present.
     */
    private @Nullable EditorMouseEvent gutterMouseEvent(QMouseEvent event, int visualLine, EditorMouseEventArea area) {
        Document document = myEditor.getDocument();

        int logicalLine = myEditor.getVisualLines().visualToLogicalLine(visualLine);
        if (logicalLine < 0 || logicalLine >= document.getLineCount()) {
            return null;
        }

        LogicalPosition logicalPosition = new LogicalPosition(logicalLine, 0);

        return new EditorMouseEvent(
            myEditor,
            FAKE_MOUSE_EVENT,
            DesktopQtInputDetails.mouse(this, event),
            event.button() == Qt.MouseButton.RightButton,
            area,
            document.getLineStartOffset(logicalLine),
            logicalPosition,
            myEditor.logicalToVisualPosition(logicalPosition),
            false,
            null,
            null,
            null
        );
    }

    private EditorMouseEventArea areaAt(int x) {
        int numbers = lineNumberAreaOffset();

        return x >= numbers && x < numbers + lineNumberAreaWidth()
            ? EditorMouseEventArea.LINE_NUMBERS_AREA
            : EditorMouseEventArea.LINE_MARKERS_AREA;
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        int visualLine = visualLineAt(event.pos().y());

        // the mark takes the click for itself, the way the awt gutter consumes one it found an action for -
        // otherwise the same click both navigates and toggles whatever the platform binds to the marker area
        if (visualLine == myPressedLine
            && (event.button() == Qt.MouseButton.LeftButton || event.button() == Qt.MouseButton.MiddleButton)) {
            AnAction action = clickActionAt(event);
            if (action != null) {
                myPressedLine = -1;

                performClickAction(action, event);

                event.accept();
                return;
            }
        }

        EditorMouseEvent editorEvent = gutterMouseEvent(event, visualLine, areaAt(event.pos().x()));
        if (editorEvent != null) {
            myEditor.fireMouseReleased(editorEvent);

            // a press followed by a release over the same row is the click the platform is waiting for
            if (visualLine == myPressedLine) {
                myEditor.fireMouseClicked(editorEvent);
            }
        }

        myPressedLine = -1;

        event.accept();
    }

    private Set<Integer> caretRows() {
        Set<Integer> lines = new HashSet<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            lines.add(caret.getVisualPosition().line);
        }
        return lines;
    }
}
