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

package com.intellij.diagram;

import com.intellij.openapi.graph.view.Arrow;
import com.intellij.openapi.graph.view.Drawable;
import com.intellij.diagram.presentation.DiagramLineType;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * @author Konstantin Bulenkov
 */
public class DiagramRelationships {
  private static Shape getAngleArrow() {
    GeneralPath shape = new GeneralPath();
    shape.moveTo(-8F, -5F);
    shape.lineTo(0.0F, 0.0F);
    shape.lineTo(-8F, 5F);
    return shape;
  }

  public static final DiagramRelationshipInfo DEPENDENCY = new DiagramRelationshipInfoAdapter("DEPENDENCY", DiagramLineType.DASHED){
    public Shape getStartArrow() {
      return getAngleArrow();
    }
  };

  public static final DiagramRelationshipInfo CREATE = new DiagramRelationshipInfoAdapter("CREATE", DiagramLineType.DASHED, "<html>&laquo;create&raquo;</html>"){
    public Shape getStartArrow() {
      return getAngleArrow();
    }
  };

  public static final DiagramRelationshipInfo TO_ONE = new DiagramRelationshipInfoAdapter("TO_ONE", DiagramLineType.SOLID, "1:1") {
    public Shape getStartArrow() {
      return DIAMOND;
    }

    @Override
    public Shape getEndArrow() {
      return getAngleArrow();
    }
  };

  public static final DiagramRelationshipInfo TO_MANY = new DiagramRelationshipInfoAdapter("TO_MANY", DiagramLineType.SOLID, "1:*") {
    public Shape getStartArrow() {
      return DIAMOND;
    }

    @Override
    public Shape getEndArrow() {
      return getAngleArrow();
    }
  };


  public static final DiagramRelationshipInfo GENERALIZATION = new DiagramRelationshipInfoAdapter("GENERALIZATION") {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final DiagramRelationshipInfo INTERFACE_GENERALIZATION = new DiagramRelationshipInfoAdapter("INTERFACE_GENERALIZATION") {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final DiagramRelationshipInfo REALIZATION = new DiagramRelationshipInfoAdapter("REALIZATION", DiagramLineType.DASHED) {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final DiagramRelationshipInfo ANNOTATION = new DiagramRelationshipInfoAdapter("ANNOTATION", DiagramLineType.DOTTED) {
    public Shape getStartArrow() {
      return NONE;
    }
  };

  public static final DiagramRelationshipInfo INNER_CLASS = new DiagramRelationshipInfoAdapter("INNER_CLASS") {
    private @NonNls final String INNER_CLASS_ARROW = "InnerClassArrow";
    private static final int R = 5;

    public Arrow getArrow() {
      if (Arrow.Statics.getCustomArrow(INNER_CLASS_ARROW) == null) {
        Drawable arrow = new Drawable() {
          public void paint(final Graphics2D g) {
            Paint paint = g.getPaint();
            g.setPaint(g.getBackground());
            g.fillOval(-2*R, -R, 2*R, 2*R);
            g.setPaint(paint);
            g.drawOval(-2*R, -R, 2*R, 2*R);
            g.drawLine(-R, -R+2, -R, R-2);
            g.drawLine(-2*R+2, 0, -2, 0);
          }

          public Rectangle getBounds() {return new Rectangle(-R, -R, R, R);}
        };
        Arrow.Statics.addCustomArrow(INNER_CLASS_ARROW, arrow);
      }
      return Arrow.Statics.getCustomArrow(INNER_CLASS_ARROW);
    }

    public Shape getStartArrow() {
      return getArrow().getShape();
    }
  };

  static abstract class DiagramRelationshipInfoAdapter implements DiagramRelationshipInfo {
    private final String myName;
    private final DiagramLineType myLineType;
    private final String myLabel;

    public DiagramRelationshipInfoAdapter(String name, DiagramLineType lineType, String label) {
      myName = name == null ? "UNDEFINED" : name;
      myLineType = lineType == null ? DiagramLineType.SOLID : lineType;
      myLabel = label == null ? "" : label;
    }

    public DiagramRelationshipInfoAdapter(String name, DiagramLineType lineType) {
      this(name, lineType, null);
    }

    public DiagramRelationshipInfoAdapter(String name) {
      this(name, null, null);
    }

    public DiagramLineType getLineType() {
      return myLineType;
    }

    public String getLabel() {
      return myLabel;
    }

    public abstract Shape getStartArrow();

    public Shape getEndArrow() {
      return null;
    }

    @Override
    public String toString() {
      return myName;
    }
  }
}
