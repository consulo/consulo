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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.log.ColorGenerator;
import consulo.versionControlSystem.log.graph.EdgePrintElement;
import consulo.versionControlSystem.log.graph.NodePrintElement;
import consulo.versionControlSystem.log.graph.PrintElement;
import consulo.versionControlSystem.log.graph.paint.VcsGraphAnchor;
import consulo.versionControlSystem.log.graph.paint.VcsGraphLine;
import consulo.versionControlSystem.log.graph.paint.VcsGraphMouseListener;
import consulo.versionControlSystem.log.graph.paint.VcsGraphNode;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.impl.internal.graph.TerminalEdgePrintElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Turns the print elements of a row into {@link VcsGraphRow} shapes. An edge to a different lane of the neighbour row
 * stops at the shared border, half way between the two lanes, so both rows draw the same crossing point.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class VcsGraphRowBuilder {
    private static final int TERMINAL_OFFSET = VcsGraphRow.NODE_RADIUS / 2 + 1;
    private static final int MAX_GRAPH_WIDTH = 6;

    private final ColorGenerator myColorGenerator;

    VcsGraphRowBuilder(ColorGenerator colorGenerator) {
        myColorGenerator = colorGenerator;
    }

    VcsGraphRow build(
        Collection<? extends PrintElement> elements,
        int recommendedWidth,
        ColorValue backgroundColor,
        @Nullable VcsGraphMouseListener mouseListener
    ) {
        List<VcsGraphLine> lines = new ArrayList<>();
        List<VcsGraphNode> nodes = new ArrayList<>();
        double maxIndex = 0;

        for (PrintElement element : elements) {
            ColorValue color = myColorGenerator.getColor(element.getColorId());
            int lane = element.getPositionInCurrentRow();
            maxIndex = Math.max(maxIndex, lane);

            if (element instanceof NodePrintElement) {
                nodes.add(new VcsGraphNode(laneX(lane), color, element.isSelected()));
            }
            else if (element instanceof EdgePrintElement edge) {
                int otherLane = edge.getPositionInOtherRow();
                maxIndex = Math.max(maxIndex, (lane + otherLane) / 2.0);
                lines.add(line(edge, lane, otherLane, color));
            }
        }

        maxIndex = Math.max(maxIndex + 1, Math.min(MAX_GRAPH_WIDTH, recommendedWidth));
        int width = (int) Math.ceil(maxIndex * VcsGraphRow.LANE_WIDTH);
        return new VcsGraphRow(width, lines, nodes, backgroundColor, mouseListener);
    }

    /**
     * The element of {@code elements} drawn under the point of a row built from them: nodes first, then edges.
     */
    static @Nullable PrintElement findElement(
        Collection<? extends PrintElement> elements,
        VcsGraphRow row,
        float x,
        float y,
        int rowHeight
    ) {
        List<PrintElement> nodes = new ArrayList<>();
        List<PrintElement> edges = new ArrayList<>();
        for (PrintElement element : elements) {
            if (element instanceof NodePrintElement) {
                nodes.add(element);
            }
            else if (element instanceof EdgePrintElement) {
                edges.add(element);
            }
        }

        int node = row.findNodeAt(x, y, rowHeight);
        if (node >= 0 && node < nodes.size()) {
            return nodes.get(node);
        }
        int line = row.findLineAt(x, y, rowHeight);
        if (line >= 0 && line < edges.size()) {
            return edges.get(line);
        }
        return null;
    }

    private static VcsGraphLine line(EdgePrintElement edge, int lane, int otherLane, ColorValue color) {
        boolean terminal = edge instanceof TerminalEdgePrintElement;
        boolean dashed = edge.getLineStyle() == EdgePrintElement.LineStyle.DASHED;
        boolean arrow = edge.hasArrow();
        boolean selected = edge.isSelected();
        int x = laneX(lane);

        if (edge.getType() == EdgePrintElement.Type.DOWN) {
            if (lane == otherLane) {
                int endOffset = terminal ? -TERMINAL_OFFSET : 0;
                return new VcsGraphLine(x, VcsGraphAnchor.MIDDLE, 0, x, VcsGraphAnchor.BOTTOM, endOffset, color, dashed, arrow, selected);
            }
            int borderX = (x + laneX(otherLane)) / 2;
            return new VcsGraphLine(x, VcsGraphAnchor.MIDDLE, 0, borderX, VcsGraphAnchor.BOTTOM, 0, color, dashed, arrow, selected);
        }

        if (lane == otherLane) {
            int endOffset = terminal ? TERMINAL_OFFSET : 0;
            return new VcsGraphLine(x, VcsGraphAnchor.MIDDLE, -1, x, VcsGraphAnchor.TOP, endOffset, color, dashed, arrow, selected);
        }
        int borderX = (x + laneX(otherLane)) / 2;
        return new VcsGraphLine(x, VcsGraphAnchor.MIDDLE, 0, borderX, VcsGraphAnchor.TOP, 0, color, dashed, arrow, selected);
    }

    private static int laneX(int lane) {
        return VcsGraphRow.LANE_WIDTH * lane + VcsGraphRow.LANE_WIDTH / 2;
    }
}
