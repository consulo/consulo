// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.internal.stickyLine.VisualStickyLine;
import consulo.desktop.awt.editor.impl.DesktopEditorImpl;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBLayeredPane;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class StickyLinesPanel extends JBPanel<StickyLinesPanel> {
    private final EditorEx editor;
    private final StickyLineShadowPainter shadowPainter;
    private final VisualStickyLines visualStickyLines;

    private final JBLayeredPane layeredPane = new JBLayeredPane();
    private final StickyLineComponents stickyComponents;

    private int panelW = 0;
    private int panelH = 0;

    public StickyLinesPanel(EditorEx editor, StickyLineShadowPainter shadowPainter, VisualStickyLines visualStickyLines) {
        this.editor = editor;
        this.shadowPainter = shadowPainter;
        this.visualStickyLines = visualStickyLines;
        this.stickyComponents = new StickyLineComponents(editor, layeredPane);

        setBorder(bottomBorder());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        layeredPane.setLayout(null);
        add(layeredPane);
    }

    // ------------------------------------------- API -------------------------------------------

    public void repaintLines(int startVisualLine, int endVisualLine) {
        if (isPanelEnabled()) {
            for (StickyLineComponent lineComp : stickyComponents.components()) {
                lineComp.repaintIfInRange(startVisualLine, endVisualLine);
            }
        }
    }

    public void repaintLines() {
        if (isPanelEnabled()) {
            repaintLinesImpl();
        }
    }

    public void startDumb() {
        if (isPanelEnabled()) {
            for (StickyLineComponent lineComp : stickyComponents.components()) {
                lineComp.startDumb();
            }
        }
    }

    // ------------------------------------------- Impl -------------------------------------------

    private void repaintLinesImpl() {
        int panelWidth = stickyLinesPanelWidth();
        int lineHeight = editor.getLineHeight();
        int index = 0;
        Iterator<StickyLineComponent> components = stickyComponents.unboundComponents().iterator();
        List<VisualStickyLine> stickyLines = visualStickyLines.lines(editor.getScrollingModel().getVisibleArea());

        for (VisualStickyLine stickyLine : stickyLines) {
            StickyLineComponent component = components.next();
            component.setLine(
                stickyLine.primaryLine(),
                stickyLine.scopeLine(),
                stickyLine.navigateOffset(),
                stickyLine.debugText()
            );
            component.setBounds(0, stickyLine.yLocation(), panelWidth, lineHeight);
            component.setVisible(true);
            index++;
        }

        stickyComponents.resetAfterIndex(index);

        int panelHeight = visualStickyLines.height();
        if (isPanelSizeChanged(panelWidth, panelHeight)) {
            setSize(panelWidth, panelHeight == 0 ? 0 : panelHeight + 1);
            this.panelW = panelWidth;
            this.panelH = panelHeight;
            layeredPane.setSize(panelWidth, panelHeight);
            revalidate();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int panelHeight = panelH;
        int panelWidth = stickyLinesPanelWidth();
        int lineHeight = editor.getLineHeight();

        if (g instanceof Graphics2D && panelHeight > 0 && panelWidth > 0 && lineHeight > 0) {
            int borderHeight = ((LineBorder) getBorder()).getThickness();
            shadowPainter.paintShadow((Graphics2D) g, panelHeight + borderHeight, panelWidth, lineHeight);
        }
    }

    // ------------------------------------------- Utils -------------------------------------------

    private boolean isPanelSizeChanged(int panelWidth, int panelHeight) {
        return this.panelW != panelWidth || this.panelH != panelHeight;
    }

    private int stickyLinesPanelWidth() {
        return ((DesktopEditorImpl) editor).getStickyLinesPanelWidth();
    }

    private boolean isPanelEnabled() {
        boolean isEnabled = editor.getSettings().isStickyLineShown();
        if (!isEnabled && stickyComponents.clear()) {
            panelW = 0;
            panelH = 0;
            layeredPane.setSize(0, 0);
            setSize(0, 0);
            revalidate();
            repaint();
        }
        return isEnabled;
    }

    private SideBorder bottomBorder() {
        return new SideBorder(null, SideBorder.BOTTOM) {
            @Override
            public Color getLineColor() {
                ColorValue color = editor.getColorsScheme().getColor(EditorColors.STICKY_LINES_BORDER_COLOR);
                return TargetAWT.to(color != null ? color : editor.getColorsScheme().getDefaultBackground());
            }
        };
    }
}
