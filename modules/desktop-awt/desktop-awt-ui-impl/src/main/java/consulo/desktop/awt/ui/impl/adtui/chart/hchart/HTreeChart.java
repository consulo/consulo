/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.adtui.chart.hchart;

import consulo.desktop.awt.ui.impl.adtui.AnimatedComponent;
import consulo.desktop.awt.ui.impl.adtui.common.AdtUiUtils;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.impl.chart.model.HNode;
import consulo.ui.impl.chart.model.Range;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.DoubleConsumer;

/**
 * A chart which renders nodes using a horizontal flow. That is, while normal trees are vertical, rendering nested rows top-to-bottom, this
 * chart renders nested columns left-to-right.
 *
 * @param <N> The type of the node used by this tree chart
 */
public class HTreeChart<N extends HNode<N>> extends AnimatedComponent {
    private static final String NO_HTREE = "No data available.";
    private static final String NO_RANGE = "X range width is zero: Please use a wider range.";
    private static final int ZOOM_FACTOR = 20;
    private static final String ACTION_ZOOM_IN = "zoom in";
    private static final String ACTION_ZOOM_OUT = "zoom out";
    private static final String ACTION_MOVE_LEFT = "move left";
    private static final String ACTION_MOVE_RIGHT = "move right";
    private static final int ACTION_MOVEMENT_FACTOR = 5;

    public static final int PADDING = 1;
    private static final int INITIAL_Y_POSITION = 0;

    public static final int HEIGHT_PADDING = 15;
    private static final int MOUSE_WHEEL_SCROLL_FACTOR = 8;

    private final Orientation myOrientation;
    private final HRenderer<N> myRenderer;
    private @Nullable N myRoot;
    private final Range myXRange;

    /**
     * The X range that myXRange could possibly be. Any changes to X range should be limited within it.
     */
    private final Range myGlobalXRange;
    private final Range myYRange = new Range(INITIAL_Y_POSITION, INITIAL_Y_POSITION);
    private final List<Rectangle2D.Float> myRectangles = new ArrayList<>();
    private final List<N> myNodes = new ArrayList<>();
    private final boolean myRootVisible;

    /**
     * Normally, the focused node is set by mouse hover. However, for tests, it can be a huge convenience to set this directly.
     * <p>
     * It is up to the caller to make sure that the node specified here actually belongs to this chart. Otherwise, the call will have no
     * effect.
     */
    private @Nullable N myFocusedNode;
    private final boolean myNodeSelectionEnabled;

    private @Nullable N mySelectedNode;

    private final List<Rectangle2D.Float> myDrawnRectangles = new ArrayList<>();
    private final List<N> myDrawnNodes = new ArrayList<>();
    private final HTreeChartReducer<N> myReducer;
    private @Nullable Image myCanvas;

    /**
     * If true, the next render pass will forcefully rebuild this chart's canvas (an expensive operation which doesn't have to be done too
     * often as usually the contents are static)
     */
    private boolean myDataUpdated = false;
    private boolean mySelectionUpdated = false;
    private int myMaximumHeight = 0;

    /**
     * Height of a tree node in pixels. If not set, we use the default font height.
     */
    private final int myCustomNodeHeightPx;

    /**
     * Vertical and horizontal padding in pixels between tree nodes.
     */
    private final int myNodeXPaddingPx;
    private final int myNodeYPaddingPx;

    private HTreeChart(Builder<N> builder) {
        myOrientation = builder.myOrientation;
        myRenderer = builder.myRenderer;
        myRoot = builder.myRoot;
        myXRange = builder.myXRange;
        myGlobalXRange = builder.myGlobalXRange;
        myRootVisible = builder.myRootVisible;
        myNodeSelectionEnabled = builder.myNodeSelectionEnabled;
        myReducer = builder.myReducer;
        myCustomNodeHeightPx = builder.myCustomNodeHeightPx;
        myNodeXPaddingPx = builder.myNodeXPaddingPx;
        myNodeYPaddingPx = builder.myNodeYPaddingPx;

        setFocusable(true);
        initializeInputMap();
        initializeMouseEvents();
        setFont(AdtUiUtils.DEFAULT_FONT);
        myXRange.addDependency(myAspectObserver).onChange(Range.Aspect.RANGE, this::rangeChanged);
        myYRange.addDependency(myAspectObserver).onChange(Range.Aspect.RANGE, this::rangeChanged);
        rootChanged();
    }

