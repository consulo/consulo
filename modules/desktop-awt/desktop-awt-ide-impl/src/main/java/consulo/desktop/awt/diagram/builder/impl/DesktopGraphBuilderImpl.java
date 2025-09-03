/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.diagram.builder.impl;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import consulo.diagram.GraphBuilder;
import consulo.diagram.GraphNode;
import consulo.diagram.GraphPositionStrategy;
import consulo.diagram.internal.GraphNodeImpl;
import consulo.ui.Component;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22:38/15.10.13
 */
public class DesktopGraphBuilderImpl implements GraphBuilder {
  private List<GraphNode<?>> myGraphNodes = new ArrayList<GraphNode<?>>();

  public DesktopGraphBuilderImpl() {

  }

  @Override
  @Nonnull
  public Component getComponent() {
    mxGraph graph = new mxGraph();
    graph.setCellsResizable(false);
    graph.setEdgeLabelsMovable(false);
    graph.setConnectableEdges(false);
    graph.setCellsEditable(false);
    graph.setAllowDanglingEdges(false);

    mxGraphComponent mxGraphComponent = new mxGraphComponent(graph) {
      @Override
      public void addNotify() {
        super.addNotify();
        build(this);
      }
    };
    mxGraphComponent.setGridVisible(true);

    return TargetAWT.wrap(mxGraphComponent);
  }

  private void build(mxGraphComponent mxGraphComponent) {
    Container componentParent = mxGraphComponent.getParent();
    mxGraph graph = mxGraphComponent.getGraph();

    Object parent = graph.getDefaultParent();


    graph.getModel().beginUpdate();
    try {
      Map<GraphNode<?>, Object> map = new HashMap<>();

      int i = 0;
      for (GraphNode<?> graphNode : myGraphNodes) {
        mxCell vertex = graph.insertVertex(parent, graphNode.getName(), graphNode, 0, i * 100, 0, 0);

        map.put(graphNode, vertex);
        i++;
      }

      for (Map.Entry<GraphNode<?>, Object> entry : map.entrySet()) {
        List<GraphNode<?>> arrowNodes = entry.getKey().getArrowNodes();

        for (GraphNode<?> arrowNode : arrowNodes) {
          mxCell mxCell = graph.insertEdge(parent, null, null, entry.getValue(), map.get(arrowNode));
          mxCell.setStyle(mxConstants.STYLE_STROKECOLOR + "=" + "#FF0000");
        }
      }
    }
    finally {
      graph.getModel().endUpdate();
    }
  }

  @Nonnull
  @Override
  public <E> GraphNode<E> createNode(@Nonnull String name, @Nullable Image icon, @Nullable E value, GraphPositionStrategy strategy) {
    GraphNodeImpl<E> graphNode = new GraphNodeImpl<>(name, icon, value, strategy);
    myGraphNodes.add(graphNode);
    return graphNode;
  }
}
