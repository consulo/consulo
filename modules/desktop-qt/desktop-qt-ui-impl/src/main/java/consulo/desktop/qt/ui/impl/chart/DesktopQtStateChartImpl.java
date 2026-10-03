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
package consulo.desktop.qt.ui.impl.chart;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.StateRowImpl;
import consulo.ui.impl.chart.TimeAxisImpl;
import consulo.ui.impl.chart.TimeAxisSubscription;
import consulo.ui.impl.chart.model.axis.ResizingAxisComponentModel;
import consulo.ui.impl.chart.model.updater.Updatable;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtStateChartImpl<S> extends QtComponentDelegate<QWidget> implements StateChart<S> {
    static final int NAME_WIDTH = 150;

    private static final int MIN_ROWS = 2;
    private static final int PREFERRED_ROWS = 8;

    private final TimeAxisImpl myAxis;
    private final ResizingAxisComponentModel myTimeAxisModel;
    private final Map<S, StatePresentation> myPresentations = new HashMap<>();
    private final List<StateRowImpl<S>> myRows = new ArrayList<>();
    private final TimeAxisSubscription mySubscription;

    private @Nullable DesktopQtStateChartRowsWidget<S> myRowsWidget;
    private @Nullable DesktopQtStateChartAxisWidget myAxisWidget;
    private @Nullable QScrollArea myScrollArea;

    public DesktopQtStateChartImpl(TimeAxis axis) {
        myAxis = (TimeAxisImpl) axis;
        myTimeAxisModel = myAxis.createTimeAxisModel();
        Updatable repaint = elapsedNs -> repaint();
        mySubscription = new TimeAxisSubscription(myAxis, List.of(repaint));
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget root = new QWidget(parent);
        QVBoxLayout layout = new QVBoxLayout(root);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);

        DesktopQtStateChartRowsWidget<S> rows = new DesktopQtStateChartRowsWidget<>(this);
        QScrollArea scrollArea = new QScrollArea(root) {
            @Override
            public QSize sizeHint() {
                return withRows(super.sizeHint(), Math.clamp(myRows.size(), MIN_ROWS, PREFERRED_ROWS));
            }

            @Override
            public QSize minimumSizeHint() {
                return withRows(super.minimumSizeHint(), MIN_ROWS);
            }

            private QSize withRows(QSize size, int rows) {
                return new QSize(size.width(), rows * DesktopQtStateChartRowsWidget.ROW_HEIGHT + frameWidth() * 2);
            }
        };
        scrollArea.setWidgetResizable(true);
        scrollArea.setFrameShape(QFrame.Shape.NoFrame);
        scrollArea.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOn);
        scrollArea.setWidget(rows);
        layout.addWidget(scrollArea, 1);

        DesktopQtStateChartAxisWidget axisWidget = new DesktopQtStateChartAxisWidget(root, this, scrollArea);
        layout.addWidget(axisWidget);

        myRowsWidget = rows;
        myAxisWidget = axisWidget;
        myScrollArea = scrollArea;
        rows.rowsChanged();
        return root;
    }

    TimeAxisSubscription getSubscription() {
        return mySubscription;
    }

    void repaint() {
        DesktopQtStateChartRowsWidget<S> rows = myRowsWidget;
        if (rows != null && !rows.isDisposed()) {
            rows.update();
        }
        DesktopQtStateChartAxisWidget axisWidget = myAxisWidget;
        if (axisWidget != null && !axisWidget.isDisposed()) {
            axisWidget.update();
        }
    }

    TimeAxisImpl getAxisImpl() {
        return myAxis;
    }

    ResizingAxisComponentModel getTimeAxisModel() {
        return myTimeAxisModel;
    }

    List<StateRowImpl<S>> getRowList() {
        return myRows;
    }

    @Nullable
    StatePresentation getPresentation(S state) {
        return myPresentations.get(state);
    }

    @Override
    public TimeAxis getAxis() {
        return myAxis;
    }

    @RequiredUIAccess
    @Override
    public void setStatePresentation(S state, LocalizeValue label, ColorValue color) {
        myPresentations.put(state, new StatePresentation(label, color));
        repaint();
    }

    @RequiredUIAccess
    @Override
    public StateRow<S> addRow(LocalizeValue name) {
        StateRowImpl<S> row = new StateRowImpl<>(name, this::repaint);
        myRows.add(row);
        rowsChanged();
        return row;
    }

    @RequiredUIAccess
    @Override
    public void removeRow(StateRow<S> row) {
        if (myRows.remove(row)) {
            rowsChanged();
        }
    }

    private void rowsChanged() {
        DesktopQtStateChartRowsWidget<S> rows = myRowsWidget;
        if (rows != null && !rows.isDisposed()) {
            rows.rowsChanged();
        }
        QScrollArea scrollArea = myScrollArea;
        if (scrollArea != null && !scrollArea.isDisposed()) {
            scrollArea.updateGeometry();
        }
    }

    @Override
    public List<StateRow<S>> getRows() {
        return List.copyOf(myRows);
    }
}
