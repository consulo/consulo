package consulo.ide.impl.codeInsight.codeVision.ui.renderers;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.CodeVisionListData;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters.CodeVisionListPainter;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters.CodeVisionTheme;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.language.editor.codeVision.ClickableTextCodeVisionEntry;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.ui.ex.RelativePoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public abstract class CodeVisionInlayRendererBase implements CodeVisionInlayRenderer {
    private boolean isHovered = false;
    private @Nullable CodeVisionEntry hoveredEntry = null;
    protected final CodeVisionListPainter painter;
    protected Inlay<?> inlay;

    // Debounce timer for tooltip (1000 ms delay, same as JetBrains)
    private @Nullable Timer tooltipTimer = null;
    private @Nullable IdeTooltip currentTooltip = null;
    private @Nullable VisibleAreaListener tooltipAreaListener = null;

    protected CodeVisionInlayRendererBase() {
        this(new CodeVisionTheme());
    }

    protected CodeVisionInlayRendererBase(CodeVisionTheme theme) {
        this.painter = CodeVisionListPainterFactory.getDefault().createCodeVisionListPainter(theme);
    }

    public void initialize(Inlay<?> inlay) {
        assert this.inlay == null : "Inlay already defined for current renderer";
        this.inlay = inlay;
    }

    @Override
    public void paint(Inlay<?> inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        if (!inlay.isValid()) return;

        CodeVisionListData userData = inlay.getUserData(CodeVisionListData.KEY);
        if (userData != null) {
            userData.setPainted(true);
        }

        painter.paint(
            inlay.getEditor(),
            textAttributes,
            g,
            userData,
            getPoint(inlay, targetRegion.getLocation()),
            userData != null ? userData.rangeCodeVisionModel.state() : RangeCodeVisionModel.InlayState.NORMAL,
            isHovered && userData != null && userData.isMoreLensActive(),
            hoveredEntry
        );
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        updateMouseState(true, translated);
    }

    @Override
    public void mouseExited() {
        updateMouseState(false, null);
    }

    // Consuming to prevent showing context menu
    @Override
    public void mousePressed(MouseEvent event, Point translated) {
        if (hoveredEntry == null) return;
        if (event.isShiftDown()) return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            event.consume();
        }
        else if (SwingUtilities.isRightMouseButton(event)) {
            event.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent event, Point translated) {
        CodeVisionEntry clickedEntry = hoveredEntry;
        if (clickedEntry == null) return;
        clickedEntry.putUserData(ClickableTextCodeVisionEntry.MOUSE_EVENT_KEY, event);

        if (event.isShiftDown()) return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            handleLeftClick(clickedEntry);
        }
        else if (SwingUtilities.isRightMouseButton(event)) {
            handleRightClick(clickedEntry);
        }
        event.consume();
    }

    private void handleRightClick(CodeVisionEntry clickedEntry) {
        CodeVisionListData data = inlay.getUserData(CodeVisionListData.KEY);
        if (data != null) {
            data.rangeCodeVisionModel.handleLensRightClick(clickedEntry, inlay);
        }
    }

    private void handleLeftClick(CodeVisionEntry clickedEntry) {
        CodeVisionListData data = inlay.getUserData(CodeVisionListData.KEY);
        if (data != null) {
            data.rangeCodeVisionModel.handleLensClick(clickedEntry, inlay);
        }
    }

    private void updateMouseState(boolean isHovered, @Nullable Point point) {
        this.isHovered = isHovered;
        // Only invoke setHoveredEntry when the entry actually changes — mirrors JB's reactive
        // Property<T> which only fires downstream effects on value change, preventing cursor/
        // tooltip flicker on every mouseMoved tick within the same entry.
        @Nullable CodeVisionEntry newEntry = isHovered ? getHoveredEntry(point) : null;
        if (newEntry != hoveredEntry) {
            setHoveredEntry(newEntry);
        }
        inlay.repaint();
    }

    private @Nullable CodeVisionEntry getHoveredEntry(@Nullable Point point) {
        CodeVisionListData codeVisionListData = inlay.getUserData(CodeVisionListData.KEY);
        RangeCodeVisionModel.InlayState state = codeVisionListData != null
            ? codeVisionListData.rangeCodeVisionModel.state()
            : RangeCodeVisionModel.InlayState.NORMAL;
        if (point == null) return null;
        return painter.hoveredEntry(inlay.getEditor(), state, codeVisionListData, point.x, point.y);
    }

    @Override
    public Point translatePoint(Point inlayPoint) {
        return getPoint(inlay, inlayPoint);
    }

    private void setHoveredEntry(@Nullable CodeVisionEntry entry) {
        // Cancel existing tooltip timer
        if (tooltipTimer != null) {
            tooltipTimer.stop();
            tooltipTimer = null;
        }
        // Hide existing tooltip
        hideTooltip();

        this.hoveredEntry = entry;

        // Update cursor
        updateCursor(entry != null);

        // If new entry: start debounce timer for tooltip (same 1000ms as JetBrains)
        if (entry != null) {
            final CodeVisionEntry finalEntry = entry;
            tooltipTimer = new Timer(1000, e -> {
                tooltipTimer = null;
                showTooltip(finalEntry);
            });
            tooltipTimer.setRepeats(false);
            tooltipTimer.start();
        }
    }

    private void updateCursor(boolean hasHoveredEntry) {
        if (inlay.getEditor() instanceof EditorEx editorEx) {
            // Use the editor's custom-cursor API so the cursor participates in the editor's
            // cursor-arbitration system and is not silently overridden by other listeners.
            // Passing null restores the editor's default cursor management (same pattern as
            // DeclarativeInlayHintsMouseMotionListener in Consulo).
            editorEx.setCustomCursor(CodeVisionInlayRendererBase.class,
                hasHoveredEntry ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
        }
    }

    protected Point getPoint(Inlay<?> inlay, Point targetPoint) {
        return targetPoint;
    }

    protected RangeCodeVisionModel.InlayState inlayState(Inlay<?> inlay) {
        CodeVisionListData data = inlay.getUserData(CodeVisionListData.KEY);
        return data != null ? data.rangeCodeVisionModel.state() : RangeCodeVisionModel.InlayState.NORMAL;
    }

    private void showTooltip(CodeVisionEntry entry) {
        String text = entry.tooltip;
        if (text == null || text.isEmpty()) return;

        Rectangle inlayBounds = inlay.getBounds();
        if (inlayBounds == null) return;
        Rectangle entryBounds = calculateCodeVisionEntryBounds(entry);
        if (entryBounds == null) return;

        int x = inlayBounds.x + entryBounds.x + (entryBounds.width / 2);
        int y = inlayBounds.y + (inlayBounds.height / 2);

        java.awt.Component contentComponent = inlay.getEditor().getContentComponent();
        java.awt.Component component = inlay.getEditor().getComponent();
        RelativePoint relativePoint = new RelativePoint(contentComponent, new Point(x, y));
        IdeTooltip tooltip = new IdeTooltip(component, relativePoint.getPoint(component), new JLabel(text));
        currentTooltip = IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, false, false);

        // Hide tooltip on scroll (equivalent to visibleAreaChanged().advise in JetBrains)
        tooltipAreaListener = e -> hideTooltip();
        inlay.getEditor().getScrollingModel().addVisibleAreaListener(tooltipAreaListener);
    }

    private void hideTooltip() {
        if (currentTooltip != null) {
            currentTooltip.hide();
            currentTooltip = null;
        }
        if (tooltipAreaListener != null && inlay != null && inlay.isValid()) {
            inlay.getEditor().getScrollingModel().removeVisibleAreaListener(tooltipAreaListener);
            tooltipAreaListener = null;
        }
        else {
            tooltipAreaListener = null;
        }
    }
}
