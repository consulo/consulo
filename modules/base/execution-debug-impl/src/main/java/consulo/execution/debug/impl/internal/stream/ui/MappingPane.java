// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.stream.ui.LinkedValuesMapping;
import consulo.execution.debug.stream.ui.TraceController;
import consulo.execution.debug.stream.ui.ValueWithPosition;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class MappingPane extends JPanel {
    private static final LineColor DARCULA_LINE_COLOR = new LineColor(
        JBColor.GRAY,
        JBColor.BLUE,
        MorphColor.of(() -> new Color(92, 92, 92))
    );

    private static final LineColor INTELLIJ_LINE_COLOR = new LineColor(
        MorphColor.of(() -> new Color(168, 168, 168)),
        MorphColor.of(() -> new Color(0, 96, 229)),
        MorphColor.of(() -> new Color(204, 204, 204))
    );

    private static final BasicStroke STROKE = new BasicStroke(JBUIScale.scale(1.0f));

    private final List<? extends ValueWithPosition> beforeValues;
    private final LinkedValuesMapping mapping;
    private final TraceController controller;

    public MappingPane(@Nls @Nonnull String name,
                       @Nls @Nonnull String fullCallExpression,
                       @Nonnull List<? extends ValueWithPosition> beforeValues,
                       @Nonnull LinkedValuesMapping mapping,
                       @Nonnull TraceController controller) {
        super(new BorderLayout());
        this.beforeValues = beforeValues;
        this.mapping = mapping;
        this.controller = controller;

        JBLabel label = new JBLabel(name, SwingConstants.CENTER);
        label.setToolTipText(fullCallExpression);
        label.setBorder(JBUI.Borders.empty(2, 0, 3, 0));
        add(label, BorderLayout.NORTH);
        add(new MyDrawPane(), BorderLayout.CENTER);
    }

    private class MyDrawPane extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            if (g == null) {
                return;
            }

            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(STROKE);

                GraphicsConfig config = GraphicsUtil.setupAAPainting(g2d);

                LineColor colors = UIUtil.isUnderDarcula() ? DARCULA_LINE_COLOR : INTELLIJ_LINE_COLOR;
                if (isSelectedExist()) {
                    drawLines(g2d, colors.inactive, false);
                    drawLines(g2d, colors.selected, true);
                }
                else {
                    drawLines(g2d, colors.regular, false);
                }

                config.restore();
            }
        }

        private boolean isSelectedExist() {
            return controller.isSelectionExists();
        }

        private void drawLines(Graphics2D g, Color color, boolean highlighted) {
            int x1 = getX();
            int x2 = getX() + getWidth();
            g.setColor(color);
            for (ValueWithPosition value : beforeValues) {
                List<ValueWithPosition> linkedValues = mapping.getLinkedValues(value);
                if (linkedValues == null) {
                    continue;
                }

                for (ValueWithPosition nextValue : linkedValues) {
                    if (needToDraw(value, nextValue) && highlighted == needToHighlight(value, nextValue)) {
                        int y1 = value.getPosition();
                        int y2 = nextValue.getPosition();

                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            }
        }

        private boolean needToDraw(ValueWithPosition left, ValueWithPosition right) {
            return (left.isVisible() || right.isVisible()) && left.isValid() && right.isValid();
        }

        private boolean needToHighlight(ValueWithPosition left, ValueWithPosition right) {
            return left.isHighlighted() && right.isHighlighted();
        }
    }

    private static class LineColor {
        final Color regular;
        final Color selected;
        final Color inactive;

        LineColor(Color regular, Color selected, Color inactive) {
            this.regular = regular;
            this.selected = selected;
            this.inactive = inactive;
        }
    }
}
