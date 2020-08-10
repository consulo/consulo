/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.profile.codeInspection.ui.inspectionsTree;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.profile.codeInspection.ui.ScopeOrderComparator;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Dmitry Batkovich
 */
public class MultiScopeSeverityIcon {
  @Nullable
  public static Image create(final Map<String, HighlightSeverity> scopeToAverageSeverityMap, final String defaultScopeName, final InspectionProfileImpl inspectionProfile) {
    final List<String> sortedScopeNames = new ArrayList<String>(scopeToAverageSeverityMap.keySet());
    LinkedHashMap<String, HighlightDisplayLevel> myScopeToAverageSeverityMap = new LinkedHashMap<>();
    Collections.sort(sortedScopeNames, new ScopeOrderComparator(inspectionProfile));
    sortedScopeNames.remove(defaultScopeName);
    sortedScopeNames.add(defaultScopeName);
    for (final String scopeName : sortedScopeNames) {
      final HighlightSeverity severity = scopeToAverageSeverityMap.get(scopeName);
      if (severity == null) {
        continue;
      }
      final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
      if (level == null) {
        continue;
      }
      myScopeToAverageSeverityMap.put(scopeName, level);
    }

    if (myScopeToAverageSeverityMap.size() == 1) {
      HighlightDisplayLevel firstItem = ContainerUtil.getFirstItem(myScopeToAverageSeverityMap.values());
      assert firstItem != null;
      return firstItem.getIcon();
    }
    return null;
  }
}
