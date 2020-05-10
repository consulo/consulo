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
package consulo.web.options;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import consulo.ide.base.BaseShowSettingsUtil;
import consulo.ide.settings.impl.ShowSdksSettingsUtil;
import consulo.options.ProjectStructureSelector;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.app.impl.settings.SettingsDialog;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-02
 */
@Singleton
public class WebShowSettingsUtil extends BaseShowSettingsUtil implements ShowSdksSettingsUtil {
  private DefaultProjectFactory myDefaultProjectFactory;

  @Inject
  public WebShowSettingsUtil(DefaultProjectFactory defaultProjectFactory) {
    myDefaultProjectFactory = defaultProjectFactory;
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project) {
    Project actualProject = project == null ? myDefaultProjectFactory.getDefaultProject() : project;

    SettingsDialog settingsDialog = new SettingsDialog(buildConfigurables(actualProject));
    settingsDialog.showAsync();
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project, Class toSelect) {
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
  public void showProjectStructureDialog(@Nonnull Project project, @Nonnull Consumer<ProjectStructureSelector> consumer) {

  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable, Runnable advancedInitialization) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, @NonNls String dimensionServiceKey, Configurable configurable) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
    return null;
  }

  @Override
  public boolean isAlreadyShown() {
    return false;
  }
}
