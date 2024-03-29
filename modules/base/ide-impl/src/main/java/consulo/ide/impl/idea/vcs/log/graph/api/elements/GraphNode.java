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

package consulo.ide.impl.idea.vcs.log.graph.api.elements;

import jakarta.annotation.Nonnull;

public final class GraphNode implements GraphElement {
  private final int myNodeIndex;
  @Nonnull
  private final GraphNodeType myType;

  public GraphNode(int nodeIndex) {
    this(nodeIndex, GraphNodeType.USUAL);
  }

  public GraphNode(int nodeIndex, @Nonnull GraphNodeType type) {
    myNodeIndex = nodeIndex;
    myType = type;
  }

  public int getNodeIndex() {
    return myNodeIndex;
  }

  @Nonnull
  public GraphNodeType getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GraphNode graphNode = (GraphNode)o;

    if (myNodeIndex != graphNode.myNodeIndex) return false;
    if (myType != graphNode.myType) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myNodeIndex;
    result = 31 * result + myType.hashCode();
    return result;
  }
}
