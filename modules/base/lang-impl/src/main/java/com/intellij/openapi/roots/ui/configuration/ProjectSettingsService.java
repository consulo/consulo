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
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import com.intellij.packaging.artifacts.Artifact;
import consulo.annotation.DeprecationInfo;
import consulo.roots.orderEntry.OrderEntryType;
import consulo.roots.orderEntry.OrderEntryTypeEditor;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Singleton;

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
    ModulesConfigurator.showDialog(myProject, module.getName(), null);
  }

  public final boolean canOpenModuleSettings() {
    return true;
  }

  @RequiredUIAccess
  public void openModuleLibrarySettings(final Module module) {
    ModulesConfigurator.showDialog(myProject, module.getName(), ClasspathEditor.NAME);
  }

  public final boolean canOpenModuleLibrarySettings() {
    return true;
  }

  @RequiredUIAccess
  public void openContentEntriesSettings(final Module module) {
    ModulesConfigurator.showDialog(myProject, module.getName(), ContentEntriesEditor.NAME);
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

  public boolean processModulesMoved(final Module[] modules, @Nullable final ModuleGroup targetGroup) {
    final ModuleStructureConfigurable rootConfigurable = ModuleStructureConfigurable.getInstance(myProject);
    if (rootConfigurable.updateProjectTree(modules, targetGroup)) { //inside project root editor
      if (targetGroup != null) {
        rootConfigurable.selectNodeInTree(targetGroup.toString());
      }
      else {
        rootConfigurable.selectNodeInTree(modules[0].getName());
      }
      return true;
    }
    return false;
  }

  @RequiredUIAccess
  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
    ModulesConfigurator.showDialog(myProject, moduleToSelect, editorNameToSelect);
  }

  @RequiredUIAccess
  public void openArtifactSettings(@Nullable Artifact artifact) {
    ModulesConfigurator.showArtifactSettings(myProject, artifact);
  }
}