    public Orientation getOrientation() {
        return myOrientation;
    }

    public Range getYRange() {
        return myYRange;
    }

    public @Nullable N getFocusedNode() {
        return myFocusedNode;
    }

    public void setFocusedNode(@Nullable N focusedNode) {
        myFocusedNode = focusedNode;
    }

    public @Nullable N getSelectedNode() {
        return mySelectedNode;
    }

    /**
     * Updates the selected node. This is called by mouse click event handler and also from other instances of HTreeChart selects a node and
     * wants to update the (un)selected state of this instance.
     */
    public void setSelectedNode(@Nullable N node) {
        if (mySelectedNode != node) {
            mySelectionUpdated = true;
            mySelectedNode = node;
        }
    }

    public int getMaximumHeight() {
        return myMaximumHeight;
    }

    public boolean isNodeSelectionEnabled() {
        return myNodeSelectionEnabled;
    }

    private int getNodeHeight() {
        return myCustomNodeHeightPx > 0 ? myCustomNodeHeightPx : mDefaultFontMetrics.getHeight();
    }

    private void rangeChanged() {
        myDataUpdated = true;
        opaqueRepaint();
    }

    private void rootChanged() {
        myMaximumHeight = calculateMaximumHeight();
        // Update preferred size using calculated height to make sure containers of this chart account
        // for the height change during layout.
        setPreferredSize(new Dimension(getPreferredSize().width, myMaximumHeight));
        rangeChanged();
    }

    @Override
    protected void draw(Graphics2D g, Dimension dim) {
        long startTime = System.nanoTime();

        // If the selection changed, a call to updateNodesAndClearCanvas is unnecessary as it
        // reconstructs all nodes and rectangles.
        // All we need to do is null out the canvas to trigger another render pass with the preserved
        // node and rectangle data.
        if (mySelectionUpdated) {
            myCanvas = null;
            mySelectionUpdated = false;
        }

        if (myDataUpdated) {
            // Nulling out the canvas will trigger a render pass, below
            updateNodesAndClearCanvas();
            myDataUpdated = false;
        }
        g.setFont(getFont());
        if (myRoot == null || myRoot.getChildCount() == 0) {
            g.drawString(NO_HTREE, dim.width / 2 - mDefaultFontMetrics.stringWidth(NO_HTREE), dim.height / 2);
            return;
        }
        if (myXRange.getLength() == 0.0) {
            g.drawString(NO_RANGE, dim.width / 2 - mDefaultFontMetrics.stringWidth(NO_RANGE), dim.height / 2);
            return;
        }
        if (myCanvas == null || ImageUtil.getUserHeight(myCanvas) != dim.height || ImageUtil.getUserWidth(myCanvas) != dim.width) {
            redrawToCanvas(dim);
        }
        UIUtil.drawImage(g, myCanvas, 0, 0, null);
        addDebugInfo("Draw time %.2fms", (System.nanoTime() - startTime) / 1e6);
        addDebugInfo("# of nodes %d", myNodes.size());
        addDebugInfo("# of reduced nodes %d", myDrawnNodes.size());
    }

