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
package consulo.desktop.awt.ui.impl.graph;

import com.mxgraph.view.mxGraph;
import consulo.ui.impl.graph.GraphEdgeStyle;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
class DesktopAWTMxGraph extends mxGraph {
    private Function<Object, @Nullable String> myVertexTooltip = value -> null;

    DesktopAWTMxGraph() {
        setCellsResizable(false);
        setCellsEditable(false);
        setCellsDeletable(false);
        setCellsCloneable(false);
        setCellsDisconnectable(false);
        setCellsBendable(false);
        setConnectableEdges(false);
        setAllowDanglingEdges(false);
        setEdgeLabelsMovable(false);
        setDropEnabled(false);
        setSplitEnabled(false);
    }

    void setVertexTooltip(Function<Object, @Nullable String> vertexTooltip) {
        myVertexTooltip = vertexTooltip;
    }

    @Override
    public String convertValueToString(Object cell) {
        return getModel().getValue(cell) instanceof GraphEdgeStyle style ? style.getLabel().get() : "";
    }

    @Override
    public @Nullable String getToolTipForCell(Object cell) {
        Object value = getModel().getValue(cell);
        if (value instanceof GraphEdgeStyle style) {
            String tooltip = style.getTooltip().get();
            return tooltip.isEmpty() ? null : tooltip;
        }
        return value == null ? null : myVertexTooltip.apply(value);
    }
}
