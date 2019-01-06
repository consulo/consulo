/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.packaging.artifacts.Artifact;
import consulo.roots.orderEntry.OrderEntryType;
import consulo.roots.orderEntry.OrderEntryTypeEditor;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class IdeaProjectSettingsService extends ProjectSettingsService {
  private final Project myProject;

  @Inject
  public IdeaProjectSettingsService(final Project project) {
    myProject = project;
  }

  @Override
  public void openProjectSettings() {
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(myProject);
    ShowSettingsUtil.getInstance().editConfigurable(myProject, config, () -> config.selectProjectGeneralSettings(true));
  }

  @Override
  public void openLibrary(@Nonnull final Library library) {
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(myProject);
    ShowSettingsUtil.getInstance().editConfigurable(myProject, config, () -> config.selectProjectOrGlobalLibrary(library, true));
  }

  @Override
  public boolean canOpenModuleSettings() {
    return true;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> openModuleSettings(final Module module) {
    return ModulesConfigurator.showDialog(myProject, module.getName(), null);
  }

  @Override
  public boolean canOpenModuleLibrarySettings() {
    return true;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> openModuleLibrarySettings(final Module module) {
    return ModulesConfigurator.showDialog(myProject, module.getName(), ClasspathEditor.NAME);
  }

  @Override
  public boolean canOpenContentEntriesSettings() {
    return true;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> openContentEntriesSettings(final Module module) {
    return ModulesConfigurator.showDialog(myProject, module.getName(), ContentEntriesEditor.NAME);
  }

  @Override
  public boolean canOpenModuleDependenciesSettings() {
    return true;
  }

  @Nonnull
  @Override
  @RequiredUIAccess
  public AsyncResult<Void> openModuleDependenciesSettings(@Nonnull final Module module, @Nullable final OrderEntry orderEntry) {
    return ShowSettingsUtil.getInstance()
            .editConfigurable(myProject, ProjectStructureConfigurable.getInstance(myProject), () -> ProjectStructureConfigurable.getInstance(myProject).selectOrderEntry(module, orderEntry));
  }

  @RequiredUIAccess
  @Nonnull
  @SuppressWarnings("unchecked")
  @Override
  public AsyncResult<Void> openLibraryOrSdkSettings(@Nonnull final OrderEntry orderEntry) {
    OrderEntryType type = orderEntry.getType();

    OrderEntryTypeEditor editor = OrderEntryTypeEditor.FACTORY.getByKey(type);
    if (editor != null) {
      return editor.navigateAsync(orderEntry);
    }
    return AsyncResult.resolved();
  }

  @Override
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

  @Override
  public void showModuleConfigurationDialog(String moduleToSelect, String editorNameToSelect) {
    ModulesConfigurator.showDialog(myProject, moduleToSelect, editorNameToSelect);
  }

  @Override
  public AsyncResult<Void> openArtifactSettings(@Nullable Artifact artifact) {
    return ModulesConfigurator.showArtifactSettings(myProject, artifact);
  }
}
