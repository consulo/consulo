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

import java.util.Collection;

/**
 * Nodes of a {@link Graph} and the arrows between them. The nodes are the domain values themselves, so a node
 * which appears twice in {@link #getNodes()} is one node.
 * <p>
 * The graph reads the model on the UI thread, so it must hand out values which are already computed.
 *
 * @author VISTALL
 * @since 2026-09-30
 */
public interface GraphModel<E> {
    Collection<E> getNodes();

    /**
     * Targets of the arrows which start at the node. A target which is not one of {@link #getNodes()} is skipped.
     */
    Collection<E> getArrows(E node);
}
