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

import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphDoubleClickEvent;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.FlameGraphOrientation;
import consulo.ui.chart.FlameGraphSelectEvent;
import consulo.ui.impl.chart.FlameGraphIndex;
import consulo.ui.impl.chart.FlameGraphNode;
import io.qt.core.QSize;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QFrame;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QScrollBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtFlameGraphImpl<E> extends QtComponentDelegate<QScrollArea> implements FlameGraph<E> {
    private static final int MIN_ROWS = 3;

    private final FlameGraphIndex<E> myIndex;

    private FlameGraphOrientation myOrientation = FlameGraphOrientation.FLAME;
    private @Nullable E myFocused;
    private @Nullable E mySelected;
    private @Nullable Predicate<E> myHighlight;
    private @Nullable DesktopQtFlameGraphWidget<E> myWidget;
    private int myDistanceFromBottom;

    public DesktopQtFlameGraphImpl(FlameGraphModel<E> model) {
        myIndex = new FlameGraphIndex<>(model);
    }

    @Override
    protected QScrollArea createQt(QWidget parent) {
        DesktopQtFlameGraphWidget<E> widget = new DesktopQtFlameGraphWidget<>(this);
        QScrollArea area = new QScrollArea(parent) {
            @Override
            public QSize minimumSizeHint() {
                return new QSize(super.minimumSizeHint().width(), MIN_ROWS * widget.rowHeight() + frameWidth() * 2);
            }
        };
        area.setWidgetResizable(true);
        area.setFrameShape(QFrame.Shape.NoFrame);
        area.setWidget(widget);

        QScrollBar bar = area.verticalScrollBar();
        bar.valueChanged.connect(value -> myDistanceFromBottom = bar.maximum() - value);
        bar.rangeChanged.connect((minimum, maximum) -> {
            if (myOrientation == FlameGraphOrientation.FLAME) {
                bar.setValue(maximum - myDistanceFromBottom);
            }
        });

        myWidget = widget;
        widget.contentChanged();
        return area;
    }

    void repaint() {
        DesktopQtFlameGraphWidget<E> widget = myWidget;
        if (widget != null && !widget.isDisposed()) {
            widget.contentChanged();
        }
    }

    FlameGraphNode<E> getRoot() {
        return myIndex.getRoot();
    }

    int getMaxDepth() {
        return myIndex.getMaxDepth();
    }

    @Nullable
    FlameGraphNode<E> getFocusedNode() {
        return myIndex.getNode(myFocused);
    }

    @Nullable
    E getSelected() {
        return mySelected;
    }

    boolean isHighlighted(E value) {
        return myHighlight == null || myHighlight.test(value);
    }

    void clicked(@Nullable FlameGraphNode<E> node, QWidget widget, QMouseEvent event, boolean doubleClick) {
        if (doubleClick) {
            if (node != null) {
                getListenerDispatcher(FlameGraphDoubleClickEvent.class)
                    .onEvent(new FlameGraphDoubleClickEvent<>(this, node.getValue(), DesktopQtInputDetails.mouse(widget, event)));
            }
            return;
        }
        mySelected = node == null ? null : node.getValue();
        repaint();
        getListenerDispatcher(FlameGraphSelectEvent.class)
            .onEvent(new FlameGraphSelectEvent<>(this, mySelected, DesktopQtInputDetails.mouse(widget, event)));
    }

    @Override
    public FlameGraphModel<E> getModel() {
        return myIndex.getModel();
    }

    @RequiredUIAccess
    @Override
    public void setOrientation(FlameGraphOrientation orientation) {
        myOrientation = orientation;
        myDistanceFromBottom = 0;
        QScrollArea area = myComponent;
        if (area != null && !area.isDisposed()) {
            QScrollBar bar = area.verticalScrollBar();
            bar.setValue(orientation == FlameGraphOrientation.FLAME ? bar.maximum() : bar.minimum());
        }
        repaint();
    }

    @Override
    public FlameGraphOrientation getOrientation() {
        return myOrientation;
    }

    @RequiredUIAccess
    @Override
    public void focus(@Nullable E node) {
        myFocused = myIndex.retain(node);
        repaint();
    }

    @Override
    public @Nullable E getFocused() {
        return myFocused;
    }

    @RequiredUIAccess
    @Override
    public void setHighlight(@Nullable Predicate<E> filter) {
        myHighlight = filter;
        repaint();
    }

    @Override
    public @Nullable E getSelectedValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        myIndex.rebuild();
        myFocused = myIndex.retain(myFocused);
        mySelected = myIndex.retain(mySelected);
        repaint();
    }
}