    private void redrawToCanvas(Dimension dim) {
        if (myCanvas == null || ImageUtil.getUserWidth(myCanvas) < dim.width || ImageUtil.getUserHeight(myCanvas) < dim.height) {
            // Note: We intentionally create an RGB image, not an ARGB image, because this allows nodes
            // to render their text clearly (ARGB prevents LCD rendering from working).
            myCanvas = ImageUtil.createImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g = (Graphics2D) myCanvas.getGraphics();
        g.setColor(getBackground());
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setComposite(AlphaComposite.Src);
        UISettingsUtil.setupAntialiasing(g);
        g.setFont(getFont());
        myDrawnNodes.clear();
        myDrawnNodes.addAll(myNodes);
        myDrawnRectangles.clear();
        // Transform
        for (Rectangle2D.Float rect : myRectangles) {
            Rectangle2D.Float newRect = new Rectangle2D.Float();
            newRect.x = rect.x * (float) dim.getWidth();
            newRect.y = rect.y;
            newRect.width = Math.max(0f, rect.width * (float) dim.getWidth() - myNodeXPaddingPx);
            newRect.height = rect.height;
            if (myOrientation == Orientation.BOTTOM_UP) {
                newRect.y = (float) (dim.getHeight() - newRect.y - newRect.getHeight());
            }
            myDrawnRectangles.add(newRect);
        }
        myReducer.reduce(myDrawnRectangles, myDrawnNodes);
        assert myDrawnRectangles.size() == myDrawnNodes.size();
        for (int i = 0; i < myDrawnNodes.size(); i++) {
            N node = myDrawnNodes.get(i);
            Rectangle2D.Float drawingArea = myDrawnRectangles.get(i);
            Rectangle2D.Float clampedDrawingArea = new Rectangle2D.Float(
                Math.max(0f, drawingArea.x),
                drawingArea.y,
                Math.min(drawingArea.x + drawingArea.width, (float) (dim.width - myNodeXPaddingPx)) - Math.max(0f, drawingArea.x),
                drawingArea.height
            );
            // In an effort to optimize performance of this chart's usage (b/281850040), hovering over a
            // node no longer triggers a redraw.
            // However, after this change, if something else (like a timeline range change) does trigger a
            // redraw, we do not want to show a
            // different fill color on the last hovered node. Thus, the isFocused parameter of render is
            // now statically set as false to prevent
            // all hover coloring. This achieves a consistent UI (if a mouse position change does not
            // update the node's fill color, no other
            // chart update should either).
            myRenderer.render(g, node, drawingArea, clampedDrawingArea, false, mySelectedNode != null && node != mySelectedNode);
        }
        g.dispose();
    }

    private void updateNodesAndClearCanvas() {
        myNodes.clear();
        myRectangles.clear();
        myCanvas = null;
        if (myRoot == null) {
            return;
        }
        if (inRange(myRoot)) {
            myNodes.add(myRoot);
            myRectangles.add(createRectangle(myRoot));
        }
        int head = 0;
        while (head < myNodes.size()) {
            N curNode = myNodes.get(head++);
            for (int i = 0; i < curNode.getChildCount(); i++) {
                N child = curNode.getChildAt(i);
                if (inRange(child)) {
                    myNodes.add(child);
                    myRectangles.add(createRectangle(child));
                }
            }
        }
        if (!myRootVisible && !myNodes.isEmpty()) {
            myNodes.remove(0);
            myRectangles.remove(0);
        }
    }

    private boolean inRange(N node) {
        return node.getStart() <= myXRange.getMax() && node.getEnd() >= myXRange.getMin();
    }

    private Rectangle2D.Float createRectangle(N node) {
        float left = (float) ((node.getStart() - myXRange.getMin()) / myXRange.getLength());
        float right = (float) ((node.getEnd() - myXRange.getMin()) / myXRange.getLength());
        Rectangle2D.Float rectangle = new Rectangle2D.Float();
        rectangle.x = left;
        rectangle.y = (float) ((getNodeHeight() + myNodeYPaddingPx) * node.getDepth() - myYRange.getMin());
        rectangle.width = right - left;
        rectangle.height = getNodeHeight();
        return rectangle;
    }

    private double positionToRange(double x) {
        return x / getWidth() * myXRange.getLength() + myXRange.getMin();
    }

    public void setHTree(@Nullable N root) {
        myRoot = root;
        rootChanged();
    }

    public @Nullable N getNodeAt(Point point) {
        int size = Math.min(myDrawnNodes.size(), myDrawnRectangles.size());
        for (int i = 0; i < size; i++) {
            if (myDrawnRectangles.get(i).contains(point)) {
                return myDrawnNodes.get(i);
            }
        }
        return null;
    }

    private void initializeInputMap() {
        bindKey(KeyEvent.VK_UP, ACTION_ZOOM_IN);
        bindKey(KeyEvent.VK_W, ACTION_ZOOM_IN);
        bindKey(KeyEvent.VK_DOWN, ACTION_ZOOM_OUT);
        bindKey(KeyEvent.VK_S, ACTION_ZOOM_OUT);
        bindKey(KeyEvent.VK_LEFT, ACTION_MOVE_LEFT);
        bindKey(KeyEvent.VK_A, ACTION_MOVE_LEFT);
        bindKey(KeyEvent.VK_RIGHT, ACTION_MOVE_RIGHT);
        bindKey(KeyEvent.VK_D, ACTION_MOVE_RIGHT);

        bindMovementAction(ACTION_ZOOM_IN, delta -> myXRange.set(myXRange.getMin() + delta, myXRange.getMax() - delta));
        bindMovementAction(ACTION_ZOOM_OUT, delta -> myXRange.set(
            Math.max(myGlobalXRange.getMin(), myXRange.getMin() - delta),
            Math.min(myGlobalXRange.getMax(), myXRange.getMax() + delta)
        ));
        bindMovementAction(ACTION_MOVE_LEFT, delta -> myXRange.shift(-Math.min(myXRange.getMin() - myGlobalXRange.getMin(), delta)));
        bindMovementAction(ACTION_MOVE_RIGHT, delta -> myXRange.shift(Math.min(myGlobalXRange.getMax() - myXRange.getMax(), delta)));
    }

    private void bindKey(int key, String action) {
        getInputMap().put(KeyStroke.getKeyStroke(key, 0), action);
    }

    private void bindMovementAction(String action, DoubleConsumer perform) {
        getActionMap().put(action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                perform.accept(myXRange.getLength() / ACTION_MOVEMENT_FACTOR);
            }
        });
    }

    private void initializeMouseEvents() {
        MouseAdapter adapter = new MouseAdapter() {
            private @Nullable Point myLastPoint;

            @Override
            public void mouseMoved(MouseEvent e) {
                N node = getNodeAt(e.getPoint());
                if (node != myFocusedNode) {
                    myFocusedNode = node;
                    eventSourceRepaint(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!hasFocus()) {
                    requestFocusInWindow();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                myLastPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point lastPoint = myLastPoint == null ? e.getPoint() : myLastPoint;
                // First, handle Y range.
                double deltaY = e.getPoint().y - lastPoint.y;
                shiftYRange(myOrientation == Orientation.BOTTOM_UP ? deltaY : -deltaY);

                // Second, handle X Range.
                double deltaX = e.getPoint().x - lastPoint.x;
                double deltaXToShift = myXRange.getLength() / getWidth() * -deltaX;
                if (deltaXToShift > 0) {
                    // User attempts to move the chart towards left to view the area to the right.
                    deltaXToShift = Math.min(myGlobalXRange.getMax() - myXRange.getMax(), deltaXToShift);
                }
                else if (deltaXToShift < 0) {
                    // User attempts to move the chart towards right to view the area to the left.
                    deltaXToShift = Math.max(myGlobalXRange.getMin() - myXRange.getMin(), deltaXToShift);
                }
                myXRange.shift(deltaXToShift);
                myLastPoint = e.getPoint();
            }

            private void shiftYRange(double delta) {
                double deltaY = delta;
                // The height of the contents we can show, including those not currently shown because of
                // vertical scrollbar's position.
                int contentHeight = myMaximumHeight;
                // The height of the GUI component to draw the contents.
                int viewHeight = getHeight();
                if (myYRange.getMin() + deltaY < INITIAL_Y_POSITION) {
                    // User attempts to drag the chart's head (the outermost frame on call stacks) away from
                    // the boundary. No.
                    deltaY = INITIAL_Y_POSITION - myYRange.getMin();
                }
                else if (myYRange.getMin() + viewHeight + deltaY > contentHeight) {
                    // User attempts to drag the chart's toe (the innermost frame on call stacks) away from
                    // the boundary. No.
                    // Note that the chart may be taller than the stacks, so we need to limit the delta.
                    deltaY = Math.max(0.0, contentHeight - viewHeight - myYRange.getMin());
                }
                myYRange.shift(deltaY);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (AdtUiUtils.isActionKeyDown(e)) {
                    double cursorRange = positionToRange(e.getX());
                    double leftDelta = (cursorRange - myXRange.getMin()) / ZOOM_FACTOR * e.getWheelRotation();
                    double rightDelta = (myXRange.getMax() - cursorRange) / ZOOM_FACTOR * e.getWheelRotation();
                    myXRange.set(Math.max(myGlobalXRange.getMin(), myXRange.getMin() - leftDelta),
                        Math.min(myGlobalXRange.getMax(), myXRange.getMax() + rightDelta));
                }
                else {
                    double deltaY = e.getPreciseWheelRotation() * MOUSE_WHEEL_SCROLL_FACTOR;
                    shiftYRange(myOrientation == Orientation.TOP_DOWN ? deltaY : -deltaY);
                }
            }
        };
        addMouseWheelListener(adapter);
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private int calculateMaximumHeight() {
        if (myRoot == null) {
            return 0;
        }
        int maxDepth = -1;
        Queue<N> queue = new LinkedList<>();
        queue.add(myRoot);
        while (!queue.isEmpty()) {
            N n = queue.poll();
            if (n.getDepth() > maxDepth) {
                maxDepth = n.getDepth();
            }
            for (int i = 0; i < n.getChildCount(); i++) {
                queue.add(n.getChildAt(i));
            }
        }
        maxDepth += 1;
        // The HEIGHT_PADDING is for the chart's toe (the innermost frame on call stacks).
        // We have this because the padding near the chart's head (the outermost frame on call stacks)
        // is there because the root node of the tree is invisible.
        return (getNodeHeight() + myNodeYPaddingPx) * maxDepth + HEIGHT_PADDING;
    }

    public static class Builder<N extends HNode<N>> {
        private final @Nullable N myRoot;
        // the range of the chart's visible area
        private final Range myXRange;
        // a HRenderer which is responsible for rendering a single node.
        private final HRenderer<N> myRenderer;
        private Orientation myOrientation = Orientation.TOP_DOWN;
        private Range myGlobalXRange = new Range(-Double.MAX_VALUE, Double.MAX_VALUE);
        private boolean myRootVisible = true;
        private boolean myNodeSelectionEnabled = false;
        private HTreeChartReducer<N> myReducer = new DefaultHTreeChartReducer<>();
        private int myCustomNodeHeightPx = 0;
        private int myNodeXPaddingPx = PADDING;
        private int myNodeYPaddingPx = PADDING;

        public Builder(@Nullable N root, Range xRange, HRenderer<N> renderer) {
            myRoot = root;
            myXRange = xRange;
            myRenderer = renderer;
        }

        public Builder<N> setOrientation(Orientation orientation) {
            myOrientation = orientation;
            return this;
        }

        public Builder<N> setRootVisible(boolean visible) {
            myRootVisible = visible;
            return this;
        }

        public Builder<N> setNodeSelectionEnabled(boolean nodeSelectionEnabled) {
            myNodeSelectionEnabled = nodeSelectionEnabled;
            return this;
        }

        /**
         * @param globalXRange the bounding range of chart's visible area, if it's not set, it assumes that there is no bounding range of
         *                     chart.
         */
        public Builder<N> setGlobalXRange(Range globalXRange) {
            myGlobalXRange = globalXRange;
            return this;
        }

        public Builder<N> setReducer(HTreeChartReducer<N> reducer) {
            myReducer = reducer;
            return this;
        }

        public Builder<N> setCustomNodeHeightPx(int customNodeHeightPx) {
            myCustomNodeHeightPx = customNodeHeightPx;
            return this;
        }

        public Builder<N> setNodeXPaddingPx(int nodeXPaddingPx) {
            myNodeXPaddingPx = nodeXPaddingPx;
            return this;
        }

        public Builder<N> setNodeYPaddingPx(int nodeYPaddingPx) {
            myNodeYPaddingPx = nodeYPaddingPx;
            return this;
        }

        public HTreeChart<N> build() {
            return new HTreeChart<>(this);
        }
    }

    public enum Orientation {
        TOP_DOWN,
        BOTTOM_UP,
    }
}
