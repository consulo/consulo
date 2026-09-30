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

import com.intellij.uml.presentation.UmlLineType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Area;

/**
 * @author Konstantin Bulenkov
 */
public interface UmlRelationshipInfo {
  @Nullable UmlLineType getLineType();
  @Nullable String getLabel();
  @Nullable Shape getStartArrow();
  @Nullable Shape getEndArrow();

  UmlRelationshipInfo NO_RELATIONSHIP = new UmlRelationshipInfo() {
    public UmlLineType getLineType() {return null;}
    public String getLabel() {return null;}
    public Shape getStartArrow() {return null;}
    public Shape getEndArrow() {return null;}
  };

  Shape DELTA = new Area();
  Shape DIAMOND = new Area();
  Shape NONE = new Area();
  Shape INNER_CLASS_ARROW = new Area();
}
