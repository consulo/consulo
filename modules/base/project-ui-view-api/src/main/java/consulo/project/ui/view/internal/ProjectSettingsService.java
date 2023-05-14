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

package consulo.project.ui.view.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
public class ProjectSettingsService {
  public static ProjectSettingsService getInstance(Project project) {
    return project.getInstance(ProjectSettingsService.class);
  }

  @RequiredUIAccess
  public void openProjectSettings() {
  }

  @RequiredUIAccess
  public void openLibrary(@Nonnull Library library) {
  }

  @RequiredUIAccess
  public void openModuleSettings(final Module module) {
  }

  public final boolean canOpenModuleSettings() {
    return true;
  }

  @RequiredUIAccess
  public void openModuleLibrarySettings(final Module module) {
  }

  public final boolean canOpenModuleLibrarySettings() {
    return true;
  }

  @RequiredUIAccess
  public void openContentEntriesSettings(final Module module) {
  }

  public final boolean canOpenContentEntriesSettings() {
    return true;
  }

  @RequiredUIAccess
  public void openModuleDependenciesSettings(@Nonnull Module module, @Nullable OrderEntry orderEntry) {
  }

  public final boolean canOpenModuleDependenciesSettings() {
    return true;
  }

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public void openLibraryOrSdkSettings(final @Nonnull OrderEntry orderEntry) {
  }

  @RequiredUIAccess
  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
  }
}
