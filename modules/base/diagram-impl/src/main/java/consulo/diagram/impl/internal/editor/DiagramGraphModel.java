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
package consulo.diagram.impl.internal.editor;

import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphModel;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public final class DiagramGraphModel implements GraphModel<DiagramGraphNode> {
    private volatile DiagramGraphSnapshot mySnapshot;

    public DiagramGraphModel(DiagramGraphSnapshot snapshot) {
        mySnapshot = snapshot;
    }

    public void setSnapshot(DiagramGraphSnapshot snapshot) {
        mySnapshot = snapshot;
    }

    @Override
    public Collection<DiagramGraphNode> getNodes() {
        return mySnapshot.getNodes();
    }

    @Override
    public Collection<DiagramGraphNode> getArrows(DiagramGraphNode node) {
        return mySnapshot.getArrows(node);
    }

    @Override
    public @Nullable GraphGroup getGroup(DiagramGraphNode node) {
        return mySnapshot.getGroup(node);
    }

    public @Nullable DiagramGraphEdgeStyle getEdgeStyle(DiagramGraphNode source, DiagramGraphNode target) {
        return mySnapshot.getEdgeStyle(source, target);
    }
}
