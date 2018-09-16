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

package com.intellij.find;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import java.util.List;

public interface FindInProjectSettings {

  static FindInProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, FindInProjectSettings.class);
  }

  void addStringToFind(@Nonnull String s);

  void addStringToReplace(@Nonnull String s);

  void addDirectory(@Nonnull String s);

  @Nonnull
  String[] getRecentFindStrings();

  @Nonnull
  String[] getRecentReplaceStrings();

  @Nonnull
  List<String> getRecentDirectories();
}
