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

import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.impl.internal.util.Flags;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import jakarta.annotation.Nonnull;

class GraphNodesVisibility {
  @Nonnull
  private final LinearGraph myLinearGraph;
  @Nonnull
  private UnsignedBitSet myNodeVisibilityById;

  GraphNodesVisibility(@Nonnull LinearGraph linearGraph, @Nonnull UnsignedBitSet nodeVisibilityById) {
    myLinearGraph = linearGraph;
    myNodeVisibilityById = nodeVisibilityById;
  }

  @Nonnull
  UnsignedBitSet getNodeVisibilityById() {
    return myNodeVisibilityById;
  }

  void setNodeVisibilityById(@Nonnull UnsignedBitSet nodeVisibilityById) {
    myNodeVisibilityById = nodeVisibilityById;
  }

  boolean isVisible(int nodeIndex) {
    return myNodeVisibilityById.get(nodeId(nodeIndex));
  }

  void show(int nodeIndex) {
    myNodeVisibilityById.set(nodeId(nodeIndex), true);
  }

  void hide(int nodeIndex) {
    myNodeVisibilityById.set(nodeId(nodeIndex), false);
  }

  Flags asFlags() {
    return new Flags() {
      @Override
      public int size() {
        return myLinearGraph.nodesCount();
      }

      @Override
      public boolean get(int index) {
        return myNodeVisibilityById.get(nodeId(index));
      }

      @Override
      public void set(int index, boolean value) {
        myNodeVisibilityById.set(nodeId(index), value);
      }

      @Override
      public void setAll(boolean value) {
        for (int index = 0; index < size(); index++) set(index, value);
      }
    };
  }

  private int nodeId(int nodeIndex) {
    return myLinearGraph.getNodeId(nodeIndex);
  }
}
