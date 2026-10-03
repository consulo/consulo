/*
 * Copyright (C) 2017 The Android Open Source Project
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
package consulo.desktop.awt.ui.impl.adtui.chart.statechart;

import consulo.desktop.awt.ui.impl.adtui.AnimatedComponent;
import consulo.desktop.awt.ui.impl.adtui.common.AdtUiUtils;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.impl.chart.model.RangedSeries;
import consulo.ui.impl.chart.model.SeriesData;
import consulo.ui.impl.chart.model.StateChartModel;
import consulo.ui.impl.chart.model.Stopwatch;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A chart component that renders series of state change events as rectangles.
 */
public class StateChart<T> extends AnimatedComponent {
    private static final int INVALID_INDEX = -1;
    private static final int TEXT_PADDING = 3;
    private static final int PREFERRED_ROW_HEIGHT = 27;

    private final StateChartModel<T> myModel;
    private final Renderer<T> myRender;
    private final StateChartConfig<T> myConfig;

    /**
     * The gap value as a percentage {0...1} of the height given to each data series
     */
    private float myHeightGap;

    private boolean myNeedsTransformToViewSpace = true;

    /**
     * For each series, cache a pair of:
     * - List of rectangles for the events in the current range, and
     * - List of values for the events in the current range The lists are parallel and should always have the same size. We maintain 2
     * lists just for compatibility with the code from other places.
     */
    private List<RectanglesAndValues<T>> myRectangleCache = List.of();
    private @Nullable Point myRowPoint;
    private int myHoveredSeriesIndex = INVALID_INDEX;

    private final List<Consumer<T>> myItemClickedListeners = new ArrayList<>();
    private final List<IntConsumer> myRowIndexChangeListeners = new ArrayList<>();

    private record RectanglesAndValues<T>(List<Rectangle2D.Float> rectangles, List<T> values) {
    }

    private record SeriesIndex(int seriesIndex, int itemIndex) {
    }

    public StateChart(StateChartModel<T> model, Renderer<T> render, StateChartConfig<T> config) {
        myModel = model;
        myRender = render;
        myConfig = config;
        myHeightGap = config.getHeightGap();

        setFont(AdtUiUtils.DEFAULT_FONT);

        model.addDependency(myAspectObserver).onChange(StateChartModel.Aspect.MODEL_CHANGED, this::modelChanged);
        modelChanged();
        registerMouseEvents();
        setPreferredSize(new Dimension(getPreferredSize().width, JBUI.scale(PREFERRED_ROW_HEIGHT) * model.getSeries().size()));
    }

    public StateChart(StateChartModel<T> model, Renderer<T> render) {
        this(model, render, defaultConfig());
    }

    public StateChart(StateChartModel<T> model, StateChartColorProvider<T> colorProvider, StateChartConfig<T> config) {
        this(model, fillRectRenderer(colorProvider), config);
    }

    public StateChart(StateChartModel<T> model, StateChartColorProvider<T> colorProvider) {
        this(model, colorProvider, defaultConfig());
    }

    public StateChart(StateChartModel<T> model,
                      StateChartColorProvider<T> colorProvider,
                      StateChartTextConverter<T> textConverter,
                      StateChartConfig<T> config) {
        this(model, fillRectAndTextRenderer(colorProvider, textConverter), config);
    }

    public StateChart(StateChartModel<T> model, StateChartColorProvider<T> colorProvider, StateChartTextConverter<T> textConverter) {
        this(model, colorProvider, textConverter, defaultConfig());
    }

    /**
     * @param colors map of a state to corresponding color
     */
    public StateChart(StateChartModel<T> model, Map<T, Color> colors) {
        this(model, new StateChartColorProvider<T>() {
            @Override
            public Color getColor(boolean isMouseOver, T value) {
                Color color = Objects.requireNonNull(colors.get(value));
                return isMouseOver ? ColorUtil.brighter(color, 2) : color;
            }
        });
    }

    public float getHeightGap() {
        return myHeightGap;
    }

    public void setHeightGap(float gap) {
        myHeightGap = Math.max(Math.min(gap, 1f), 0f);
    }

    private void modelChanged() {
        myNeedsTransformToViewSpace = true;
        opaqueRepaint();
    }

    private void setRowPoint(@Nullable Point point) {
        myRowPoint = point;
        Integer index = point == null ? null : seriesIndexAtPoint(point);
        setHoveredSeriesIndex(index == null ? INVALID_INDEX : index);
    }

