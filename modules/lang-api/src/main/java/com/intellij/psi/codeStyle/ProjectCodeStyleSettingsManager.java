/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.psi.codeStyle;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;


@State(
        name = "ProjectCodeStyleSettingsManager",
        storages = {@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/codeStyleSettings.xml")})
public class ProjectCodeStyleSettingsManager extends CodeStyleSettingsManager {
  @NotNull
  public static ProjectCodeStyleSettingsManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ProjectCodeStyleSettingsManager.class);
  }
}
