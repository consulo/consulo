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
package consulo.versionControlSystem.log.graph;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface GraphChanges<NodeId> {

  
  Collection<Node<NodeId>> getChangedNodes();

  
  Collection<Edge<NodeId>> getChangedEdges();

  interface Node<NodeId> {
    
    NodeId getNodeId();

    boolean removed();
  }

  interface Edge<NodeId> {
    @Nullable NodeId upNodeId();

    @Nullable NodeId downNodeId();

    @Nullable NodeId targetId();

    boolean removed();
  }

  class NodeImpl<NodeId> implements Node<NodeId> {
    
    private final NodeId myNodeId;
    private final boolean myRemoved;

    public NodeImpl(NodeId nodeId, boolean removed) {
      myNodeId = nodeId;
      myRemoved = removed;
    }

    
    @Override
    public NodeId getNodeId() {
      return myNodeId;
    }

    @Override
    public boolean removed() {
      return myRemoved;
    }
  }

  class EdgeImpl<NodeId> implements Edge<NodeId> {
    private final @Nullable NodeId myUpNodeId;
    private final @Nullable NodeId myDownNodeId;
    private final @Nullable NodeId myTargetId;
    private final boolean myRemoved;

    public EdgeImpl(@Nullable NodeId upNodeId, @Nullable NodeId downNodeId, @Nullable NodeId targetId, boolean removed) {
      myUpNodeId = upNodeId;
      myDownNodeId = downNodeId;
      myTargetId = targetId;
      myRemoved = removed;
    }

    @Override
    public @Nullable NodeId upNodeId() {
      return myUpNodeId;
    }

    @Override
    public @Nullable NodeId downNodeId() {
      return myDownNodeId;
    }

    @Override
    public @Nullable NodeId targetId() {
      return myTargetId;
    }

    @Override
    public boolean removed() {
      return myRemoved;
    }
  }

  class GraphChangesImpl<NodeId> implements GraphChanges<NodeId> {
    private final Collection<Node<NodeId>> myChangedNodes;
    private final Collection<Edge<NodeId>> myChangedEdges;

    public GraphChangesImpl(Collection<Node<NodeId>> changedNodes, Collection<Edge<NodeId>> changedEdges) {
      myChangedNodes = changedNodes;
      myChangedEdges = changedEdges;
    }

    
    @Override
    public Collection<Node<NodeId>> getChangedNodes() {
      return myChangedNodes;
    }

    
    @Override
    public Collection<Edge<NodeId>> getChangedEdges() {
      return myChangedEdges;
    }
  }
}