    private void setHoveredSeriesIndex(int index) {
        if (myHoveredSeriesIndex != index) {
            myHoveredSeriesIndex = index;
            myRowIndexChangeListeners.forEach(it -> it.accept(index));
        }
    }

    private void transformToViewSpace() {
        if (!myNeedsTransformToViewSpace) {
            return;
        }
        myNeedsTransformToViewSpace = false;
        List<RangedSeries<T>> series = myModel.getSeries();
        int seriesSize = series.size();
        if (seriesSize == 0) {
            return;
        }

        // TODO support interpolation.
        float rectHeight = 1.0f / seriesSize;
        float gap = rectHeight * myHeightGap;
        float barHeight = rectHeight - gap;

        List<RectanglesAndValues<T>> cache = new ArrayList<>(seriesSize);
        for (int seriesIndex = 0; seriesIndex < seriesSize; seriesIndex++) {
            RangedSeries<T> data = series.get(seriesIndex);
            double min = data.getXRange().getMin();
            double max = data.getXRange().getMax();
            double invRange = 1.0 / (max - min);
            double startHeight = 1.0 - rectHeight * (seriesIndex + 1);
            double barY = startHeight + gap * 0.5f;
            List<SeriesData<T>> seriesDataList = data.getSeries();
            List<Rectangle2D.Float> rectangles = new ArrayList<>();
            List<T> rectangleValues = new ArrayList<>();

            if (!seriesDataList.isEmpty()) {
                // Construct rectangles.
                double previousX = seriesDataList.get(0).x;
                T previousValue = seriesDataList.get(0).value;
                for (SeriesData<T> seriesData : seriesDataList.subList(1, seriesDataList.size())) {
                    long x = seriesData.x;
                    T value = seriesData.value;
                    if (!Objects.equals(value, previousValue)) { // Ignore repeated values.
                        // Don't draw if this block doesn't intersect with [min..max]
                        if (x >= min) {
                            // Draw the previous block if previous value is non-null
                            if (previousValue != null) {
                                addRectangleDelta(rectangles, rectangleValues, previousValue, previousX, x, min, invRange, barY, barHeight);
                            }
                        }

                        // Start a new block.
                        previousValue = value;
                        previousX = x;
                        if (previousX >= max) {
                            break; // Drawn past max range, stop.
                        }
                    }
                }
                // The last data point continues till max
                if (previousX < max && previousValue != null) {
                    addRectangleDelta(
                        rectangles,
                        rectangleValues,
                        previousValue,
                        Math.max(min, previousX),
                        max,
                        min,
                        invRange,
                        barY,
                        barHeight
                    );
                }
            }

            cache.add(new RectanglesAndValues<>(rectangles, rectangleValues));
        }
        myRectangleCache = cache;
    }

    private static <T> void addRectangleDelta(List<Rectangle2D.Float> rectangles,
                                              List<T> rectangleValues,
                                              T value,
                                              double previousX,
                                              double currentX,
                                              double min,
                                              double invRange,
                                              double barY,
                                              float barHeight) {
        // Because we start our activity line from the bottom and grow up we offset the height
        // from the bottom of the component
        // instead of the top by subtracting our height from 1.
        rectangles.add(new Rectangle2D.Float(
            (float) ((previousX - min) * invRange),
            (float) barY,
            (float) ((currentX - previousX) * invRange),
            barHeight
        ));
        rectangleValues.add(value);
    }

