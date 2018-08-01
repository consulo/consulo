/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.find.impl;

import com.intellij.find.FindInProjectSettings;
import com.intellij.find.FindSettings;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@State(name = "FindInProjectRecents", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)})
@Singleton
final class FindInProjectRecents extends FindInProjectSettingsBase implements FindInProjectSettings {
  @Deprecated
  public static FindInProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, FindInProjectSettings.class);
  }

  private final FindSettings myFindSettings;

  @Inject
  FindInProjectRecents(FindSettings findSettings) {
    myFindSettings = findSettings;
  }

  @Override
  @Nonnull
  public List<String> getRecentDirectories() {
    ArrayList<String> strings = new ArrayList<>(myFindSettings.getRecentDirectories());
    strings.addAll(super.getRecentDirectories());
    return strings;
  }

  @Override
  @Nonnull
  public String[] getRecentFindStrings() {
    return ArrayUtil.mergeArrays(myFindSettings.getRecentFindStrings(), super.getRecentFindStrings());
  }

  @Override
  @Nonnull
  public String[] getRecentReplaceStrings() {
    return ArrayUtil.mergeArrays(myFindSettings.getRecentReplaceStrings(), super.getRecentReplaceStrings());
  }
}
