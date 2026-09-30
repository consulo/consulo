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

package com.intellij.uml.settings;

import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.settings.GraphSettingsProvider;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author Konstantin Bulenkov
 */
public enum UmlLayout {
  BALLOON,
  CIRCULAR,
  HIERARCHIC_GROUP,
  ORGANIC,
  ORTHOGONAL,
  DIRECTED_ORTHOGONAL;

  public static UmlLayout getDefault() {
    return HIERARCHIC_GROUP;
  }

  public Layouter getLayouter(Graph2D graph, Project project) {
    final GraphSettings settings = GraphSettingsProvider.getInstance(project).getSettings(graph);
    switch (this) {
      case BALLOON: return settings.getBalloonLayouter();
      case CIRCULAR: return settings.getCircularLayouter();
      case DIRECTED_ORTHOGONAL: return settings.getDirectedOrthogonalLayouter();
      case HIERARCHIC_GROUP: return settings.getGroupLayouter();
      case ORGANIC: return settings.getOrganicLayouter();
      case ORTHOGONAL: return settings.getOrthogonalLayouter();
      default: return settings.getGroupLayouter();
    }
  }

  public String getPresentableName() {
    return StringUtil.capitalizeWords(name().toLowerCase().replaceAll("_", " "), true);
  }
  
  public static UmlLayout fromString(Object obj) {
    if (obj == null) return getDefault();
    final String name = obj.toString();

    try {
      return valueOf(name.toUpperCase().replaceAll(" ", "_"));
    } catch (IllegalArgumentException e) {
      return getDefault();
    }
  }

}
