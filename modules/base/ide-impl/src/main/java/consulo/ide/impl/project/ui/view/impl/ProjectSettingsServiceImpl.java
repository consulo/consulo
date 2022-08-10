/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.project.ui.view.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.artifact.Artifact;
import consulo.content.library.Library;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@Singleton
@ServiceImpl
public class ProjectSettingsServiceImpl extends ProjectSettingsService {

  private final Project myProject;
  private final ShowSettingsUtil myShowSettingsUtil;

  @Inject
  public ProjectSettingsServiceImpl(Project project, ShowSettingsUtil showSettingsUtil) {
    myProject = project;
    myShowSettingsUtil = showSettingsUtil;
  }

  @Override
  @RequiredUIAccess
  public void openProjectSettings() {
    myShowSettingsUtil.showProjectStructureDialog(myProject, c -> c.selectProjectGeneralSettings(true));
  }

  @Override
  @RequiredUIAccess
  public void openLibrary(@Nonnull Library library) {
    myShowSettingsUtil.showProjectStructureDialog(myProject, c -> c.selectProjectOrGlobalLibrary(library, true));
  }

  @Override
  @RequiredUIAccess
  public void openModuleSettings(final Module module) {
    Project project = module.getProject();
    myShowSettingsUtil.showProjectStructureDialog(project, config -> config.select(module.getName(), null, true));
  }

  @Override
  @RequiredUIAccess
  public void openModuleLibrarySettings(final Module module) {
    myShowSettingsUtil.showProjectStructureDialog(module.getProject(), config -> config.select(module.getName(), ProjectBundle.message("modules.classpath.title"), true));
  }

  @Override
  @RequiredUIAccess
  public void openContentEntriesSettings(final Module module) {
    myShowSettingsUtil.showProjectStructureDialog(module.getProject(), config -> config.select(module.getName(), ProjectBundle.message("module.paths.title"), true));
  }

  @Override
  @RequiredUIAccess
  public void openModuleDependenciesSettings(@Nonnull Module module, @Nullable OrderEntry orderEntry) {
    myShowSettingsUtil.showProjectStructureDialog(myProject, c -> c.selectOrderEntry(module, orderEntry));
  }

  @Override
  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public void openLibraryOrSdkSettings(final @Nonnull OrderEntry orderEntry) {
    OrderEntryType type = orderEntry.getType();

    OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
    editor.navigate(orderEntry);
  }

  @Override
  @RequiredUIAccess
  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
    myShowSettingsUtil.showProjectStructureDialog(myProject, config -> config.select(moduleToSelect, editorNameToSelect, true));
  }
}
