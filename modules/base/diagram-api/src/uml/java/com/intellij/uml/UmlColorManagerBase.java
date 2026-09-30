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

package com.intellij.uml;

import static com.intellij.uml.UmlRelationshipInfo.DELTA;
import static com.intellij.uml.UmlRelationshipInfo.NONE;
import com.intellij.uml.presentation.UmlLineType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class UmlColorManagerBase extends UmlColorManager {
  public static final Color REALIZATION = new Color(0,130,0);
  public static final Color GENERALIZATION = new Color(0,0,130);
  public static final Color NODE_BACKGROUND = new Color(252, 250, 209);
  public static final Color DEFAULT_EDGE_COLOR = Color.GRAY.darker();
  public static final Color ANNOTATION = new Color(153, 153, 0);
  public static final Color NODE_HEADER_COLOR = new Color(215, 213, 172);

  @Override
  public Color getNodeHeaderColor(@Nullable UmlNode node) {
    return NODE_HEADER_COLOR;
  }

  @Override
  public Color getNodeBackgroundColor() {
    return NODE_BACKGROUND;
  }

  @Override
  public Color getNodeForegroundColor(boolean selected) {
    return selected ? Color.WHITE : Color.BLACK;
  }

  @Override
  public Color getEdgeColor(UmlEdge edge) {
    if (edge == null) return DEFAULT_EDGE_COLOR;

    final UmlRelationshipInfo relationship = edge.getRelationship();
    if (relationship.getStartArrow() == DELTA) {
      if (relationship.getLineType() == UmlLineType.SOLID) return GENERALIZATION;
      if (relationship.getLineType() == UmlLineType.DASHED) return REALIZATION;
    }
    if (!isArrow(relationship.getEndArrow()) && !isArrow(relationship.getStartArrow()) && relationship.getLineType() == UmlLineType.DOTTED) {
      return ANNOTATION;
    }
    return DEFAULT_EDGE_COLOR;
  }

  private static boolean isArrow(Shape arrow) {
    return arrow != null && arrow != NONE;
  }
}
