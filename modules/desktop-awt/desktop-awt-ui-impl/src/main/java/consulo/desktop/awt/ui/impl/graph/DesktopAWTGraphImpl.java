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
package consulo.desktop.awt.ui.impl.graph;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.graph.Graph;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopAWTGraphImpl<E> extends SwingComponentDelegate<DesktopAWTGraphComponent<E>> implements Graph<E> {
    private final GraphModel<E> myModel;

    private GraphNodeRender<E> myNodeRender = GraphNodeRender.defaultRender();
    private GraphEdgeRender<E> myEdgeRender = GraphEdgeRender.defaultRender();

    public DesktopAWTGraphImpl(GraphModel<E> model) {
        myModel = model;
    }

    @Override
    protected DesktopAWTGraphComponent<E> createComponent() {
        return new DesktopAWTGraphComponent<>(this, myModel, () -> myNodeRender, () -> myEdgeRender);
    }

    @Override
    protected void init(DesktopAWTGraphComponent<E> component) {
        super.init(component);

        component.rebuild();
    }

    @RequiredUIAccess
    @Override
    public void setNodeRender(GraphNodeRender<E> render) {
        myNodeRender = render;

        if (isInitialized()) {
            toAWTComponent().rebuild();
        }
    }

    @RequiredUIAccess
    @Override
    public void setEdgeRender(GraphEdgeRender<E> render) {
        myEdgeRender = render;

        if (isInitialized()) {
            toAWTComponent().rebuild();
        }
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        if (isInitialized()) {
            toAWTComponent().rebuild();
        }
    }
}
