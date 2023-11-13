/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.configurable;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.configurable.Configurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.DefaultSdksModel;
import consulo.ide.impl.ui.app.impl.settings.UnifiedSettingsDialog;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.project.Project;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-02
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedShowSettingsUtil extends BaseProjectStructureShowSettingsUtil {
  private DefaultProjectFactory myDefaultProjectFactory;

  @Inject
  public UnifiedShowSettingsUtil(DefaultProjectFactory defaultProjectFactory) {
    myDefaultProjectFactory = defaultProjectFactory;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showSettingsDialog(@Nullable Project project) {
    Project actualProject = project == null ? myDefaultProjectFactory.getDefaultProject() : project;

    UnifiedSettingsDialog settingsDialog = new UnifiedSettingsDialog(buildConfigurables(actualProject));
    return settingsDialog.showAsync();
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public <T extends UnnamedConfigurable> AsyncResult<Void> showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect, @Nonnull Consumer<T> afterSelect) {
    return showSettingsDialog(project);
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect) {
    return showSettingsDialog(project);
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showSettingsDialog(@Nullable Project project, String id2Select, String filter) {
    return showSettingsDialog(project);
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showSettingsDialog(@Nonnull Project project, Configurable toSelect) {
    return showSettingsDialog(project);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showProjectStructureDialog(@Nonnull Project project, @Nonnull Consumer<ProjectStructureSelector> consumer) {
    return AsyncResult.rejected();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable, Runnable advancedInitialization) {
    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization) {
    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, String dimensionServiceKey, Configurable configurable) {
    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
    return AsyncResult.resolved();
  }

  @Override
  public boolean isAlreadyShown(Project project) {
    return false;
  }

  @Nonnull
  @Override
  public SettingsSdksModel getSdksModel() {
    SettingsSdksModel model = new DefaultSdksModel();
    model.reset();
    return model;
  }
}
