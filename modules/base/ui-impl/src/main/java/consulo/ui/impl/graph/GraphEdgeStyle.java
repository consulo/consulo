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
package consulo.ui.impl.graph;

import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.graph.GraphArrow;
import consulo.ui.graph.GraphEdgePresentation;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphLineStyle;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public final class GraphEdgeStyle implements GraphEdgePresentation {
    private GraphLineStyle myLineStyle = GraphLineStyle.SOLID;
    private GraphArrow mySourceArrow = GraphArrow.NONE;
    private GraphArrow myTargetArrow = GraphArrow.FILLED;
    private LocalizeValue myLabel = LocalizeValue.empty();
    private LocalizeValue myTooltip = LocalizeValue.empty();
    private @Nullable ColorValue myColor;

    public static <E> GraphEdgeStyle of(GraphEdgeRender<E> render, E source, E target) {
        GraphEdgeStyle style = new GraphEdgeStyle();
        render.render(style, source, target);
        return style;
    }

    @Override
    public GraphEdgePresentation withLineStyle(GraphLineStyle lineStyle) {
        myLineStyle = lineStyle;
        return this;
    }

    @Override
    public GraphEdgePresentation withSourceArrow(GraphArrow arrow) {
        mySourceArrow = arrow;
        return this;
    }

    @Override
    public GraphEdgePresentation withTargetArrow(GraphArrow arrow) {
        myTargetArrow = arrow;
        return this;
    }

    @Override
    public GraphEdgePresentation withLabel(LocalizeValue label) {
        myLabel = label;
        return this;
    }

    @Override
    public GraphEdgePresentation withTooltip(LocalizeValue tooltip) {
        myTooltip = tooltip;
        return this;
    }

    @Override
    public GraphEdgePresentation withColor(@Nullable ColorValue color) {
        myColor = color;
        return this;
    }

    public GraphLineStyle getLineStyle() {
        return myLineStyle;
    }

    public GraphArrow getSourceArrow() {
        return mySourceArrow;
    }

    public GraphArrow getTargetArrow() {
        return myTargetArrow;
    }

    public LocalizeValue getLabel() {
        return myLabel;
    }

    public LocalizeValue getTooltip() {
        return myTooltip;
    }

    public @Nullable ColorValue getColor() {
        return myColor;
    }
}
