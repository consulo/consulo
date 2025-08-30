/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.versionControlSystem.log.graph.GraphEdge;
import consulo.versionControlSystem.log.graph.GraphElement;
import consulo.versionControlSystem.log.graph.GraphNode;
import consulo.versionControlSystem.log.graph.NormalEdge;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.function.Function;

import static consulo.versionControlSystem.log.graph.LinearGraphUtils.asNormalEdge;
import static consulo.versionControlSystem.log.graph.LinearGraphUtils.getNotNullNodeIndex;

public class GraphElementComparatorByLayoutIndex implements Comparator<GraphElement> {
  @Nonnull
  private final Function<Integer, Integer> myLayoutIndexGetter;

  public GraphElementComparatorByLayoutIndex(@Nonnull Function<Integer, Integer> layoutIndexGetter) {
    myLayoutIndexGetter = layoutIndexGetter;
  }

  @Override
  public int compare(@Nonnull GraphElement o1, @Nonnull GraphElement o2) {
    if (o1 instanceof GraphEdge && o2 instanceof GraphEdge) {
      GraphEdge edge1 = (GraphEdge)o1;
      GraphEdge edge2 = (GraphEdge)o2;
      NormalEdge normalEdge1 = asNormalEdge(edge1);
      NormalEdge normalEdge2 = asNormalEdge(edge2);
      if (normalEdge1 == null) return -compare2(edge2, new GraphNode(getNotNullNodeIndex(edge1)));
      if (normalEdge2 == null) return compare2(edge1, new GraphNode(getNotNullNodeIndex(edge2)));

      if (normalEdge1.up == normalEdge2.up) {
        if (getLayoutIndex(normalEdge1.down) != getLayoutIndex(normalEdge2.down)) {
          return getLayoutIndex(normalEdge1.down) - getLayoutIndex(normalEdge2.down);
        }
        else {
          return normalEdge1.down - normalEdge2.down;
        }
      }

      if (normalEdge1.up < normalEdge2.up) {
        return compare2(edge1, new GraphNode(normalEdge2.up));
      }
      else {
        return -compare2(edge2, new GraphNode(normalEdge1.up));
      }
    }

    if (o1 instanceof GraphEdge && o2 instanceof GraphNode) return compare2((GraphEdge)o1, (GraphNode)o2);

    if (o1 instanceof GraphNode && o2 instanceof GraphEdge) return -compare2((GraphEdge)o2, (GraphNode)o1);

    assert false; // both GraphNode
    return 0;
  }

  private int compare2(@Nonnull GraphEdge edge, @Nonnull GraphNode node) {
    NormalEdge normalEdge = asNormalEdge(edge);
    if (normalEdge == null) {
      return getLayoutIndex(getNotNullNodeIndex(edge)) - getLayoutIndex(node.getNodeIndex());
    }

    int upEdgeLI = getLayoutIndex(normalEdge.up);
    int downEdgeLI = getLayoutIndex(normalEdge.down);

    int nodeLI = getLayoutIndex(node.getNodeIndex());
    if (Math.max(upEdgeLI, downEdgeLI) != nodeLI) {
      return Math.max(upEdgeLI, downEdgeLI) - nodeLI;
    }
    else {
      return normalEdge.up - node.getNodeIndex();
    }
  }

  private int getLayoutIndex(int nodeIndex) {
    return myLayoutIndexGetter.apply(nodeIndex);
  }
}
