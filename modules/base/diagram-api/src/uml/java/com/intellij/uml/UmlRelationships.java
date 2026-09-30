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

import com.intellij.openapi.graph.view.Arrow;
import com.intellij.openapi.graph.view.Drawable;
import com.intellij.uml.presentation.UmlLineType;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * @author Konstantin Bulenkov
 */
public class UmlRelationships {
  private static Shape getAngleArrow() {
    GeneralPath shape = new GeneralPath();
    shape.moveTo(-8F, -5F);
    shape.lineTo(0.0F, 0.0F);
    shape.lineTo(-8F, 5F);
    return shape;
  }

  public static final UmlRelationshipInfo DEPENDENCY = new UmlRelationshipInfoAdapter("DEPENDENCY", UmlLineType.DASHED){
    public Shape getStartArrow() {
      return getAngleArrow();
    }
  };

  public static final UmlRelationshipInfo CREATE = new UmlRelationshipInfoAdapter("CREATE", UmlLineType.DASHED, "<html>&laquo;create&raquo;</html>"){
    public Shape getStartArrow() {
      return getAngleArrow();
    }
  };

  public static final UmlRelationshipInfo TO_ONE = new UmlRelationshipInfoAdapter("TO_ONE", UmlLineType.SOLID, "1:1") {
    public Shape getStartArrow() {
      return DIAMOND;
    }

    @Override
    public Shape getEndArrow() {
      return getAngleArrow();
    }
  };

  public static final UmlRelationshipInfo TO_MANY = new UmlRelationshipInfoAdapter("TO_MANY", UmlLineType.SOLID, "1:*") {
    public Shape getStartArrow() {
      return DIAMOND;
    }

    @Override
    public Shape getEndArrow() {
      return getAngleArrow();
    }
  };


  public static final UmlRelationshipInfo GENERALIZATION = new UmlRelationshipInfoAdapter("GENERALIZATION") {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final UmlRelationshipInfo INTERFACE_GENERALIZATION = new UmlRelationshipInfoAdapter("INTERFACE_GENERALIZATION") {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final UmlRelationshipInfo REALIZATION = new UmlRelationshipInfoAdapter("REALIZATION", UmlLineType.DASHED) {
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public static final UmlRelationshipInfo ANNOTATION = new UmlRelationshipInfoAdapter("ANNOTATION", UmlLineType.DOTTED) {
    public Shape getStartArrow() {
      return NONE;
    }
  };

  public static final UmlRelationshipInfo INNER_CLASS = new UmlRelationshipInfoAdapter("INNER_CLASS") {
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

  static abstract class UmlRelationshipInfoAdapter implements UmlRelationshipInfo {
    private final String myName;
    private final UmlLineType myLineType;
    private final String myLabel;

    public UmlRelationshipInfoAdapter(String name, UmlLineType lineType, String label) {
      myName = name == null ? "UNDEFINED" : name;
      myLineType = lineType == null ? UmlLineType.SOLID : lineType;
      myLabel = label == null ? "" : label;
    }

    public UmlRelationshipInfoAdapter(String name, UmlLineType lineType) {
      this(name, lineType, null);
    }

    public UmlRelationshipInfoAdapter(String name) {
      this(name, null, null);
    }

    public UmlLineType getLineType() {
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
