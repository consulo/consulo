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
package consulo.desktop.awt.ui.impl.chart;

import consulo.desktop.awt.ui.impl.adtui.chart.hchart.HTreeChart;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphDoubleClickEvent;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.FlameGraphOrientation;
import consulo.ui.chart.FlameGraphSelectEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.impl.chart.ChartFormatters;
import consulo.ui.impl.chart.FlameGraphIndex;
import consulo.ui.impl.chart.FlameGraphNode;
import consulo.ui.impl.chart.model.Range;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopAWTFlameGraphImpl<E> extends SwingComponentDelegate<JPanel> implements FlameGraph<E> {
    private static final int NODE_HEIGHT = 20;
    private static final int MIN_ROWS = 3;

    private final FlameGraphModel<E> myModel;
    private final Range myXRange = new Range(0, 0);
    private final Range myGlobalXRange = new Range(0, 0);
    private final FlameGraphIndex<E> myIndex;
    private final DesktopAWTFlameGraphRenderer<E> myRenderer;

    private FlameGraphOrientation myOrientation = FlameGraphOrientation.FLAME;
    private @Nullable E myFocused;
    private @Nullable HTreeChart<FlameGraphNode<E>> myChart;

    public DesktopAWTFlameGraphImpl(FlameGraphModel<E> model) {
        myModel = model;
        myRenderer = new DesktopAWTFlameGraphRenderer<>(model);
        myIndex = new FlameGraphIndex<>(model);
        updateRanges();
    }

    @Override
    protected JPanel createComponent() {
        JPanel panel = new DesktopAWTChartPanel(new BorderLayout(),
            () -> MIN_ROWS * (JBUI.scale(NODE_HEIGHT) + HTreeChart.PADDING),
            height -> height);
        rebuildChart(panel);
        return panel;
    }

    private void rebuildTree() {
        myIndex.rebuild();
        updateRanges();
    }

    private void updateRanges() {
        FlameGraphNode<E> root = myIndex.getRoot();
        myGlobalXRange.set(root.getStart(), Math.max(root.getEnd(), root.getStart() + 1));
        FlameGraphNode<E> focused = myIndex.getNode(myFocused);
        if (focused == null) {
            myFocused = null;
            myXRange.set(myGlobalXRange.getMin(), myGlobalXRange.getMax());
        }
        else {
            myXRange.set(focused.getStart(), Math.max(focused.getEnd(), focused.getStart() + 1));
        }
    }

    private void rebuildChart(JPanel panel) {
        panel.removeAll();

        HTreeChart<FlameGraphNode<E>> chart = new HTreeChart.Builder<>(myIndex.getRoot(), myXRange, myRenderer)
            .setGlobalXRange(myGlobalXRange)
            .setOrientation(orientation(myOrientation))
            .setRootVisible(true)
            .setNodeSelectionEnabled(true)
            .setCustomNodeHeightPx(JBUI.scale(NODE_HEIGHT))
            .build();
        chart.setFont(UIUtil.getLabelFont());
        chart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                FlameGraphNode<E> node = chart.getNodeAt(e.getPoint());
                DesktopAWTFlameGraphImpl<E> graph = DesktopAWTFlameGraphImpl.this;
                if (e.getClickCount() == 2 && node != null) {
                    getListenerDispatcher(FlameGraphDoubleClickEvent.class)
                        .onEvent(new FlameGraphDoubleClickEvent<>(graph, node.getValue(), DesktopAWTInputDetails.convert(chart, e)));
                    return;
                }
                E value = node == null ? null : node.getValue();
                myRenderer.setSelected(value);
                chart.setSelectedNode(node);
                chart.repaint();
                getListenerDispatcher(FlameGraphSelectEvent.class)
                    .onEvent(new FlameGraphSelectEvent<>(graph, value, DesktopAWTInputDetails.convert(chart, e)));
            }
        });
        chart.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                FlameGraphNode<E> node = chart.getNodeAt(e.getPoint());
                chart.setToolTipText(node == null ? null : tooltip(node));
            }
        });
        myChart = chart;

        panel.add(chart, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    private String tooltip(FlameGraphNode<E> node) {
        long total = myIndex.getRoot().getDuration();
        long weight = node.getDuration();
        double scale = ChartFormatters.storageScale(myModel.getWeightUnit());
        String value = ChartFormatters.forUnit(myModel.getWeightUnit()).getFormattedString(total * scale, weight * scale, true);
        double percent = total == 0 ? 0 : weight * 100.0 / total;
        return String.format("%s%n%s (%.2f%%)", myModel.getName(node.getValue()), value, percent);
    }

    private static HTreeChart.Orientation orientation(FlameGraphOrientation orientation) {
        return orientation == FlameGraphOrientation.FLAME ? HTreeChart.Orientation.BOTTOM_UP : HTreeChart.Orientation.TOP_DOWN;
    }

    @Override
    public FlameGraphModel<E> getModel() {
        return myModel;
    }

    @RequiredUIAccess
    @Override
    public void setOrientation(FlameGraphOrientation orientation) {
        if (myOrientation == orientation) {
            return;
        }
        myOrientation = orientation;
        if (isInitialized()) {
            rebuildChart(toAWTComponent());
        }
    }

    @Override
    public FlameGraphOrientation getOrientation() {
        return myOrientation;
    }

    @RequiredUIAccess
    @Override
    public void focus(@Nullable E node) {
        FlameGraphNode<E> target = myIndex.getNode(node);
        myFocused = target == null ? null : node;
        if (target == null) {
            myXRange.set(myGlobalXRange.getMin(), myGlobalXRange.getMax());
        }
        else {
            myXRange.set(target.getStart(), Math.max(target.getEnd(), target.getStart() + 1));
        }
    }

    @Override
    public @Nullable E getFocused() {
        return myFocused;
    }

    @RequiredUIAccess
    @Override
    public void setHighlight(@Nullable Predicate<E> filter) {
        myRenderer.setHighlight(filter);
        if (myChart != null) {
            myChart.setHTree(myIndex.getRoot());
        }
    }

    @Override
    public @Nullable E getSelectedValue() {
        HTreeChart<FlameGraphNode<E>> chart = myChart;
        FlameGraphNode<E> selected = chart == null ? null : chart.getSelectedNode();
        return selected == null ? null : selected.getValue();
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        rebuildTree();
        if (isInitialized()) {
            rebuildChart(toAWTComponent());
        }
    }
}
