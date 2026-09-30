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
package consulo.ui.graph;

import consulo.ui.Component;
import consulo.ui.internal.UIInternal;

/**
 * A directed graph over a {@link GraphModel}: every node is drawn as a box with the presentation of its
 * {@link GraphNodeRender}, and every arrow of the model as a line from its node to its target, styled by the
 * {@link GraphEdgeRender}. Where the nodes are placed is up to the graph - arrows point from one layer down to the
 * next wherever the model allows it.
 *
 * @author VISTALL
 * @since 2026-09-30
 */
public interface Graph<E> extends Component {
    static <E> Graph<E> create(GraphModel<E> model) {
        return UIInternal.get()._Components_graph(model);
    }

    void setNodeRender(GraphNodeRender<E> render);

    void setEdgeRender(GraphEdgeRender<E> render);

    /**
     * Reads the model again and places every node anew.
     */
    void refresh();
}
