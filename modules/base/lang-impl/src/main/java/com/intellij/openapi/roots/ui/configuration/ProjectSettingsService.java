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
import com.intellij.openapi.util.AsyncResult;
import com.intellij.packaging.artifacts.Artifact;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public abstract class ProjectSettingsService {
  public static ProjectSettingsService getInstance(Project project) {
    return ServiceManager.getService(project, ProjectSettingsService.class);
  }

  public void openProjectSettings() {
  }

  public void openLibrary(@Nonnull Library library) {
  }

  @Nonnull
  public abstract AsyncResult<Void> openModuleSettings(Module module);

  public boolean canOpenModuleSettings() {
    return false;
  }

  @Nonnull
  public abstract AsyncResult<Void> openModuleLibrarySettings(Module module);

  public boolean canOpenModuleLibrarySettings() {
    return false;
  }

  @Nonnull
  public abstract AsyncResult<Void> openContentEntriesSettings(Module module);

  public boolean canOpenContentEntriesSettings() {
    return false;
  }

  @Nonnull
  public abstract AsyncResult<Void> openModuleDependenciesSettings(@Nonnull Module module, @Nullable OrderEntry orderEntry);

  public boolean canOpenModuleDependenciesSettings() {
    return false;
  }

  @Nonnull
  @RequiredUIAccess
  public abstract AsyncResult<Void> openLibraryOrSdkSettings(final @Nonnull OrderEntry orderEntry);

  public boolean processModulesMoved(final Module[] modules, @Nullable final ModuleGroup targetGroup) {
    return false;
  }

  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
  }

  @Nonnull
  public abstract AsyncResult<Void> openArtifactSettings(@Nullable Artifact artifact);
}
