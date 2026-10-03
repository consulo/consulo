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
import consulo.desktop.awt.ui.impl.adtui.TabularLayout;
import consulo.desktop.awt.ui.impl.adtui.chart.statechart.StateChartColorProvider;
import consulo.desktop.awt.ui.impl.adtui.common.AdtUiUtils;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.TimeAxisImpl;
import consulo.ui.impl.chart.TimeAxisSubscription;
import consulo.ui.impl.chart.model.axis.ResizingAxisComponentModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopAWTStateChartImpl<S> extends SwingComponentDelegate<JPanel> implements StateChart<S> {
    private static final String NAME_COLUMN = "150px";
    private static final int ROW_HEIGHT = 20;
    private static final int MIN_ROWS = 2;
    private static final int PREFERRED_ROWS = 8;

    private final TimeAxisImpl myAxis;
    private final ResizingAxisComponentModel myTimeAxisModel;
    private final Map<S, StatePresentation> myPresentations = new HashMap<>();
    private final List<DesktopAWTStateRow<S>> myRows = new ArrayList<>();
    private final StateChartColorProvider<S> myColorProvider = new StateChartColorProvider<>() {
        @Override
        public Color getColor(boolean isMouseOver, S value) {
            StatePresentation presentation = myPresentations.get(value);
            Color color = presentation == null ? JBColor.GRAY : TargetAWT.to(presentation.color());
            return isMouseOver ? ColorUtil.brighter(color, 2) : color;
        }
    };

    private @Nullable JPanel myRowsPanel;
    private final TimeAxisSubscription mySubscription;

    public DesktopAWTStateChartImpl(TimeAxis axis) {
        myAxis = (TimeAxisImpl) axis;
        myTimeAxisModel = myAxis.createTimeAxisModel();
        mySubscription = new TimeAxisSubscription(myAxis, List.of());
    }

    @Override
    protected JPanel createComponent() {
        JPanel rowsPanel = new JPanel(new TabularLayout(NAME_COLUMN + ",*"));
        myRowsPanel = rowsPanel;
        rebuildRows();

        AxisComponent timeAxis = new AxisComponent(myTimeAxisModel, AxisComponent.AxisOrientation.BOTTOM, true);
        timeAxis.setShowAxisLine(false);
        timeAxis.setMinimumSize(new Dimension(0, JBUI.scale(20)));

        JBScrollPane scrollPane = new JBScrollPane(rowsPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        int scrollBarWidth = AdtUiUtils.unscale(scrollPane.getVerticalScrollBar().getPreferredSize().width);
        JPanel axisPanel = new JPanel(new TabularLayout(NAME_COLUMN + ",*," + scrollBarWidth + "px"));
        axisPanel.add(timeAxis, new TabularLayout.Constraint(0, 1));

        JPanel root = new DesktopAWTChartPanel(new TabularLayout("*", "*,Fit-"),
            () -> chartHeight(scrollPane, axisPanel, MIN_ROWS),
            height -> chartHeight(scrollPane, axisPanel, Math.clamp(myRows.size(), MIN_ROWS, PREFERRED_ROWS)));
        root.add(scrollPane, new TabularLayout.Constraint(0, 0));
        root.add(axisPanel, new TabularLayout.Constraint(1, 0));
        root.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (root.isShowing()) {
                    mySubscription.activate();
                }
                else {
                    mySubscription.deactivate();
                }
            }
        });
        return root;
    }

    private void rebuildRows() {
        JPanel rowsPanel = myRowsPanel;
        if (rowsPanel == null) {
            return;
        }
        rowsPanel.removeAll();
        TabularLayout layout = new TabularLayout(NAME_COLUMN + ",*");
        for (int i = 0; i < myRows.size(); i++) {
            layout.setRowSizing(i, ROW_HEIGHT + "px");
        }
        rowsPanel.setLayout(layout);
        for (int i = 0; i < myRows.size(); i++) {
            DesktopAWTStateRow<S> row = myRows.get(i);
            JBLabel label = new JBLabel(row.getName().get());
            label.setBorder(JBUI.Borders.empty(0, 4, 0, 8));
            rowsPanel.add(label, new TabularLayout.Constraint(i, 0));
            rowsPanel.add(row.getComponent(), new TabularLayout.Constraint(i, 1));
        }
        rowsPanel.revalidate();
        rowsPanel.repaint();
        if (isInitialized()) {
            toAWTComponent().revalidate();
        }
    }

    private static int chartHeight(JScrollPane scrollPane, JComponent axisPanel, int rows) {
        Insets insets = scrollPane.getInsets();
        return JBUI.scale(ROW_HEIGHT) * rows + insets.top + insets.bottom + axisPanel.getMinimumSize().height;
    }

    @Nullable
    StatePresentation getPresentation(S state) {
        return myPresentations.get(state);
    }

    StateChartColorProvider<S> getColorProvider() {
        return myColorProvider;
    }

    TimeAxisImpl getTimeAxis() {
        return myAxis;
    }

    @Override
    public TimeAxis getAxis() {
        return myAxis;
    }

    @RequiredUIAccess
    @Override
    public void setStatePresentation(S state, LocalizeValue label, ColorValue color) {
        myPresentations.put(state, new StatePresentation(label, color));
        for (DesktopAWTStateRow<S> row : myRows) {
            row.repaint();
        }
    }

    @RequiredUIAccess
    @Override
    public StateRow<S> addRow(LocalizeValue name) {
        DesktopAWTStateRow<S> row = new DesktopAWTStateRow<>(this, name);
        myRows.add(row);
        rebuildRows();
        return row;
    }

    @RequiredUIAccess
    @Override
    public void removeRow(StateRow<S> row) {
        if (myRows.remove(row)) {
            rebuildRows();
        }
    }

    @Override
    public List<StateRow<S>> getRows() {
        return List.copyOf(myRows);
    }
}
