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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

class PrintElementManagerImpl implements PrintElementManager {
  @Nonnull
  private final Comparator<GraphElement> myGraphElementComparator;
  @Nonnull
  private final ColorGetterByLayoutIndex myColorGetter;
  @Nonnull
  private final LinearGraph myLinearGraph;
  @Nonnull
  private Set<Integer> mySelectedNodeIds = Collections.emptySet();
  @Nullable
  private PrintElementWithGraphElement mySelectedPrintElement = null;

  @SuppressWarnings("unchecked")
  PrintElementManagerImpl(@Nonnull final LinearGraph linearGraph,
                          @Nonnull final PermanentGraphInfo myPermanentGraph,
                          @Nonnull GraphColorManager colorManager) {
    myLinearGraph = linearGraph;
    myColorGetter = new ColorGetterByLayoutIndex(linearGraph, myPermanentGraph, colorManager);
    myGraphElementComparator = new GraphElementComparatorByLayoutIndex(new Function<Integer, Integer>() {
      @Nonnull
      @Override
      public Integer apply(Integer nodeIndex) {
        int nodeId = linearGraph.getNodeId(nodeIndex);
        if (nodeId < 0) return nodeId;
        return myPermanentGraph.getPermanentGraphLayout().getLayoutIndex(nodeId);
      }
    });
  }

  @Override
  public boolean isSelected(@Nonnull PrintElementWithGraphElement printElement) {
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

  void setSelectedElement(@Nonnull PrintElementWithGraphElement printElement) {
    mySelectedNodeIds = Collections.emptySet();
    mySelectedPrintElement = printElement;
  }

  void setSelectedElements(@Nonnull Set<Integer> selectedNodeId) {
    mySelectedPrintElement = null;
    mySelectedNodeIds = selectedNodeId;
  }

  @Override
  public int getColorId(@Nonnull GraphElement element) {
    return myColorGetter.getColorId(element);
  }

  @Nonnull
  @Override
  public Comparator<GraphElement> getGraphElementComparator() {
    return myGraphElementComparator;
  }
}