    private static int searchByX(List<Rectangle2D.Float> rectangles, float x, float w) {
        int low = 0;
        int high = rectangles.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Rectangle2D.Float it = rectangles.get(mid);
            int cmp;
            if (it.x + it.width < x) {
                cmp = -1;
            }
            else if (it.x > x + w) {
                cmp = 1;
            }
            else {
                cmp = 0;
            }

            if (cmp < 0) {
                low = mid + 1;
            }
            else if (cmp > 0) {
                high = mid - 1;
            }
            else {
                return mid;
            }
        }
        return -(low + 1);
    }

    @Override
    protected void draw(Graphics2D g2d, Dimension dim) {
        Stopwatch stopwatch = new Stopwatch().start();
        long scalingTime = 0L;
        long reducerTime = 0L;
        int transformedShapesCount = 0;

        transformToViewSpace();
        long transformTime = stopwatch.getElapsedSinceLastDeltaNs();
        g2d.setFont(getFont());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        float scaleX = getWidth();
        float scaleY = getHeight();
        Rectangle clipRect = g2d.getClipBounds();

        for (int seriesIndex = 0; seriesIndex < myRectangleCache.size(); seriesIndex++) {
            RectanglesAndValues<T> cached = myRectangleCache.get(seriesIndex);
            List<Rectangle2D.Float> rectangles = cached.rectangles();
            List<T> rectangleValues = cached.values();

            int startIndexInclusive;
            if (clipRect != null && clipRect.x != 0) {
                int it = searchByX(rectangles, clipRect.x / scaleX, 0f);
                startIndexInclusive = it < 0 ? -(it + 1) : it;
            }
            else {
                startIndexInclusive = 0;
            }
            int endIndexExclusive;
            if (clipRect != null && clipRect.width != getWidth()) {
                int it = searchByX(rectangles, (clipRect.x + clipRect.width) / scaleX, 0f);
                endIndexExclusive = it < 0 ? -(it + 1) : (it + 1); // add 1 because exclusive
            }
            else {
                endIndexExclusive = rectangles.size();
            }
            List<T> transformedValues = new ArrayList<>(rectangleValues.subList(startIndexInclusive, endIndexExclusive));
            List<Rectangle2D.Float> transformedShapes = new ArrayList<>(endIndexExclusive - startIndexInclusive);
            for (Rectangle2D.Float it : rectangles.subList(startIndexInclusive, endIndexExclusive)) {
                // Manually scaling the rectangle results in ~6x performance improvement over calling
                // AffineTransform::createTransformedShape. The reason for this is the shape created is a
                // Point2D.Double.
                // This shape has to support all types of points as such cannot be transformed as
                // efficiently as a
                // rectangle. Furthermore, AffineTransform uses doubles, which is about half as fast for
                // LS
                // when compared to floats (doubles memory bandwidth).
                transformedShapes.add(new Rectangle2D.Float(it.x * scaleX, it.y * scaleY, it.width * scaleX, it.height * scaleY));
            }
            transformedShapesCount += transformedShapes.size();
            scalingTime += stopwatch.getElapsedSinceLastDeltaNs();
            myConfig.getReducer().reduce(transformedShapes, transformedValues);
            assert transformedShapes.size() == transformedValues.size();
            reducerTime += stopwatch.getElapsedSinceLastDeltaNs();
            int hoverIndex;
            if (seriesIndex != myHoveredSeriesIndex || myRowPoint == null) {
                hoverIndex = INVALID_INDEX;
            }
            else {
                hoverIndex = searchByX(transformedShapes, (float) myRowPoint.x, 1f);
            }

            for (int i = 0; i < transformedShapes.size(); i++) {
                Rectangle2D.Float rect = transformedShapes.get(i);
                // the rectangles are allowed to go outside this component, so we clip it
                if (0 <= rect.x && rect.x + rect.width <= getWidth()) {
                    g2d.setClip(rect);
                }
                else {
                    float x = Math.max(0f, rect.x);
                    float w = Math.min(scaleX - x, rect.width);
                    g2d.setClip(new Rectangle2D.Float(x, rect.y, w, rect.height));
                }
                myRender.render(g2d, rect, mDefaultFontMetrics, i == hoverIndex, transformedValues.get(i));
            }
        }

        long drawTime = stopwatch.getElapsedSinceLastDeltaNs();
        addDebugInfo("XS ms: %.2fms, %.2fms", transformTime / 1000000f, scalingTime / 1000000f);
        addDebugInfo(
            "RDT ms: %.2f, %.2f, %.2f",
            reducerTime / 1000000f,
            drawTime / 1000000f,
            (scalingTime + reducerTime + drawTime) / 1000000f
        );
        addDebugInfo("# of drawn rects: %d", transformedShapesCount);
    }

    public void addItemClickedListener(Consumer<T> onClicked) {
        myItemClickedListeners.add(onClicked);
    }

    public void addRowIndexChangeListener(IntConsumer onNewRowIndex) {
        myRowIndexChangeListeners.add(onNewRowIndex);
    }

    private void registerMouseEvents() {
        MouseAdapter handler = new MouseAdapter() {
            /**
             * In some cases, StateChart is delegated to by a parent containing component (e.g. a JList or a table). In order to preform
             * some painting optimizations, we need access to that source component.
             * <p>
             * TODO(b/116747281): It seems like we shouldn't have to know about this. Otherwise, almost every component would need
             * special-case logic like this. We should revisit how this class is being used by CpuCellRenderer.
             */
            private @Nullable Object myMouseEventSource;
            private @Nullable Point myMousePoint;
            private int myRowIndex = INVALID_INDEX;

            @Override
            public void mouseClicked(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handle(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handle(e);
            }

            private void handle(MouseEvent event) {
                if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                    T item = itemAtMouse(event.getPoint());
                    if (item != null) {
                        myItemClickedListeners.forEach(handler -> handler.accept(item));
                    }
                    return;
                }

                if (event.getPoint().equals(myMousePoint)) {
                    return;
                }
                Object src = event.getSource();
                if (myRowIndex != INVALID_INDEX) {
                    Point oldRowOriginInEventSpace;
                    // First convert the event mouse position into row index for the list.
                    if (src instanceof JList<?> list) {
                        oldRowOriginInEventSpace = list.getUI().indexToLocation(list, myRowIndex);
                    }
                    else {
                        oldRowOriginInEventSpace = new Point(0, 0);
                    }
                    if (oldRowOriginInEventSpace != null) {
                        renderUnion(myMouseEventSource, oldRowOriginInEventSpace);
                    }
                }
                if (event.getID() == MouseEvent.MOUSE_EXITED) {
                    myMousePoint = null;
                    setRowPoint(null);
                    myMouseEventSource = null;
                    myRowIndex = INVALID_INDEX;
                }
                else {
                    Point rowOrigin = new Point(0, 0);
                    Point eventPoint = event.getPoint();
                    myMousePoint = eventPoint;
                    myMouseEventSource = src;
                    if (src instanceof JList<?> list) {
                        // Since JList uses CellRenderers to render each list item, we actually need to
                        // translate the source location (in the JList's
                        // space) to the cell's coordinate space. We do this by simply getting the row index
                        // that the mouse location corresponds to,
                        // and then translate the index back to the List's coordinate space (which uses the
                        // origin of the row automatically). Then we
                        // subtract/translate the mouse point (which is still in the JLists's space) by the
                        // origin to get the mouse coordinate in the
                        // row's origin. This is akin to calculating the value after the decimal of a
                        // floating point number to its floor.
                        myRowIndex = list.getUI().locationToIndex(list, eventPoint);
                        if (myRowIndex >= 0) {
                            rowOrigin = Objects.requireNonNull(list.getUI().indexToLocation(list, myRowIndex));
                        }
                        Point rowPoint = new Point(eventPoint);
                        rowPoint.translate(-rowOrigin.x, -rowOrigin.y);
                        setRowPoint(rowPoint);
                    }
                    else {
                        // If the StateChart is not in a JList, then there is only one row. So we set the
                        // row to the first (and only) row to let the
                        // render happen.
                        myRowIndex = 0;
                        setRowPoint(eventPoint);
                    }
                    if (myRowIndex != INVALID_INDEX) {
                        renderUnion(myMouseEventSource, rowOrigin);
                    }
                }
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    public @Nullable T itemAtMouse(Point point) {
        SeriesIndex index = seriesIndexAtMouse(point);
        if (index == null) {
            return null;
        }
        List<SeriesData<T>> data = myModel.getSeries().get(index.seriesIndex()).getSeries();
        int i = index.itemIndex();
        return i >= 0 && i < data.size() ? data.get(i).value : null;
    }

    /**
     * Find the item index in the model that corresponds to the mouse position.
     *
     * @return - null if the mouse isn't on any series, or
     * - a pair of the series index, and the item index within the series. The item index corresponds to the right-most edge that's to the
     * mouse's left, or (-1) if the mouse is to the left of all items
     */
    private @Nullable SeriesIndex seriesIndexAtMouse(Point point) {
        List<RangedSeries<T>> series = myModel.getSeries();
        if (series.isEmpty()) {
            return null;
        }

        double scaleX = getWidth();
        Integer seriesIndex = seriesIndexAtPoint(point);
        if (seriesIndex == null) {
            return null;
        }
        RangedSeries<T> seriesAtMouse = series.get(seriesIndex);
        List<SeriesData<T>> seriesData = seriesAtMouse.getSeries();
        double min = seriesAtMouse.getXRange().getMin();
        double max = seriesAtMouse.getXRange().getMax();
        double range = max - min;

        // Convert mouseX into data/series coordinate space
        double modelMouseX = point.x / scaleX * range + min;
        if (seriesData.isEmpty()) {
            return null;
        }
        int i = binarySearch(seriesData, modelMouseX);
        if (i >= 0 && i < seriesData.size()) {
            return new SeriesIndex(seriesIndex, i); // mouse right on edge
        }
        return new SeriesIndex(seriesIndex, -i - 1 - 1); // mouse to the right of insertion index
    }

    private static <T> int binarySearch(List<SeriesData<T>> seriesData, double x) {
        int low = 0;
        int high = seriesData.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = Double.compare(seriesData.get(mid).x, x);
            if (cmp < 0) {
                low = mid + 1;
            }
            else if (cmp > 0) {
                high = mid - 1;
            }
            else {
                return mid;
            }
        }
        return -(low + 1);
    }

    private @Nullable Integer seriesIndexAtPoint(Point point) {
        float normalizedY = 1f - point.y / (float) getHeight();
        int n = myModel.getSeries().size();
        int i = (int) (normalizedY * n);
        int tolerancePixels = 2; // just in case of Swing off-by-one-pixel-mouse-handling issues
        if (i >= 0 && i < n) {
            return i;
        }
        else if (point.y >= getHeight() && point.y <= getHeight() + tolerancePixels) {
            return 0; // a hair too low
        }
        else if (point.y >= -tolerancePixels && point.y <= 0) {
            return n - 1; // a hair too high
        }
        return null;
    }

    private void renderUnion(@Nullable Object container, Point containerOffset) {
        if (myRowPoint != null && container instanceof Component component) {
            Rectangle2D.Float union = getMouseRectanglesUnion(myRowPoint);
            if (union != null) {
                // StateChart is commonly used as a cell renderer component, and therefore is not in the
                // proper Swing hierarchy.
                // Because of this, we need to use the source (which is probably a JList) to perform the
                // actual repaint.
                component.repaint(
                    (int) union.x + containerOffset.x,
                    (int) union.y + containerOffset.y,
                    (int) Math.ceil(union.width),
                    (int) Math.ceil(union.height)
                );
            }
        }
    }

    private Rectangle2D.@Nullable Float getMouseRectanglesUnion(Point mousePoint) {
        SeriesIndex index = seriesIndexAtMouse(mousePoint);
        if (index == null) {
            return null;
        }
        int seriesIndex = index.seriesIndex();
        int i = index.itemIndex();
        RangedSeries<T> series = myModel.getSeries().get(seriesIndex);
        List<SeriesData<T>> seriesDataList = series.getSeries();
        double min = series.getXRange().getMin();
        double max = series.getXRange().getMax();
        double range = max - min;
        int seriesSize = myModel.getSeries().size();
        double scaleX = getWidth();
        double scaleY = getHeight();

        // Transform the union of the left and right (or range max) index x values back into view
        // space.
        double modelXLeft = i >= 0 && i < seriesDataList.size() ? seriesDataList.get(i).x : min;
        double modelXRight = i + 1 >= 0 && i + 1 < seriesDataList.size() ? seriesDataList.get(i + 1).x : max;
        double screenXLeft = Math.floor((modelXLeft - min) * scaleX / range);
        double screenYTop = Math.floor(scaleY - (seriesIndex + 1) * scaleY / seriesSize);
        double screenXRight = Math.ceil((modelXRight - min) * scaleX / range);
        double screenYBottom = Math.ceil(scaleY - seriesIndex * scaleY / seriesSize);
        double screenWidth = screenXRight - screenXLeft;
        double screenHeight = screenYBottom - screenYTop;
        return new Rectangle2D.Float((float) screenXLeft, (float) screenYTop, (float) screenWidth, (float) screenHeight);
    }

    private static <T> StateChartConfig<T> defaultConfig() {
        return new StateChartConfig<>(new DefaultStateChartReducer<>());
    }

    public static <T> StateChartTextConverter<T> defaultTextConverter() {
        return String::valueOf;
    }

    private static <T> Renderer<T> fillRectRenderer(StateChartColorProvider<T> colorProvider) {
        return (g, rect, fontMetrics, hovered, value) -> {
            g.setColor(colorProvider.getColor(hovered, value));
            g.fill(rect);
        };
    }

    private static <T> Renderer<T> fillRectAndTextRenderer(
        StateChartColorProvider<T> colorProvider,
        StateChartTextConverter<T> textConverter
    ) {
        Renderer<T> fillRect = fillRectRenderer(colorProvider);
        return (g, rect, fontMetrics, hovered, value) -> {
            fillRect.render(g, rect, fontMetrics, hovered, value);
            String text = AdtUiUtils.shrinkToFit(textConverter.convertToString(value), fontMetrics, rect.width - TEXT_PADDING * 2);
            if (!text.isEmpty()) {
                g.setColor(colorProvider.getFontColor(hovered, value));
                float textOffset = rect.y + (rect.height - fontMetrics.getHeight()) * 0.5f + fontMetrics.getAscent();
                g.drawString(text, rect.x + TEXT_PADDING, textOffset);
            }
        };
    }
}
