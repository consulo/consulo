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
package consulo.versionControlSystem.log.graph.paint;

import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The graph of one commit row as data. Lines and dots are resolved against the real row height when the row is
 * drawn, so the rows join whatever height they are given. {@link VcsGraphRowImageFactory} turns it into an image a
 * cell can show.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class VcsGraphRow {
    public static final int LANE_WIDTH = 15;
    public static final int REFERENCE_ROW_HEIGHT = 22;
    public static final int NODE_RADIUS = 4;

    private static final float LINE_WIDTH = 1.5f;
    private static final float SELECTION_GAP = 1.5f;
    private static final float SELECTION_BORDER = 1f;
    private static final double ARROW_ANGLE_COS2 = 0.7;
    private static final double ARROW_LENGTH = 0.3;
    private static final float HIT_NODE_RADIUS = NODE_RADIUS + 1;
    private static final float HIT_LINE_DISTANCE = 2.5f;

    private final int myWidth;
    private final List<VcsGraphLine> myLines;
    private final List<VcsGraphNode> myNodes;
    private final ColorValue myBackgroundColor;
    private final @Nullable VcsGraphMouseListener myMouseListener;

    /**
     * @param backgroundColor the fill of the row, kept as a gap between a selected element and its border
     */
    public VcsGraphRow(
        int width,
        List<VcsGraphLine> lines,
        List<VcsGraphNode> nodes,
        ColorValue backgroundColor,
        @Nullable VcsGraphMouseListener mouseListener
    ) {
        myWidth = width;
        myLines = List.copyOf(lines);
        myNodes = List.copyOf(nodes);
        myBackgroundColor = backgroundColor;
        myMouseListener = mouseListener;
    }

    public ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    public @Nullable VcsGraphMouseListener getMouseListener() {
        return myMouseListener;
    }

    /**
     * The index in {@link #getNodes()} of the node under the point, or {@code -1}.
     */
    public int findNodeAt(float x, float y, int rowHeight) {
        float centerY = rowHeight / 2 + 0.5f;
        for (int i = 0; i < myNodes.size(); i++) {
            float dx = x - (myNodes.get(i).x() + 0.5f);
            float dy = y - centerY;
            if (dx * dx + dy * dy <= HIT_NODE_RADIUS * HIT_NODE_RADIUS) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The index in {@link #getLines()} of the line under the point, or {@code -1}.
     */
    public int findLineAt(float x, float y, int rowHeight) {
        for (int i = 0; i < myLines.size(); i++) {
            VcsGraphLine line = myLines.get(i);
            float x1 = line.startX();
            float y1 = resolveY(line.startAnchor(), line.startOffset(), rowHeight);
            float x2 = line.endX();
            float y2 = resolveY(line.endAnchor(), line.endOffset(), rowHeight);
            if (distanceToSegment(x, y, x1, y1, x2, y2) <= HIT_LINE_DISTANCE) {
                return i;
            }
        }
        return -1;
    }

    private static float distanceToSegment(float x, float y, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lengthSquared = dx * dx + dy * dy;
        float t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / lengthSquared));
        float px = x1 + t * dx - x;
        float py = y1 + t * dy - y;
        return (float) Math.sqrt(px * px + py * py);
    }

    public List<VcsGraphLine> getLines() {
        return myLines;
    }

    public List<VcsGraphNode> getNodes() {
        return myNodes;
    }

    public int getWidth() {
        return myWidth;
    }

    public List<VcsGraphSegment> resolveSegments(int rowHeight) {
        List<VcsGraphSegment> result = new ArrayList<>();
        for (VcsGraphLine line : myLines) {
            if (!line.selected()) {
                addLine(result, line, line.color(), LINE_WIDTH, rowHeight);
            }
        }
        for (VcsGraphLine line : myLines) {
            if (line.selected()) {
                addLine(result, line, line.color(), LINE_WIDTH + 2 * (SELECTION_GAP + SELECTION_BORDER), rowHeight);
            }
        }
        for (VcsGraphLine line : myLines) {
            if (line.selected()) {
                addLine(result, line, myBackgroundColor, LINE_WIDTH + 2 * SELECTION_GAP, rowHeight);
            }
        }
        for (VcsGraphLine line : myLines) {
            if (line.selected()) {
                addLine(result, line, line.color(), LINE_WIDTH, rowHeight);
            }
        }
        return result;
    }

    public List<VcsGraphDot> resolveDots(int rowHeight) {
        List<VcsGraphDot> result = new ArrayList<>();
        float y = rowHeight / 2 + 0.5f;
        for (VcsGraphNode node : myNodes) {
            if (!node.selected()) {
                result.add(new VcsGraphDot(node.x() + 0.5f, y, NODE_RADIUS, node.color()));
            }
        }
        for (VcsGraphNode node : myNodes) {
            if (node.selected()) {
                result.add(new VcsGraphDot(node.x() + 0.5f, y, NODE_RADIUS + SELECTION_GAP + SELECTION_BORDER, node.color()));
                result.add(new VcsGraphDot(node.x() + 0.5f, y, NODE_RADIUS + SELECTION_GAP, myBackgroundColor));
                result.add(new VcsGraphDot(node.x() + 0.5f, y, NODE_RADIUS, node.color()));
            }
        }
        return result;
    }

    public static int resolveY(VcsGraphAnchor anchor, int offset, int rowHeight) {
        return switch (anchor) {
            case TOP -> offset;
            case MIDDLE -> rowHeight / 2 + offset;
            case BOTTOM -> rowHeight + offset;
        };
    }

    private static void addLine(List<VcsGraphSegment> result, VcsGraphLine line, ColorValue color, float width, int rowHeight) {
        float x1 = line.startX();
        float y1 = resolveY(line.startAnchor(), line.startOffset(), rowHeight);
        float x2 = line.endX();
        float y2 = resolveY(line.endAnchor(), line.endOffset(), rowHeight);

        float[] dash = line.dashed() && !line.arrow() ? dashOf(fullLength(line, rowHeight), rowHeight) : null;
        result.add(new VcsGraphSegment(x1, y1, x2, y2, color, width, dash));

        if (line.arrow()) {
            double arrowLength = ARROW_LENGTH * rowHeight;
            double sin = Math.sqrt(1 - ARROW_ANGLE_COS2);
            double cos = Math.sqrt(ARROW_ANGLE_COS2);
            float[] first = rotate(x1, y1, x2, y2, cos, sin, arrowLength);
            float[] second = rotate(x1, y1, x2, y2, cos, -sin, arrowLength);
            result.add(new VcsGraphSegment(x2, y2, first[0], first[1], color, width, null));
            result.add(new VcsGraphSegment(x2, y2, second[0], second[1], color, width, null));
        }
    }

    private static int fullLength(VcsGraphLine line, int rowHeight) {
        if (line.startX() == line.endX()) {
            return rowHeight;
        }
        double dx = 2.0 * (line.endX() - line.startX());
        return (int) Math.ceil(Math.sqrt(dx * dx + (double) rowHeight * rowHeight));
    }

    private static float @Nullable [] dashOf(int edgeLength, int rowHeight) {
        int space = rowHeight / 4 - 1;
        int dash = rowHeight / 4 + 1;
        int count = edgeLength / (2 * (dash + space));
        if (count <= 0 || space <= 0) {
            return null;
        }
        int dashApprox = (edgeLength / 2 - count * space) / count;
        return new float[]{2 * dashApprox, 2 * space};
    }

    private static float[] rotate(double x, double y, double centerX, double centerY, double cos, double sin, double arrowLength) {
        double translateX = x - centerX;
        double translateY = y - centerY;

        double distance = Math.sqrt(translateX * translateX + translateY * translateY);
        if (distance == 0) {
            return new float[]{(float) centerX, (float) centerY};
        }
        double scaleX = arrowLength * translateX / distance;
        double scaleY = arrowLength * translateY / distance;

        double rotateX = scaleX * cos - scaleY * sin;
        double rotateY = scaleX * sin + scaleY * cos;

        return new float[]{(float) Math.round(rotateX + centerX), (float) Math.round(rotateY + centerY)};
    }
}
