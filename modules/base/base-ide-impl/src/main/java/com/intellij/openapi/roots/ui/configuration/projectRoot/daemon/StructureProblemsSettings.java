/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.roots.ui.configuration.projectRoot.daemon.ApplicationStructureProblemsSettings;
import consulo.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemsSettings;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface StructureProblemsSettings {
  public static StructureProblemsSettings getProjectInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ProjectStructureProblemsSettings.class);
  }

  public static StructureProblemsSettings getGlobalInstance() {
    return ServiceManager.getService(ApplicationStructureProblemsSettings.class);
  }

  boolean isIgnored(@Nonnull ProjectStructureProblemDescription description);

  void setIgnored(@Nonnull ProjectStructureProblemDescription description, boolean ignored);
}
