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
package consulo.preferences;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import consulo.options.BaseProjectStructureShowSettingsUtil;
import consulo.options.ProjectStructureSelector;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.app.impl.settings.UnifiedSettingsDialog;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-02
 */
@Singleton
public class UnifiedShowSettingsUtil extends BaseProjectStructureShowSettingsUtil {
  private DefaultProjectFactory myDefaultProjectFactory;

  @Inject
  public UnifiedShowSettingsUtil(DefaultProjectFactory defaultProjectFactory) {
    myDefaultProjectFactory = defaultProjectFactory;
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project) {
    Project actualProject = project == null ? myDefaultProjectFactory.getDefaultProject() : project;

    UnifiedSettingsDialog settingsDialog = new UnifiedSettingsDialog(buildConfigurables(actualProject));
    settingsDialog.showAsync();
  }

  @RequiredUIAccess
  @Override
  public <T extends UnnamedConfigurable> void showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect, @Nonnull Consumer<T> afterSelect) {
    showSettingsDialog(project);
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect) {
    showSettingsDialog(project);
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project, String id2Select, String filter) {
    showSettingsDialog(project);
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nonnull Project project, Configurable toSelect) {
    showSettingsDialog(project);
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
  public boolean isAlreadyShown() {
    return false;
  }
}
