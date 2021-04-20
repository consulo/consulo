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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.packaging.artifacts.Artifact;
import consulo.annotation.DeprecationInfo;
import consulo.roots.orderEntry.OrderEntryType;
import consulo.roots.orderEntry.OrderEntryTypeEditor;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
@Singleton
@Deprecated
@DeprecationInfo("This class was proxy between SmallIDE and IDEA, but we don't need this anymore")
public final class ProjectSettingsService {
  public static ProjectSettingsService getInstance(Project project) {
    return ServiceManager.getService(project, ProjectSettingsService.class);
  }

  private final Project myProject;

  @Inject
  public ProjectSettingsService(Project project) {
    myProject = project;
  }

  @RequiredUIAccess
  public void openProjectSettings() {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myProject, c -> c.selectProjectGeneralSettings(true));
  }

  @RequiredUIAccess
  public void openLibrary(@Nonnull Library library) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myProject, c -> c.selectProjectOrGlobalLibrary(library, true));
  }

  @RequiredUIAccess
  public void openModuleSettings(final Module module) {
    Project project = module.getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.select(module.getName(), null, true));
  }

  public final boolean canOpenModuleSettings() {
    return true;
  }

  @RequiredUIAccess
  public void openModuleLibrarySettings(final Module module) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(module.getProject(), config -> config.select(module.getName(), ProjectBundle.message("modules.classpath.title"), true));
  }

  public final boolean canOpenModuleLibrarySettings() {
    return true;
  }

  @RequiredUIAccess
  public void openContentEntriesSettings(final Module module) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(module.getProject(), config -> config.select(module.getName(), ProjectBundle.message("module.paths.title"), true));
  }

  public final boolean canOpenContentEntriesSettings() {
    return true;
  }

  @RequiredUIAccess
  public void openModuleDependenciesSettings(@Nonnull Module module, @Nullable OrderEntry orderEntry) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myProject, c -> c.selectOrderEntry(module, orderEntry));
  }

  public final boolean canOpenModuleDependenciesSettings() {
    return true;
  }

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public void openLibraryOrSdkSettings(final @Nonnull OrderEntry orderEntry) {
    OrderEntryType type = orderEntry.getType();

    OrderEntryTypeEditor editor = OrderEntryTypeEditor.FACTORY.getByKey(type);
    if (editor != null) {
      editor.navigate(orderEntry);
    }
  }

  @RequiredUIAccess
  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myProject, config -> config.select(moduleToSelect, editorNameToSelect, true));
  }

  @RequiredUIAccess
  public void openArtifactSettings(@Nullable Artifact artifact) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myProject, config -> config.select(artifact, true));
  }
}
