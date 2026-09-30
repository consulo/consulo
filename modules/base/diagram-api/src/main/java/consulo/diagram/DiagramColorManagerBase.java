/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import consulo.diagram.presentation.DiagramArrow;
import consulo.diagram.presentation.DiagramLineType;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class DiagramColorManagerBase extends DiagramColorManager {
    public static final ColorValue REALIZATION = new RGBColor(0, 130, 0);
    public static final ColorValue GENERALIZATION = new RGBColor(0, 0, 130);
    public static final ColorValue NODE_BACKGROUND = new RGBColor(252, 250, 209);
    public static final ColorValue DEFAULT_EDGE_COLOR = new RGBColor(89, 89, 89);
    public static final ColorValue ANNOTATION = new RGBColor(153, 153, 0);
    public static final ColorValue NODE_HEADER_COLOR = new RGBColor(215, 213, 172);

    private static final ColorValue SELECTED_FOREGROUND = new RGBColor(255, 255, 255);
    private static final ColorValue FOREGROUND = new RGBColor(0, 0, 0);

    @Override
    public ColorValue getNodeHeaderColor(@Nullable DiagramNode<?> node) {
        return NODE_HEADER_COLOR;
    }

    @Override
    public ColorValue getNodeBackgroundColor() {
        return NODE_BACKGROUND;
    }

    @Override
    public ColorValue getNodeForegroundColor(boolean selected) {
        return selected ? SELECTED_FOREGROUND : FOREGROUND;
    }

    @Override
    public ColorValue getEdgeColor(@Nullable DiagramEdge<?> edge) {
        if (edge == null) {
            return DEFAULT_EDGE_COLOR;
        }
        DiagramRelationshipInfo relationship = edge.getRelationship();
        if (relationship.getStartArrow() == DiagramArrow.DELTA) {
            if (relationship.getLineType() == DiagramLineType.SOLID) {
                return GENERALIZATION;
            }
            if (relationship.getLineType() == DiagramLineType.DASHED) {
                return REALIZATION;
            }
        }
        if (!isArrow(relationship.getEndArrow()) && !isArrow(relationship.getStartArrow())
            && relationship.getLineType() == DiagramLineType.DOTTED) {
            return ANNOTATION;
        }
        return DEFAULT_EDGE_COLOR;
    }

    private static boolean isArrow(@Nullable DiagramArrow arrow) {
        return arrow != null && arrow != DiagramArrow.NONE;
    }
}
