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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class ProjectSettingsService {
  public static ProjectSettingsService getInstance(Project project) {
    return ServiceManager.getService(project, ProjectSettingsService.class);
  }

  public void openProjectSettings() {
  }

  public void openLibrary(@NotNull Library library) {
  }

  public void openModuleSettings(final Module module) {
  }

  public boolean canOpenModuleSettings() {
    return false;
  }

  public void openModuleLibrarySettings(final Module module) {
  }

  public boolean canOpenModuleLibrarySettings() {
    return false;
  }

  public void openContentEntriesSettings(final Module module) {
  }

  public boolean canOpenContentEntriesSettings() {
    return false;
  }

  public void openModuleDependenciesSettings(@NotNull Module module, @Nullable OrderEntry orderEntry) {
  }

  public boolean canOpenModuleDependenciesSettings() {
    return false;
  }

  public void openLibraryOrSdkSettings(final @NotNull OrderEntry orderEntry) {
  }

  public boolean processModulesMoved(final Module[] modules, @Nullable final ModuleGroup targetGroup) {
    return false;
  }

  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
  }
}
