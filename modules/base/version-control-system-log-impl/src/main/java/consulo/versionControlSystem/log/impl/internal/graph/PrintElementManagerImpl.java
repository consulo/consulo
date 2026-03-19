/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.versionControlSystem.log.graph.*;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

class PrintElementManagerImpl implements PrintElementManager {
  
  private final Comparator<GraphElement> myGraphElementComparator;
  
  private final ColorGetterByLayoutIndex myColorGetter;
  
  private final LinearGraph myLinearGraph;
  
  private Set<Integer> mySelectedNodeIds = Collections.emptySet();
  private @Nullable PrintElementWithGraphElement mySelectedPrintElement = null;

  @SuppressWarnings("unchecked")
  PrintElementManagerImpl(final LinearGraph linearGraph,
                          final PermanentGraphInfo myPermanentGraph,
                          GraphColorManager colorManager) {
    myLinearGraph = linearGraph;
    myColorGetter = new ColorGetterByLayoutIndex(linearGraph, myPermanentGraph, colorManager);
    myGraphElementComparator = new GraphElementComparatorByLayoutIndex(new Function<Integer, Integer>() {
      
      @Override
      public Integer apply(Integer nodeIndex) {
        int nodeId = linearGraph.getNodeId(nodeIndex);
        if (nodeId < 0) return nodeId;
        return myPermanentGraph.getPermanentGraphLayout().getLayoutIndex(nodeId);
      }
    });
  }

  @Override
  public boolean isSelected(PrintElementWithGraphElement printElement) {
    if (printElement.equals(mySelectedPrintElement)) return true;

    GraphElement graphElement = printElement.getGraphElement();
    if (graphElement instanceof GraphNode) {
      int nodeId = myLinearGraph.getNodeId(((GraphNode)graphElement).getNodeIndex());
      return mySelectedNodeIds.contains(nodeId);
    }
    if (graphElement instanceof GraphEdge) {
      GraphEdge edge = (GraphEdge)graphElement;
      boolean selected = edge.getTargetId() == null || mySelectedNodeIds.contains(edge.getTargetId());
      selected &= edge.getUpNodeIndex() == null || mySelectedNodeIds.contains(myLinearGraph.getNodeId(edge.getUpNodeIndex()));
      selected &= edge.getDownNodeIndex() == null || mySelectedNodeIds.contains(myLinearGraph.getNodeId(edge.getDownNodeIndex()));
      return selected;
    }

    return false;
  }

  void setSelectedElement(PrintElementWithGraphElement printElement) {
    mySelectedNodeIds = Collections.emptySet();
    mySelectedPrintElement = printElement;
  }

  void setSelectedElements(Set<Integer> selectedNodeId) {
    mySelectedPrintElement = null;
    mySelectedNodeIds = selectedNodeId;
  }

  @Override
  public int getColorId(GraphElement element) {
    return myColorGetter.getColorId(element);
  }

  
  @Override
  public Comparator<GraphElement> getGraphElementComparator() {
    return myGraphElementComparator;
  }
}
