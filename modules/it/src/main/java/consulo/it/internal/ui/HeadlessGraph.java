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
package consulo.it.internal.ui;


import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.graph.Graph;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessGraph<E> extends HeadlessComponentBase implements Graph<E> {
    private final GraphModel<E> myModel;

    private @Nullable GraphNodeRender<E> myNodeRender;
    private @Nullable GraphEdgeRender<E> myEdgeRender;

    private List<E> myNodes = List.of();
    private List<E> mySelection = List.of();

    public HeadlessGraph(GraphModel<E> model) {
        myModel = model;
        rebuild();
    }

    @Override
    @RequiredUIAccess
    public void setNodeRender(GraphNodeRender<E> render) {
        myNodeRender = render;
        rebuild();
    }

    @Override
    @RequiredUIAccess
    public void setEdgeRender(GraphEdgeRender<E> render) {
        myEdgeRender = render;
        rebuild();
    }

    @Override
    public List<E> getSelectedValues() {
        return mySelection;
    }

    @Override
    @RequiredUIAccess
    public void refresh() {
        rebuild();
    }

    private void rebuild() {
        myNodes = List.copyOf(myModel.getNodes());
        mySelection = List.of();
    }

    @RequiredUIAccess
    public void setSelectedValues(Collection<? extends E> values) {
        List<E> selection = new ArrayList<>();
        for (E value : values) {
            if (myNodes.contains(value) && !selection.contains(value)) {
                selection.add(value);
            }
        }
        mySelection = List.copyOf(selection);
    }

    public GraphModel<E> getModel() {
        return myModel;
    }

    public List<E> getNodes() {
        return myNodes;
    }

    public @Nullable GraphNodeRender<E> getNodeRender() {
        return myNodeRender;
    }

    public @Nullable GraphEdgeRender<E> getEdgeRender() {
        return myEdgeRender;
    }
}
