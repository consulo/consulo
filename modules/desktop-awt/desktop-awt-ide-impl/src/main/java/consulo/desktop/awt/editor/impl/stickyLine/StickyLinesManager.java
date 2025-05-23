// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.application.ui.event.UISettingsListener;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.codeEditor.internal.stickyLine.VisualStickyLine;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.util.ColorValueUtil;

import java.awt.*;

public class StickyLinesManager implements VisibleAreaListener, StickyLinesModel.Listener, Disposable {
    private final Editor editor;
    private final StickyLinesModel stickyModel;
    private final StickyLinesPanel stickyPanel;
    private final StickyLineShadowPainter shadowPainter;
    private final VisualStickyLines visualStickyLines;

    private Rectangle activeVisualArea = new Rectangle();
    private int activeVisualLine = -1;
    private int activeLineHeight = -1;
    private boolean activeIsEnabled = false;
    private int activeLineLimit = -1;

    public StickyLinesManager(Editor editor,
                              StickyLinesModel stickyModel,
                              StickyLinesPanel stickyPanel,
                              StickyLineShadowPainter shadowPainter,
                              VisualStickyLines visualStickyLines,
                              Disposable parentDisposable) {
        this.editor = editor;
        this.stickyModel = stickyModel;
        this.stickyPanel = stickyPanel;
        this.shadowPainter = shadowPainter;
        this.visualStickyLines = visualStickyLines;

        Disposer.register(parentDisposable, this);
        editor.getScrollingModel().addVisibleAreaListener(this, this);
        stickyModel.addListener(this);
        shadowPainter.setDarkColorScheme(isDarkColorScheme());

        editor.getProject().getMessageBus().connect(this).subscribe(
            UISettingsListener.class,
            settings -> {
                shadowPainter.setDarkColorScheme(isDarkColorScheme());
                recalculateAndRepaintLines();
            }
        );
    }

    public void repaintLines(int startVisualLine, int endVisualLine) {
        stickyPanel.repaintLines(startVisualLine, endVisualLine);
    }

    public int panelHeight() {
        return visualStickyLines.height();
    }

    public void startDumb() {
        stickyPanel.startDumb();
    }

    public boolean suppressHintForLine(int logicalLine) {
        for (VisualStickyLine line : visualStickyLines.lines(activeVisualArea)) {
            VisualPosition stickyVisualPos = new VisualPosition(line.primaryLine(), 0);
            int stickyLogicalLine = editor.visualToLogicalPosition(stickyVisualPos).line;
            if (logicalLine == stickyLogicalLine ||
                logicalLine == stickyLogicalLine - 1 ||
                logicalLine == stickyLogicalLine + 1) {
                return true;
            }
        }
        return false;
    }

    public void reinitSettings() {
        boolean oldIsEnabled = activeIsEnabled;
        boolean newIsEnabled = editor.getSettings().isStickyLineShown();
        int oldLineLimit = activeLineLimit;
        int newLineLimit = editor.getSettings().getStickyLinesLimit();
        activeIsEnabled = newIsEnabled;
        activeLineLimit = newLineLimit;

        if (newIsEnabled && !oldIsEnabled) {
            recalculateAndRepaintLines(true);
        }
        else if (!newIsEnabled && oldIsEnabled) {
            resetLines();
        }
        else if (newLineLimit != oldLineLimit) {
            recalculateAndRepaintLines();
        }
    }

    public void clearStickyModel() {
        stickyModel.removeAllStickyLines(editor.getProject());
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent event) {
        if (editor.getSettings().isStickyLineShown() && isAreaChanged(event)) {
            activeVisualArea = event.getNewRectangle();
            if (activeVisualArea.y < 3) {
                resetLines();
            }
            else if (event.getOldRectangle() == null || isLineChanged()) {
                recalculateAndRepaintLines();
            }
            else if (isYChanged(event) || isSizeChanged(event)) {
                repaintLines();
            }
        }
    }

    @Override
    public void linesUpdated() {
        recalculateAndRepaintLines();
    }

    @Override
    public void linesRemoved() {
        // no-op
    }

    @Override
    public void dispose() {
        stickyModel.removeListener(this);
    }

    private boolean isDarkColorScheme() {
        return ColorValueUtil.isDark(editor.getColorsScheme().getDefaultBackground());
    }

    private void recalculateAndRepaintLines() {
        recalculateAndRepaintLines(false);
    }

    private void recalculateAndRepaintLines(boolean force) {
        if (force) {
            activeVisualArea = editor.getScrollingModel().getVisibleArea();
            isLineChanged();
        }
        if (activeVisualLine != -1 && activeLineHeight != -1 && !isPoint(activeVisualArea)) {
            visualStickyLines.recalculate(activeVisualArea);
            repaintLines();
        }
    }

    private void resetLines() {
        activeVisualLine = -1;
        activeLineHeight = -1;
        visualStickyLines.clear();
        repaintLines();
    }

    private void repaintLines() {
        stickyPanel.repaintLines();
    }

    private boolean isAreaChanged(VisibleAreaEvent event) {
        Rectangle oldRect = event.getOldRectangle();
        Rectangle newRect = event.getNewRectangle();
        return oldRect == null ||
            oldRect.y != newRect.y ||
            oldRect.height != newRect.height ||
            oldRect.width != newRect.width;
    }

    private boolean isLineChanged() {
        int newVisualLine = editor.yToVisualLine(activeVisualArea.y);
        int newLineHeight = editor.getLineHeight();
        if (activeVisualLine != newVisualLine || activeLineHeight != newLineHeight) {
            activeVisualLine = newVisualLine;
            activeLineHeight = newLineHeight;
            return true;
        }
        return false;
    }

    private boolean isYChanged(VisibleAreaEvent event) {
        return event.getOldRectangle().y != event.getNewRectangle().y;
    }

    private boolean isSizeChanged(VisibleAreaEvent event) {
        Rectangle oldRect = event.getOldRectangle();
        Rectangle newRect = event.getNewRectangle();
        return oldRect.width != newRect.width || oldRect.height != newRect.height;
    }

    private boolean isPoint(Rectangle rectangle) {
        return rectangle.x == 0 &&
            rectangle.y == 0 &&
            rectangle.height == 0 &&
            rectangle.width == 0;
    }
}
