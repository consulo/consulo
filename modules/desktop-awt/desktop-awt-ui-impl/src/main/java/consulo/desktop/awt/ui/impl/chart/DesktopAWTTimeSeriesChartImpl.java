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

import consulo.desktop.awt.ui.impl.adtui.AxisComponent;
import consulo.desktop.awt.ui.impl.adtui.LegendComponent;
import consulo.desktop.awt.ui.impl.adtui.LegendConfig;
import consulo.desktop.awt.ui.impl.adtui.RangeSelectionComponent;
import consulo.desktop.awt.ui.impl.adtui.TabularLayout;
import consulo.desktop.awt.ui.impl.adtui.chart.linechart.LineChart;
import consulo.desktop.awt.ui.impl.adtui.chart.linechart.LineConfig;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.chart.TimeSeriesChartModel;
import consulo.ui.impl.chart.TimeSeriesImpl;
import consulo.ui.impl.chart.TimeSeriesListener;
import consulo.ui.impl.chart.model.RangeSelectionModel;
import consulo.ui.impl.chart.model.StreamingTimeline;
import consulo.ui.impl.chart.model.legend.LegendComponentModel;
import consulo.ui.impl.chart.model.legend.SeriesLegend;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopAWTTimeSeriesChartImpl extends SwingComponentDelegate<JPanel> implements TimeSeriesChart, TimeSeriesListener {
    private static final int Y_AXIS_TOP_MARGIN = 30;
    private static final int MARKER_LENGTH = 5;
    private static final int MIN_HEIGHT = 120;

    private final TimeSeriesChartModel myModel;
    private final LegendComponentModel myLegendModel;
    private final Map<TimeSeriesImpl, SeriesLegend> myLegends = new HashMap<>();

    private @Nullable LineChart myLineChart;
    private @Nullable LegendComponent myLegend;

    public DesktopAWTTimeSeriesChartImpl(TimeAxis axis, ChartUnit unit) {
        myModel = new TimeSeriesChartModel(axis, unit, this);
        myLegendModel = new LegendComponentModel(myModel.getTimeline().getDataRange());
    }

    @Override
    protected JPanel createComponent() {
        StreamingTimeline timeline = myModel.getTimeline();

        JPanel overlay = new JPanel(new TabularLayout("*", "*"));
        overlay.setOpaque(false);

        LegendComponent legend = new LegendComponent.Builder(myLegendModel).setRightPadding(JBUI.scale(5)).build();
        myLegend = legend;

        JPanel legendPanel = new JPanel(new BorderLayout());
        legendPanel.setOpaque(false);
        legendPanel.add(legend, BorderLayout.EAST);

        AxisComponent valueAxis = new AxisComponent(myModel.getValueAxisModel(), AxisComponent.AxisOrientation.RIGHT, true);
        valueAxis.setShowAxisLine(false);
        valueAxis.setShowMax(true);
        valueAxis.setOnlyShowUnitAtMax(false);
        valueAxis.setHideTickAtMin(true);
        valueAxis.setMarkerLengths(MARKER_LENGTH, MARKER_LENGTH);
        valueAxis.setMargins(0, Y_AXIS_TOP_MARGIN);

        LineChart lineChart = new LineChart(myModel.getLineModel());
        lineChart.setFillEndGap(true);
        myLineChart = lineChart;

        JPanel lineChartPanel = new JPanel(new BorderLayout());
        lineChartPanel.setOpaque(false);
        lineChartPanel.setBorder(BorderFactory.createEmptyBorder(Y_AXIS_TOP_MARGIN, 0, 0, 0));
        lineChartPanel.add(lineChart, BorderLayout.CENTER);

        RangeSelectionModel selectionModel = new RangeSelectionModel(timeline.getSelectionRange(), timeline.getViewRange());
        RangeSelectionComponent selection = new RangeSelectionComponent(selectionModel);
        selection.addMouseWheelListener(this::onMouseWheel);

        overlay.add(legendPanel, new TabularLayout.Constraint(0, 0));
        overlay.add(selection, new TabularLayout.Constraint(0, 0));
        overlay.add(valueAxis, new TabularLayout.Constraint(0, 0));
        overlay.add(lineChartPanel, new TabularLayout.Constraint(0, 0));

        AxisComponent timeAxis = new AxisComponent(myModel.getTimeAxisModel(), AxisComponent.AxisOrientation.BOTTOM, true);
        timeAxis.setShowAxisLine(false);
        timeAxis.setMinimumSize(new Dimension(0, JBUI.scale(20)));

        JPanel root = new DesktopAWTChartPanel(new TabularLayout("*", "*,Fit-"), () -> JBUI.scale(MIN_HEIGHT), height -> height);
        root.add(overlay, new TabularLayout.Constraint(0, 0));
        root.add(timeAxis, new TabularLayout.Constraint(1, 0));
        root.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (root.isShowing()) {
                    myModel.activate();
                }
                else {
                    myModel.deactivate();
                }
            }
        });

        for (TimeSeriesImpl series : myModel.getSeries()) {
            configure(series);
        }
        return root;
    }

    private void onMouseWheel(MouseWheelEvent event) {
        int width = event.getComponent().getWidth();
        double anchor = width == 0 ? 0.5 : (double) event.getX() / width;
        myModel.handleMouseWheel(event.getPreciseWheelRotation(), event.isControlDown() || event.isMetaDown(), anchor);
    }

    @Override
    public TimeAxis getAxis() {
        return myModel.getAxis();
    }

    @Override
    public ChartUnit getUnit() {
        return myModel.getUnit();
    }

    @RequiredUIAccess
    @Override
    public TimeSeries addSeries(LocalizeValue name, TimeSeriesKind kind) {
        TimeSeriesImpl series = myModel.addSeries(name, kind);
        SeriesLegend legend = new SeriesLegend(series.getRanged(), myModel.getFormatter(), myModel.getTimeline().getDataRange());
        myLegends.put(series, legend);
        myLegendModel.add(legend);
        configure(series);
        return series;
    }

    @Override
    public List<TimeSeries> getSeries() {
        return List.copyOf(myModel.getSeries());
    }

    private void configure(TimeSeriesImpl series) {
        LineConfig config = new LineConfig(TargetAWT.to(series.getColor())).setFilled(series.getKind() == TimeSeriesKind.AREA);
        if (myLineChart != null) {
            myLineChart.configure(series.getRanged(), config);
            myLineChart.repaint();
        }
        SeriesLegend legend = myLegends.get(series);
        if (myLegend != null && legend != null) {
            myLegend.configure(legend, new LegendConfig(config));
        }
    }

    @Override
    public void dataChanged(TimeSeriesImpl series) {
        if (myLineChart != null && !myModel.getAxis().isFollowingLatest()) {
            myLineChart.repaint();
        }
    }

    @Override
    public void colorChanged(TimeSeriesImpl series) {
        configure(series);
    }
}
