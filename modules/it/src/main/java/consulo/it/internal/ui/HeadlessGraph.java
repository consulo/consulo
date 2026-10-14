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
import consulo.ui.graph.GraphEdgeRenderer;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRenderer;
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

    private @Nullable GraphNodeRenderer<E> myNodeRenderer;
    private @Nullable GraphEdgeRenderer<E> myEdgeRenderer;

    private List<E> myNodes = List.of();
    private List<E> mySelection = List.of();

    public HeadlessGraph(GraphModel<E> model) {
        myModel = model;
        rebuild();
    }

    @Override
    @RequiredUIAccess
    public void setNodeRenderer(GraphNodeRenderer<E> renderer) {
        myNodeRenderer = renderer;
        rebuild();
    }

    @Override
    @RequiredUIAccess
    public void setEdgeRenderer(GraphEdgeRenderer<E> renderer) {
        myEdgeRenderer = renderer;
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

    public @Nullable GraphNodeRenderer<E> getNodeRenderer() {
        return myNodeRenderer;
    }

    public @Nullable GraphEdgeRenderer<E> getEdgeRenderer() {
        return myEdgeRenderer;
    }
}
